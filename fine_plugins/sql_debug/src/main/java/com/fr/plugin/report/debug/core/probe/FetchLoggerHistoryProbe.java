package com.fr.plugin.report.debug.core.probe;
import com.fr.plugin.report.debug.core.snapshot.ReportDebugExecutedSqlResolver;
import com.fr.plugin.report.debug.core.snapshot.ReportDebugDatasetInspector;

import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;
import com.fr.plugin.report.debug.core.util.SqlFingerprintUtil;

import com.fr.general.log.SqlInfor;
import com.fr.general.log.TimeLogger;
import com.fr.general.log.TimeLoggerInfor;
import com.fr.log.FetchDataTimeLogger;
import com.fr.log.TimeLoggerProperties;
import com.fr.script.Calculator;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 从会话 Calculator / Logger 属性中回溯已执行 SQL（FetchDataTimeLogger 的 infor 链表）。
 */
public final class FetchLoggerHistoryProbe {

    private FetchLoggerHistoryProbe() {
    }

    public static DatasetSqlExecutionRegistry.ExecutionRecord lookup(TemplateSessionIDInfo session, String datasetName) {
        return lookup(session, datasetName, null);
    }

    public static DatasetSqlExecutionRegistry.ExecutionRecord lookup(TemplateSessionIDInfo session, String datasetName,
                                                              String resolvedSql) {
        if (session == null || StringUtils.isBlank(datasetName)) {
            return null;
        }
        String fingerprint = SqlFingerprintUtil.extractFingerprint(resolvedSql);
        DatasetSqlExecutionRegistry.ExecutionRecord best = null;
        best = merge(best, scanCalculator(session, datasetName, fingerprint));
        best = merge(best, scanLoggerProperties(session, datasetName, fingerprint));
        best = merge(best, scanAttributeMap(session, datasetName, fingerprint));
        best = merge(best, scanSessionLoggerKeys(session, datasetName, fingerprint));
        return best;
    }

