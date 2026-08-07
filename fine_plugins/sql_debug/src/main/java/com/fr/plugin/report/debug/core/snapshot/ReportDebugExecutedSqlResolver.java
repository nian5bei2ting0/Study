package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.probe.DatasetExecutionDetector;

import com.fr.base.Parameter;
import com.fr.base.ParameterHelper;
import com.fr.data.impl.AbstractDBDataModel;
import com.fr.data.impl.DBTableData;
import com.fr.general.data.DataModel;
import com.fr.general.log.SqlInfor;
import com.fr.general.log.TimeLogger;
import com.fr.general.log.TimeLoggerInfor;
import com.fr.script.Calculator;
import com.fr.stable.ParameterProvider;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 解析数据集实际执行 SQL（非模板占位符）。
 */
public final class ReportDebugExecutedSqlResolver {

    private ReportDebugExecutedSqlResolver() {
    }

    public static String resolve(DBTableData db, String datasetName, TemplateSessionIDInfo session,
                          String templateSql, boolean executed) {
        return resolve(db, datasetName, session, templateSql, executed, null);
    }

    public static String resolve(DBTableData db, String datasetName, TemplateSessionIDInfo session,
                          String templateSql, boolean executed, SnapshotBuildContext buildContext) {
        if (db == null) {
            return safe(templateSql);
        }
        String template = safe(templateSql);
        if (buildContext != null) {
            String cacheKey = SnapshotBuildContext.sqlCacheKey(datasetName, executed, template);
            String cached = buildContext.getResolvedSql(cacheKey);
            if (cached != null) {
                return cached;
            }
            String resolved = resolveInternal(db, datasetName, session, template, executed);
            buildContext.putResolvedSql(cacheKey, resolved);
            return resolved;
        }
        return resolveInternal(db, datasetName, session, template, executed);
    }

    private static String resolveInternal(DBTableData db, String datasetName, TemplateSessionIDInfo session,
                                          String template, boolean executed) {
        if (!executed) {
            return resolvePreviewSql(db, session, template);
        }

        // 1. 已执行：优先从缓存 DataModel 读取真实下发 SQL
        String fromModel = extractSqlFromCachedModel(session, datasetName);
        if (isResolvedSql(fromModel, template)) {
            return fromModel;
        }

        // 2. 会话 SQL 日志（SqlInfor）
        String fromLogger = extractSqlFromSessionLogger(session, datasetName);
        if (isResolvedSql(fromLogger, template)) {
            return fromLogger;
        }

        // 3. DBTableData.getNewQuery（与引擎一致的参数演算）
        String fromNewQuery = resolveViaDbNewQuery(db, session);
        if (isResolvedSql(fromNewQuery, template)) {
            return fromNewQuery;
        }

        // 4. ParameterHelper 模板替换（${param} → 执行值）
        String fromTemplate = resolveViaParameterTemplate(db, session, template);
        if (isResolvedSql(fromTemplate, template)) {
            return fromTemplate;
        }

        return StringUtils.isNotBlank(fromNewQuery) ? fromNewQuery : template;
    }

    private static String resolvePreviewSql(DBTableData db, TemplateSessionIDInfo session, String template) {
        String preview = resolveViaParameterTemplate(db, session, template);
        if (isResolvedSql(preview, template)) {
            return preview;
        }
        return template;
    }

    public static boolean hasExecutedSqlInSessionLogger(TemplateSessionIDInfo session, String datasetName) {
        if (session == null || StringUtils.isBlank(datasetName)) {
            return false;
        }
        String sql = extractSqlFromSessionLogger(session, datasetName);
        return StringUtils.isNotBlank(sql) && !containsPlaceholder(sql);
    }

