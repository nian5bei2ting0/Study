package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;
import com.fr.plugin.report.debug.core.probe.DatasetExecutionDetector;
import com.fr.plugin.report.debug.core.probe.FetchLoggerHistoryProbe;
import com.fr.plugin.report.debug.core.util.SqlFingerprintUtil;

import com.fr.data.impl.AbstractDBDataModel;
import com.fr.data.impl.DBTableData;
import com.fr.data.impl.SharedDBDataModel;
import com.fr.general.data.DataModel;
import com.fr.measure.metric.DBMetric;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 统一解析数据集是否已执行及 SQL 耗时。
 */
public final class DatasetSqlMetricsResolver {

    private DatasetSqlMetricsResolver() {
    }

    public static Metrics resolve(String sessionId, String datasetName, TemplateSessionIDInfo session,
                           DBTableData dbTableData, SnapshotBuildContext buildContext,
                           boolean sqlParameterResolved, String resolvedSql) {
        Metrics metrics = new Metrics();
        if (StringUtils.isBlank(datasetName) || session == null) {
            metrics.runStatusLabel = "\u672a\u6267\u884c";
            return metrics;
        }

        // 1. 主动埋点（取数线程 FetchDataTimeLogger，不依赖抽数缓存）
        applyRecord(metrics, DatasetSqlExecutionRegistry.lookup(sessionId, datasetName));
        if (!metrics.executed) {
            applyRecord(metrics, DatasetSqlExecutionRegistry.lookupByResolvedSql(sessionId, resolvedSql));
        }
        if (!metrics.executed) {
            applyRecord(metrics, FetchLoggerHistoryProbe.lookup(session, datasetName, resolvedSql));
        }
        applyRecord(metrics, probeCachedDataModel(session, datasetName, resolvedSql));

        if (!metrics.executed) {
            metrics.executed = DatasetExecutionDetector.isDatasetExecuted(datasetName, session, buildContext);
        }

        // 2. 抽数缓存 ESD 仅作兜底（单次快照内 ExecutedSqlTimeIndex，不再重复全表扫描）
        applyEsdFallback(metrics, datasetName, resolvedSql, buildContext);

        if (metrics.executed && metrics.sqlTimeMs == null) {
            metrics.sqlTimeMs = FetchLoggerHistoryProbe.lookupSqlTimeMs(session, datasetName);
        }

        if (metrics.executed) {
            metrics.runStatusLabel = "\u5df2\u6267\u884c";
        } else if (sqlParameterResolved) {
            metrics.runStatusLabel = "\u53c2\u6570\u5df2\u66ff\u6362(\u672a\u53d6\u6570)";
        } else {
            metrics.runStatusLabel = "\u672a\u6267\u884c";
        }
        return metrics;
    }

    private static void applyEsdFallback(Metrics metrics, String datasetName, String resolvedSql,
                                         SnapshotBuildContext buildContext) {
        if (metrics.executed && metrics.sqlTimeMs != null && metrics.sqlTimeMs > 0) {
            return;
        }
        if (buildContext == null) {
            return;
        }
        if (!metrics.executed) {
            metrics.executed = buildContext.hasEsdExecutionRecord(datasetName)
                    || buildContext.hasEsdExecutionBySql(resolvedSql)
                    || buildContext.hasGlobalEsdExecutionRecord(datasetName)
                    || buildContext.hasGlobalEsdExecutionBySql(resolvedSql);
        }
        if (metrics.sqlTimeMs == null || metrics.sqlTimeMs <= 0) {
            Long fromEsd = firstPositive(
                    buildContext.lookupSqlTimeMs(datasetName),
                    buildContext.lookupSqlTimeMsBySql(resolvedSql),
                    buildContext.lookupGlobalSqlTimeMs(datasetName),
                    buildContext.lookupGlobalSqlTimeMsBySql(resolvedSql));
            if (fromEsd != null) {
                metrics.sqlTimeMs = fromEsd;
                metrics.executed = true;
            }
        }
    }

