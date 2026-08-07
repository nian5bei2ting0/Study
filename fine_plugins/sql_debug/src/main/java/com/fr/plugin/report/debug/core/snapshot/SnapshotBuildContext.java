package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.probe.ExecutedSqlTimeIndex;

import com.fr.script.Calculator;
import com.fr.web.core.TemplateSessionIDInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 单次快照构建共享上下文：ESD 索引、SQL 解析缓存、参数 Calculator 复用。
 */
public final class SnapshotBuildContext {

    private final String relativePath;
    private final ExecutedSqlTimeIndex sqlTimeIndex;
    private final Map<String, String> resolvedSqlCache = new HashMap<String, String>();
    private Calculator parameterCalculator;

    public SnapshotBuildContext(TemplateSessionIDInfo session) {
        String path = session == null || session.getRelativePath() == null ? "" : session.getRelativePath();
        this.relativePath = path;
        this.sqlTimeIndex = ExecutedSqlTimeIndex.build(relativePath);
    }

    public Long lookupSqlTimeMs(String datasetName) {
        return sqlTimeIndex.lookup(datasetName);
    }

    public Long lookupSqlTimeMsBySql(String resolvedSql) {
        return sqlTimeIndex.lookupBySqlDigest(resolvedSql);
    }

    public boolean hasEsdExecutionRecord(String datasetName) {
        return sqlTimeIndex.hasExecutionRecord(datasetName);
    }

    public boolean hasEsdExecutionBySql(String resolvedSql) {
        return sqlTimeIndex.hasExecutionBySqlDigest(resolvedSql);
    }

    public boolean hasGlobalEsdExecutionRecord(String datasetName) {
        return sqlTimeIndex.hasGlobalExecutionRecord(datasetName);
    }

    public boolean hasGlobalEsdExecutionBySql(String resolvedSql) {
        return sqlTimeIndex.hasGlobalExecutionBySqlDigest(resolvedSql);
    }

    public Long lookupGlobalSqlTimeMs(String datasetName) {
        return sqlTimeIndex.lookupGlobal(datasetName);
    }

    public Long lookupGlobalSqlTimeMsBySql(String resolvedSql) {
        return sqlTimeIndex.lookupGlobalBySqlDigest(resolvedSql);
    }

    String getResolvedSql(String cacheKey) {
        return resolvedSqlCache.get(cacheKey);
    }

    void putResolvedSql(String cacheKey, String sql) {
        if (cacheKey != null && sql != null) {
            resolvedSqlCache.put(cacheKey, sql);
        }
    }

    public static String sqlCacheKey(String datasetName, boolean executed, String templateSql) {
        String tpl = templateSql == null ? "" : templateSql;
        return datasetName + "|" + executed + "|" + tpl.hashCode();
    }

    Calculator getOrCreateParameterCalculator(TemplateSessionIDInfo session) {
        if (parameterCalculator != null) {
            return parameterCalculator;
        }
        parameterCalculator = ReportDebugDatasetInspector.createParameterCalculator(session);
        return parameterCalculator;
    }
}
