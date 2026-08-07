package com.fr.plugin.report.debug.core.fetch;
import com.fr.plugin.report.debug.core.config.DebugAssistantTraceGate;
import com.fr.plugin.report.debug.core.snapshot.ReportDebugExecutedSqlResolver;

import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;
import com.fr.plugin.report.debug.core.registry.PendingFetchRegistry;

import com.fr.general.log.TimeLoggerInfor;
import com.fr.log.FetchDataTimeLogger;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.lang.reflect.Field;
import java.util.LinkedList;

/**
 * 从 {@link FetchDataTimeLogger} / {@link SessionLocalSqlProbe} 提取 SQL 与耗时并登记。
 */
final class FetchExecutionRecorder {

    private FetchExecutionRecorder() {
    }

    public static void recordFromLogger(FetchDataTimeLogger logger) {
        if (logger == null || !DebugAssistantTraceGate.shouldTraceCurrentFetch()) {
            return;
        }
        String sessionId = resolveSessionId();
        if (StringUtils.isBlank(sessionId)) {
            recordPendingFallback(logger);
            return;
        }
        SessionLocalSqlProbe.captureToRegistry(sessionId);
        recordCurrentInfor(sessionId, logger);
        LinkedList<?> history = readInforList(logger);
        if (history != null) {
            for (Object item : history) {
                if (item instanceof TimeLoggerInfor) {
                    recordInfor(sessionId, (TimeLoggerInfor) item, readSqlTimeField(logger));
                }
            }
        }
    }

    private static void recordCurrentInfor(String sessionId, FetchDataTimeLogger logger) {
        TimeLoggerInfor infor = readField(logger, "infor", TimeLoggerInfor.class);
        recordInfor(sessionId, infor, readSqlTimeField(logger));
    }

    private static void recordInfor(String sessionId, TimeLoggerInfor infor, Long fallbackSqlTimeMs) {
        FetchExecutionContext.State ctx = FetchExecutionContext.current();
        String datasetName = ctx != null ? ctx.datasetName : null;
        if (StringUtils.isBlank(datasetName)) {
            datasetName = resolveDatasetName(infor);
        }
        String sql = ReportDebugExecutedSqlResolver.resolvePublicSqlFromInfor(infor);
        if (StringUtils.isBlank(sql)) {
            sql = readLoggerSqlFallback();
        }
        if (StringUtils.isBlank(datasetName) && StringUtils.isBlank(sql)) {
            return;
        }
        Long sqlTimeMs = fallbackSqlTimeMs;
        if (sqlTimeMs == null || sqlTimeMs <= 0) {
            sqlTimeMs = null;
        }
        if (StringUtils.isNotBlank(datasetName)) {
            DatasetSqlExecutionRegistry.record(sessionId, datasetName, sql, sqlTimeMs);
            mirrorToSession(sessionId, datasetName);
            return;
        }
        if (StringUtils.isNotBlank(sql)) {
            DatasetSqlExecutionRegistry.record(sessionId, sql, sql, sqlTimeMs);
        }
    }

    private static String readLoggerSqlFallback() {
        try {
            String sql = SessionLocalManagerHelper.getSql();
            if (StringUtils.isNotBlank(sql)) {
                return sql.trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void mirrorToSession(String sessionId, String datasetName) {
        try {
            TemplateSessionIDInfo session = SessionIdResolver.loadSession(sessionId);
            if (session == null) {
                return;
            }
            DatasetSqlExecutionRegistry.ExecutionRecord record = DatasetSqlExecutionRegistry.lookup(sessionId, datasetName);
            if (record == null || StringUtils.isBlank(record.sql)) {
                return;
            }
            FetchDataTimeLogger stub = new FetchDataTimeLogger();
            stub.setDsName(datasetName);
            stub.setSql(record.sql);
            session.putLoggerProperty(datasetName, stub);
            session.putLoggerProperty(datasetName + TimeLoggerInfor.SQL, stub);
        } catch (Throwable ignored) {
        }
    }

    private static String resolveDatasetName(TimeLoggerInfor infor) {
        if (infor == null) {
            return null;
        }
        String name = readField(infor, "name", String.class);
        if (StringUtils.isNotBlank(name) && !TimeLoggerInfor.SQL.equals(name)) {
            return name.trim();
        }
        try {
            Field field = TimeLoggerInfor.class.getDeclaredField("sqlInfor");
            field.setAccessible(true);
            Object sqlInfor = field.get(infor);
            if (sqlInfor != null) {
                Field dsField = sqlInfor.getClass().getDeclaredField("dsName");
                dsField.setAccessible(true);
                Object ds = dsField.get(sqlInfor);
                if (ds != null && StringUtils.isNotBlank(String.valueOf(ds))) {
                    return String.valueOf(ds).trim();
                }
            }
        } catch (Throwable ignored) {
        }
        return name == null ? null : name.trim();
    }

    private static void recordPendingFallback(FetchDataTimeLogger logger) {
        FetchExecutionContext.State ctx = FetchExecutionContext.current();
        String datasetName = ctx == null ? null : ctx.datasetName;
        TimeLoggerInfor infor = readField(logger, "infor", TimeLoggerInfor.class);
        String sql = ReportDebugExecutedSqlResolver.resolvePublicSqlFromInfor(infor);
        if (StringUtils.isBlank(sql)) {
            sql = readLoggerSqlFallback();
        }
        Long sqlTimeMs = readSqlTimeField(logger);
        PendingFetchRegistry.add(datasetName, sql, sqlTimeMs);
    }

    private static String resolveSessionId() {
        FetchExecutionContext.State ctx = FetchExecutionContext.current();
        if (ctx != null && StringUtils.isNotBlank(ctx.sessionId)) {
            return ctx.sessionId;
        }
        return SessionIdResolver.resolveOnFetchThread();
    }

    private static Long readSqlTimeField(FetchDataTimeLogger logger) {
        try {
            Field field = FetchDataTimeLogger.class.getDeclaredField("sqlTime");
            field.setAccessible(true);
            long value = field.getLong(logger);
            if (value > 0) {
                return value;
            }
        } catch (Throwable ignored) {
        }
        try {
            long local = SessionLocalManagerHelper.getSqlTime();
            return local > 0 ? local : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static LinkedList<?> readInforList(FetchDataTimeLogger logger) {
        return readField(logger, "timeLoggerInfors", LinkedList.class);
    }

    private static <T> T readField(Object target, String fieldName, Class<T> type) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