    public static Long lookupSqlTimeMs(TemplateSessionIDInfo session, String datasetName) {
        if (session == null || StringUtils.isBlank(datasetName)) {
            return null;
        }
        Long best = null;
        TimeLoggerProperties props = readLoggerProperties(session);
        if (props != null && props.properties != null) {
            for (Map.Entry<String, TimeLogger> entry : props.properties.entrySet()) {
                if (entry == null || !(entry.getValue() instanceof FetchDataTimeLogger)) {
                    continue;
                }
                if (!loggerKeyMatchesDataset(entry.getKey(), datasetName)) {
                    continue;
                }
                Long t = readSqlTimeField((FetchDataTimeLogger) entry.getValue());
                if (t != null && t > 0 && (best == null || t > best)) {
                    best = t;
                }
            }
        }
        if (best != null) {
            return best;
        }
        String[] keys = new String[]{datasetName, datasetName + TimeLoggerInfor.SQL};
        for (String key : keys) {
            try {
                TimeLogger logger = session.getLoggerProperty(key);
                if (logger instanceof FetchDataTimeLogger) {
                    Long t = readSqlTimeField((FetchDataTimeLogger) logger);
                    if (t != null && t > 0) {
                        return t;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean loggerKeyMatchesDataset(String loggerKey, String datasetName) {
        if (StringUtils.isBlank(loggerKey)) {
            return false;
        }
        if (loggerKey.equals(datasetName) || loggerKey.startsWith(datasetName)) {
            return true;
        }
        return DatasetExecutionDetector.datasetNamesMatch(loggerKey, datasetName);
    }

    public static int countInformEntries(TemplateSessionIDInfo session) {
        if (session == null) {
            return 0;
        }
        int count = 0;
        count += countLoggerInfors(scanCalculatorLogger(session));
        TimeLoggerProperties props = readLoggerProperties(session);
        if (props != null && props.properties != null) {
            for (TimeLogger logger : props.properties.values()) {
                if (logger instanceof FetchDataTimeLogger) {
                    count += countLoggerInfors((FetchDataTimeLogger) logger);
                }
            }
        }
        return count;
    }

    public static int countLoggerProperties(TemplateSessionIDInfo session) {
        TimeLoggerProperties props = readLoggerProperties(session);
        return props == null || props.properties == null ? 0 : props.properties.size();
    }

    private static FetchDataTimeLogger scanCalculatorLogger(TemplateSessionIDInfo session) {
        try {
            Calculator calculator = ReportDebugDatasetInspector.createParameterCalculator(session);
            return calculator.getAttribute(Calculator.TIME_LOGGER);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int countLoggerInfors(FetchDataTimeLogger logger) {
        if (logger == null) {
            return 0;
        }
        LinkedList<?> list = readInforList(logger);
        int count = list == null ? 0 : list.size();
        TimeLoggerInfor current = readField(logger, "infor", TimeLoggerInfor.class);
        return current == null ? count : count + 1;
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord scanCalculator(TemplateSessionIDInfo session,
                                                                                String datasetName, String fingerprint) {
        return extractFromFetchLogger(scanCalculatorLogger(session), datasetName, fingerprint);
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord scanLoggerProperties(TemplateSessionIDInfo session,
                                                                                      String datasetName,
                                                                                      String fingerprint) {
        TimeLoggerProperties props = readLoggerProperties(session);
        if (props == null || props.properties == null) {
            return null;
        }
        DatasetSqlExecutionRegistry.ExecutionRecord best = null;
        for (TimeLogger logger : props.properties.values()) {
            if (logger instanceof FetchDataTimeLogger) {
                best = merge(best, extractFromFetchLogger((FetchDataTimeLogger) logger, datasetName, fingerprint));
            }
        }
        return best;
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord scanSessionLoggerKeys(TemplateSessionIDInfo session,
                                                                                     String datasetName,
                                                                                     String fingerprint) {
        DatasetSqlExecutionRegistry.ExecutionRecord best = null;
        String[] keys = new String[]{
                datasetName,
                datasetName + TimeLoggerInfor.SQL,
                TimeLoggerInfor.SQL
        };
        for (String key : keys) {
            try {
                TimeLogger logger = session.getLoggerProperty(key);
                if (logger instanceof FetchDataTimeLogger) {
                    best = merge(best, extractFromFetchLogger((FetchDataTimeLogger) logger, datasetName, fingerprint));
                } else if (logger != null) {
                    best = merge(best, matchGenericLogger(logger, datasetName, fingerprint));
                }
            } catch (Throwable ignored) {
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private static DatasetSqlExecutionRegistry.ExecutionRecord scanAttributeMap(TemplateSessionIDInfo session,
                                                                                  String datasetName,
                                                                                  String fingerprint) {
        try {
            Field field = TemplateSessionIDInfo.class.getDeclaredField("attributeMap");
            field.setAccessible(true);
            Object value = field.get(session);
            if (!(value instanceof Map)) {
                return null;
            }
            DatasetSqlExecutionRegistry.ExecutionRecord best = null;
            for (Object entryValue : ((Map<?, ?>) value).values()) {
                if (entryValue instanceof FetchDataTimeLogger) {
                    best = merge(best, extractFromFetchLogger((FetchDataTimeLogger) entryValue, datasetName, fingerprint));
                }
            }
            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord extractFromFetchLogger(FetchDataTimeLogger logger,
                                                                                        String datasetName,
                                                                                        String fingerprint) {
        if (logger == null) {
            return null;
        }
        DatasetSqlExecutionRegistry.ExecutionRecord best = null;
        LinkedList<?> list = readInforList(logger);
        if (list != null) {
            for (Object item : list) {
                if (!(item instanceof TimeLoggerInfor)) {
                    continue;
                }
                best = merge(best, matchInfor((TimeLoggerInfor) item, datasetName, fingerprint));
            }
        }
        TimeLoggerInfor current = readField(logger, "infor", TimeLoggerInfor.class);
        best = merge(best, matchInfor(current, datasetName, fingerprint));
        Long sqlTime = readSqlTimeField(logger);
        if (best != null && best.sqlTimeMs == null && sqlTime != null) {
            best.sqlTimeMs = sqlTime;
        }
        return best;
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord matchGenericLogger(TimeLogger logger, String datasetName,
                                                                                    String fingerprint) {
        try {
            Field inforField = logger.getClass().getDeclaredField("infor");
            inforField.setAccessible(true);
            Object infor = inforField.get(logger);
            if (infor instanceof TimeLoggerInfor) {
                return matchInfor((TimeLoggerInfor) infor, datasetName, fingerprint);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord matchInfor(TimeLoggerInfor infor, String datasetName,
                                                                            String fingerprint) {
        if (infor == null) {
            return null;
        }
        String dsName = readField(infor, "name", String.class);
        if (StringUtils.isBlank(dsName)) {
            dsName = readInforDsName(infor);
        }
        String sql = extractSqlFromInfor(infor);
        if (StringUtils.isBlank(sql)) {
            return null;
        }
        boolean nameMatch = datasetNameMatches(dsName, datasetName);
        boolean sqlMatch = StringUtils.isNotBlank(fingerprint)
                && SqlFingerprintUtil.sqlMatchesFingerprint(sql, fingerprint);
        if (!nameMatch && !sqlMatch) {
            return null;
        }
        DatasetSqlExecutionRegistry.ExecutionRecord record = new DatasetSqlExecutionRegistry.ExecutionRecord();
        record.datasetName = datasetName;
        record.cacheKey = datasetName;
        record.sql = sql;
        return record;
    }

    private static String extractSqlFromInfor(TimeLoggerInfor infor) {
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
        return ReportDebugExecutedSqlResolver.resolvePublicSqlFromInfor(infor);
    }

    private static String readInforDsName(TimeLoggerInfor infor) {
        try {
            Field field = TimeLoggerInfor.class.getDeclaredField("sqlInfor");
            field.setAccessible(true);
            Object sqlInfor = field.get(infor);
            if (sqlInfor instanceof SqlInfor) {
                Field dsField = SqlInfor.class.getDeclaredField("dsName");
                dsField.setAccessible(true);
                Object ds = dsField.get(sqlInfor);
                return ds == null ? null : String.valueOf(ds).trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static LinkedList<?> readInforList(FetchDataTimeLogger logger) {
        return readField(logger, "timeLoggerInfors", LinkedList.class);
    }

    private static Long readSqlTimeField(FetchDataTimeLogger logger) {
        try {
            Field field = FetchDataTimeLogger.class.getDeclaredField("sqlTime");
            field.setAccessible(true);
            long value = field.getLong(logger);
            return value > 0 ? value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static TimeLoggerProperties readLoggerProperties(TemplateSessionIDInfo session) {
        return readField(session, "loggerProperties", TimeLoggerProperties.class);
    }

    private static boolean datasetNameMatches(String candidate, String datasetName) {
        if (StringUtils.isBlank(candidate) || StringUtils.isBlank(datasetName)) {
            return false;
        }
        if (candidate.equals(datasetName) || candidate.equalsIgnoreCase(datasetName)) {
            return true;
        }
        return DatasetExecutionDetector.datasetNamesMatch(candidate, datasetName);
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord merge(
            DatasetSqlExecutionRegistry.ExecutionRecord a,
            DatasetSqlExecutionRegistry.ExecutionRecord b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (StringUtils.isBlank(a.sql) && StringUtils.isNotBlank(b.sql)) {
            a.sql = b.sql;
        }
        if (a.sqlTimeMs == null && b.sqlTimeMs != null) {
            a.sqlTimeMs = b.sqlTimeMs;
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String fieldName, Class<T> type) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            if (type.isInstance(value)) {
                return (T) value;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
