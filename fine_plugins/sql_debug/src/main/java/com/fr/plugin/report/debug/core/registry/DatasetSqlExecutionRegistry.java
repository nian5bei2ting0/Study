package com.fr.plugin.report.debug.core.registry;
import com.fr.plugin.report.debug.core.probe.DatasetExecutionDetector;
import com.fr.plugin.report.debug.core.util.SqlFingerprintUtil;

import com.fr.data.impl.AbstractDBDataModel;
import com.fr.data.impl.SharedDBDataModel;
import com.fr.general.data.DataModel;
import com.fr.measure.metric.DBMetric;
import com.fr.stable.StringUtils;
import com.fr.stable.bridge.ObjectHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件侧记录：报表取数时写入，快照时读取（弥补 session 未暴露 cachedDataModels 的场景）。
 */
public final class DatasetSqlExecutionRegistry {

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, ExecutionRecord>> BY_SESSION =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, ExecutionRecord>>();

    private DatasetSqlExecutionRegistry() {
    }

    public static void record(String sessionId, String datasetName, ObjectHolder holder) {
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(datasetName) || holder == null) {
            return;
        }
        try {
            DataModel model = holder.get(DataModel.class);
            if (model != null) {
                record(sessionId, datasetName, model);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void record(String sessionId, String datasetName, DataModel model) {
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(datasetName) || model == null) {
            return;
        }
        ExecutionRecord record = buildRecord(datasetName, model);
        if (record == null) {
            return;
        }
        store(sessionId, record);
    }

    public static void record(String sessionId, String datasetName, String sql, Long sqlTimeMs) {
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(datasetName)) {
            return;
        }
        if (StringUtils.isBlank(sql) && (sqlTimeMs == null || sqlTimeMs <= 0)) {
            return;
        }
        ExecutionRecord record = new ExecutionRecord();
        record.datasetName = datasetName;
        record.cacheKey = datasetName;
        record.sql = sql;
        record.sqlTimeMs = sqlTimeMs;
        record.recordedAt = System.currentTimeMillis();
        store(sessionId, record);
    }

    private static void store(String sessionId, ExecutionRecord record) {
        ConcurrentHashMap<String, ExecutionRecord> map = BY_SESSION.get(sessionId);
        if (map == null) {
            map = new ConcurrentHashMap<String, ExecutionRecord>();
            ConcurrentHashMap<String, ExecutionRecord> existing = BY_SESSION.putIfAbsent(sessionId, map);
            if (existing != null) {
                map = existing;
            }
        }
        map.put(record.datasetName, record);
        if (!record.datasetName.equals(record.cacheKey)) {
            map.put(record.cacheKey, record);
        }
    }

    public static ExecutionRecord lookup(String sessionId, String datasetName) {
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(datasetName)) {
            return null;
        }
        ConcurrentHashMap<String, ExecutionRecord> map = BY_SESSION.get(sessionId);
        if (map == null) {
            return null;
        }
        ExecutionRecord hit = map.get(datasetName);
        if (hit != null) {
            return hit;
        }
        for (Map.Entry<String, ExecutionRecord> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (DatasetExecutionDetector.datasetNamesMatch(entry.getKey(), datasetName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** 按已替换 SQL 指纹匹配登记（取数时未带上数据集名、或以 SQL 为键写入时）。 */
    public static ExecutionRecord lookupByResolvedSql(String sessionId, String resolvedSql) {
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(resolvedSql)) {
            return null;
        }
        ConcurrentHashMap<String, ExecutionRecord> map = BY_SESSION.get(sessionId);
        if (map == null || map.isEmpty()) {
            return null;
        }
        String fingerprint = SqlFingerprintUtil.extractFingerprint(resolvedSql);
        ExecutionRecord best = null;
        for (ExecutionRecord record : map.values()) {
            if (record == null || StringUtils.isBlank(record.sql)) {
                continue;
            }
            if (sqlMatchesRecord(resolvedSql, fingerprint, record)) {
                if (best == null || preferRecord(record, best)) {
                    best = record;
                }
            }
        }
        return best;
    }

    private static boolean sqlMatchesRecord(String resolvedSql, String fingerprint, ExecutionRecord record) {
        if (SqlFingerprintUtil.sqlMatchesFingerprint(record.sql, fingerprint)) {
            return true;
        }
        String recordFp = SqlFingerprintUtil.extractFingerprint(record.sql);
        return StringUtils.isNotBlank(recordFp)
                && SqlFingerprintUtil.sqlMatchesFingerprint(resolvedSql, recordFp);
    }

    private static boolean preferRecord(ExecutionRecord candidate, ExecutionRecord current) {
        boolean candidateHasTime = candidate.sqlTimeMs != null && candidate.sqlTimeMs > 0;
        boolean currentHasTime = current.sqlTimeMs != null && current.sqlTimeMs > 0;
        if (candidateHasTime && !currentHasTime) {
            return true;
        }
        return candidate.recordedAt > current.recordedAt;
    }

    public static void clearSession(String sessionId) {
        if (StringUtils.isNotBlank(sessionId)) {
            BY_SESSION.remove(sessionId);
        }
    }

    public static int entryCount(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return 0;
        }
        ConcurrentHashMap<String, ExecutionRecord> map = BY_SESSION.get(sessionId);
        return map == null ? 0 : map.size();
    }

    private static ExecutionRecord buildRecord(String datasetName, DataModel model) {
        AbstractDBDataModel dbModel = unwrapDbModel(model);
        String sql = dbModel != null ? readSql(dbModel) : null;
        Long sqlTimeMs = null;
        try {
            DBMetric metric = model.getMetric();
            sqlTimeMs = pickMetricTimeMs(metric);
        } catch (Throwable ignored) {
        }
        if (StringUtils.isBlank(sql) && sqlTimeMs == null) {
            return null;
        }
        ExecutionRecord record = new ExecutionRecord();
        record.datasetName = datasetName;
        record.cacheKey = datasetName;
        record.sql = sql;
        record.sqlTimeMs = sqlTimeMs;
        record.recordedAt = System.currentTimeMillis();
        return record;
    }

    private static AbstractDBDataModel unwrapDbModel(DataModel model) {
        if (model instanceof AbstractDBDataModel) {
            return (AbstractDBDataModel) model;
        }
        if (model instanceof SharedDBDataModel) {
            try {
                java.lang.reflect.Field field = SharedDBDataModel.class.getDeclaredField("resultSet");
                field.setAccessible(true);
                Object inner = field.get(model);
                if (inner instanceof AbstractDBDataModel) {
                    return (AbstractDBDataModel) inner;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String readSql(AbstractDBDataModel model) {
        try {
            java.lang.reflect.Field sqlField = AbstractDBDataModel.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            Object value = sqlField.get(model);
            return value == null ? null : String.valueOf(value).trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Long pickMetricTimeMs(DBMetric metric) {
        if (metric == null) {
            return null;
        }
        try {
            long sqlTime = metric.getSqlTime();
            if (sqlTime > 0) {
                return sqlTime;
            }
        } catch (Throwable ignored) {
        }
        try {
            long queryTime = metric.getQueryTime();
            if (queryTime > 0) {
                return queryTime;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static final class ExecutionRecord {
        public String datasetName;
        public String cacheKey;
        public String sql;
        public Long sqlTimeMs;
        public long recordedAt;
    }
}