    private static String extractSqlFromCachedModel(TemplateSessionIDInfo session, String datasetName) {
        try {
            String cacheKey = DatasetExecutionDetector.resolveCachedModelKey(datasetName, session);
            if (StringUtils.isBlank(cacheKey)) {
                return null;
            }
            DataModel model = session.getCachedDataModel(cacheKey);
            return extractSqlField(model);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extractSqlField(DataModel model) {
        if (model == null) {
            return null;
        }
        if (!(model instanceof AbstractDBDataModel)) {
            return null;
        }
        try {
            Field sqlField = AbstractDBDataModel.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            Object value = sqlField.get(model);
            return value == null ? null : String.valueOf(value).trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extractSqlFromSessionLogger(TemplateSessionIDInfo session, String datasetName) {
        String[] keys = new String[]{
                datasetName,
                TimeLoggerInfor.SQL,
                datasetName + TimeLoggerInfor.SQL
        };
        for (String key : keys) {
            String sql = extractSqlFromLogger(session.getLoggerProperty(key));
            if (StringUtils.isNotBlank(sql)) {
                return sql;
            }
        }
        return null;
    }

    private static String extractSqlFromLogger(TimeLogger logger) {
        if (logger == null) {
            return null;
        }
        try {
            Field inforField = findInforField(logger.getClass());
            if (inforField != null) {
                inforField.setAccessible(true);
                Object infor = inforField.get(logger);
                String fromInfor = extractSqlFromTimeLoggerInfor(infor);
                if (StringUtils.isNotBlank(fromInfor)) {
                    return fromInfor;
                }
            }
        } catch (Throwable ignored) {
        }
        return extractSqlFragment(String.valueOf(logger));
    }

    private static Field findInforField(Class<?> type) {
        Class<?> c = type;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField("infor");
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    public static String resolvePublicSqlFromInfor(Object infor) {
        return extractSqlFromTimeLoggerInfor(infor);
    }

    private static String extractSqlFromTimeLoggerInfor(Object infor) {
        if (!(infor instanceof TimeLoggerInfor)) {
            return null;
        }
        try {
            Field sqlInforField = TimeLoggerInfor.class.getDeclaredField("sqlInfor");
            sqlInforField.setAccessible(true);
            Object sqlInfor = sqlInforField.get(infor);
            if (sqlInfor instanceof SqlInfor) {
                Field sqlField = SqlInfor.class.getDeclaredField("sql");
                sqlField.setAccessible(true);
                Object sql = sqlField.get(sqlInfor);
                if (sql != null && StringUtils.isNotBlank(String.valueOf(sql))) {
                    return String.valueOf(sql).trim();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Field sqlField = TimeLoggerInfor.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            Object sql = sqlField.get(infor);
            if (sql != null && StringUtils.isNotBlank(String.valueOf(sql))) {
                return String.valueOf(sql).trim();
            }
        } catch (Throwable ignored) {
        }
        return extractSqlFragment(String.valueOf(infor));
    }

    private static String resolveViaDbNewQuery(DBTableData db, TemplateSessionIDInfo session) {
        try {
            Calculator calculator = createExecuteCalculator(session);
            Parameter[] params = invokeProcessParameters(db, calculator);
            if (params == null) {
                return null;
            }
            Method getNewQuery = DBTableData.class.getDeclaredMethod("getNewQuery", Parameter[].class);
            getNewQuery.setAccessible(true);
            Object sql = getNewQuery.invoke(db, new Object[]{params});
            return sql == null ? null : String.valueOf(sql).trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Parameter[] invokeProcessParameters(DBTableData db, Calculator calculator) {
        try {
            for (Method method : DBTableData.class.getDeclaredMethods()) {
                if (!"processParameters".equals(method.getName())) {
                    continue;
                }
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                if (method.getReturnType() != Parameter[].class) {
                    continue;
                }
                method.setAccessible(true);
                return (Parameter[]) method.invoke(db, calculator);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String resolveViaParameterTemplate(DBTableData db, TemplateSessionIDInfo session, String template) {
        if (StringUtils.isBlank(template)) {
            return null;
        }
        try {
            Calculator calculator = createExecuteCalculator(session);
            ParameterProvider[] providers = db.getParameters(calculator);
            if (providers == null || providers.length == 0) {
                providers = db.getParameters();
            }
            if (providers != null && providers.length > 0) {
                providers = Calculator.processParameters(calculator, providers);
            }
            String sql = ParameterHelper.analyze4Templatee(template, providers);
            if (StringUtils.isNotBlank(sql)) {
                return sql.trim();
            }
            sql = ParameterHelper.analyzeCurrentContextTableData4Templatee(template, providers);
            if (StringUtils.isNotBlank(sql)) {
                return sql.trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Calculator createExecuteCalculator(TemplateSessionIDInfo session) {
        Map<String, Object> executeMap = null;
        try {
            executeMap = session.getParameterMap4ExecuteAll();
            if (executeMap == null || executeMap.isEmpty()) {
                executeMap = session.getParameterMap4Execute();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (executeMap != null && !executeMap.isEmpty()) {
                return session.createSessionCalculator(null, null, executeMap);
            }
        } catch (Throwable ignored) {
        }
        Calculator calculator = session.createSessionCalculator(null, null);
        try {
            Map<String, Object> all = session.getAllPara();
            if (all != null) {
                for (Map.Entry<String, Object> entry : all.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        calculator.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return calculator;
    }

    /**
     * 判断是否为“已替换参数”的可执行 SQL，而非仍含占位符的模板。
     */
    private static boolean isResolvedSql(String sql, String template) {
        if (StringUtils.isBlank(sql)) {
            return false;
        }
        if (!containsPlaceholder(sql)) {
            return true;
        }
        return StringUtils.isNotBlank(template) && !sql.equals(template);
    }

    private static boolean containsPlaceholder(String sql) {
        if (sql == null) {
            return false;
        }
        return sql.indexOf("${") >= 0 || sql.indexOf("$[") >= 0;
    }

    private static String extractSqlFragment(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String lower = text.toLowerCase();
        String[] keys = new String[]{"select ", "insert ", "update ", "delete ", "with "};
        int best = -1;
        for (String key : keys) {
            int idx = lower.indexOf(key);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        if (best >= 0) {
            return text.substring(best).trim();
        }
        return null;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