    private static Long firstPositive(Long... values) {
        for (Long value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private static void applyRecord(Metrics metrics, DatasetSqlExecutionRegistry.ExecutionRecord record) {
        if (record == null || metrics == null) {
            return;
        }
        metrics.executed = true;
        if (record.sqlTimeMs != null && record.sqlTimeMs > 0) {
            metrics.sqlTimeMs = record.sqlTimeMs;
        }
    }

    /**
     * 合并原 CachedDataModelSqlProbe 与 resolveFromCachedDataModel：按名或 SQL 指纹匹配会话缓存 DataModel。
     */
    private static DatasetSqlExecutionRegistry.ExecutionRecord probeCachedDataModel(
            TemplateSessionIDInfo session, String datasetName, String resolvedSql) {
        if (session == null || StringUtils.isBlank(datasetName)) {
            return null;
        }
        String fingerprint = SqlFingerprintUtil.extractFingerprint(resolvedSql);
        DataModel direct = loadCachedModel(datasetName, session);
        DatasetSqlExecutionRegistry.ExecutionRecord best = recordFromModel(
                datasetName, DatasetExecutionDetector.resolveCachedModelKey(datasetName, session), direct);
        if (best != null && best.sqlTimeMs != null && best.sqlTimeMs > 0) {
            return best;
        }
        Map<String, DataModel> map = DatasetExecutionDetector.readCachedDataModelsMap(session);
        if (map == null || map.isEmpty()) {
            return best;
        }
        for (Map.Entry<String, DataModel> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            DatasetSqlExecutionRegistry.ExecutionRecord candidate = recordFromModel(
                    datasetName, entry.getKey(), entry.getValue());
            if (candidate == null) {
                continue;
            }
            boolean nameMatch = DatasetExecutionDetector.datasetNamesMatch(entry.getKey(), datasetName);
            boolean sqlMatch = StringUtils.isNotBlank(fingerprint)
                    && StringUtils.isNotBlank(candidate.sql)
                    && SqlFingerprintUtil.sqlMatchesFingerprint(candidate.sql, fingerprint);
            if (!nameMatch && !sqlMatch) {
                continue;
            }
            best = mergeRecord(best, candidate);
            if (best != null && best.sqlTimeMs != null && best.sqlTimeMs > 0) {
                return best;
            }
        }
        return best;
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord recordFromModel(
            String datasetName, String cacheKey, DataModel model) {
        if (model == null) {
            return null;
        }
        String sql = readSql(model);
        Long sqlTimeMs = pickMetricTimeMs(safeMetric(model));
        if (StringUtils.isBlank(sql) && sqlTimeMs == null) {
            return null;
        }
        DatasetSqlExecutionRegistry.ExecutionRecord record = new DatasetSqlExecutionRegistry.ExecutionRecord();
        record.datasetName = datasetName;
        record.cacheKey = StringUtils.isBlank(cacheKey) ? datasetName : cacheKey;
        record.sql = sql;
        record.sqlTimeMs = sqlTimeMs;
        return record;
    }

    private static DBMetric safeMetric(DataModel model) {
        try {
            return model.getMetric();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static DatasetSqlExecutionRegistry.ExecutionRecord mergeRecord(
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

    private static DataModel loadCachedModel(String datasetName, TemplateSessionIDInfo session) {
        try {
            String key = DatasetExecutionDetector.resolveCachedModelKey(datasetName, session);
            if (StringUtils.isNotBlank(key)) {
                DataModel model = session.getCachedDataModel(key);
                if (model != null) {
                    return model;
                }
            }
        } catch (Throwable ignored) {
        }
        Map<String, DataModel> map = DatasetExecutionDetector.readCachedDataModelsMap(session);
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, DataModel> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (DatasetExecutionDetector.datasetNamesMatch(entry.getKey(), datasetName)) {
                return entry.getValue();
            }
        }
        return null;
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

    private static String readSql(DataModel model) {
        AbstractDBDataModel db = unwrapDbModel(model);
        if (db == null) {
            return null;
        }
        try {
            Field sqlField = AbstractDBDataModel.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            Object value = sqlField.get(db);
            return value == null ? null : String.valueOf(value).trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AbstractDBDataModel unwrapDbModel(DataModel model) {
        if (model instanceof AbstractDBDataModel) {
            return (AbstractDBDataModel) model;
        }
        if (model instanceof SharedDBDataModel) {
            try {
                Field field = SharedDBDataModel.class.getDeclaredField("resultSet");
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

    public static final class Metrics {
        boolean executed;
        Long sqlTimeMs;
        String runStatusLabel;
    }
}
