package com.fr.plugin.report.debug.core.probe;
import com.fr.plugin.report.debug.core.util.SqlFingerprintUtil;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;

import com.fr.esd.cache.runtime.ExecutedTableDataInfo;
import com.fr.esd.cache.runtime.ExecutedTableDataInfoManager;
import com.fr.stable.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 单次快照构建内复用：将 ESD 执行记录按数据集名 / SQL 摘要索引（一次扫描，含路径内与全局 digest）。
 */
public final class ExecutedSqlTimeIndex {

    private final Map<String, Long> sqlTimeByDataset;
    private final Set<String> executedDatasetNames;
    private final Map<String, Long> sqlTimeByDigest;
    private final Set<String> executedDigests;
    private final Map<String, Long> globalSqlTimeByDataset;
    private final Map<String, Long> globalSqlTimeByDigest;
    private final Set<String> globalExecutedDigests;

    private ExecutedSqlTimeIndex(Map<String, Long> sqlTimeByDataset, Set<String> executedDatasetNames,
                                 Map<String, Long> sqlTimeByDigest, Set<String> executedDigests,
                                 Map<String, Long> globalSqlTimeByDataset,
                                 Map<String, Long> globalSqlTimeByDigest, Set<String> globalExecutedDigests) {
        this.sqlTimeByDataset = sqlTimeByDataset;
        this.executedDatasetNames = executedDatasetNames;
        this.sqlTimeByDigest = sqlTimeByDigest;
        this.executedDigests = executedDigests;
        this.globalSqlTimeByDataset = globalSqlTimeByDataset;
        this.globalSqlTimeByDigest = globalSqlTimeByDigest;
        this.globalExecutedDigests = globalExecutedDigests;
    }

    public static ExecutedSqlTimeIndex build(String relativePath) {
        Map<String, Long> map = new HashMap<String, Long>();
        Set<String> executed = new HashSet<String>();
        Map<String, Long> digestMap = new HashMap<String, Long>();
        Set<String> digestExecuted = new HashSet<String>();
        Map<String, Long> globalMap = new HashMap<String, Long>();
        Map<String, Long> globalDigestMap = new HashMap<String, Long>();
        Set<String> globalDigestExecuted = new HashSet<String>();
        try {
            Collection<ExecutedTableDataInfo> list = ExecutedTableDataInfoManager.getInstance().getList();
            if (list == null) {
                return emptyIndex(map, executed, digestMap, digestExecuted, globalMap, globalDigestMap,
                        globalDigestExecuted);
            }
            for (ExecutedTableDataInfo info : list) {
                if (info == null) {
                    continue;
                }
                indexGlobalEntry(info, globalMap, globalDigestMap, globalDigestExecuted);
                if (!pathMatches(relativePath, info.getPath())) {
                    continue;
                }
                indexScopedEntry(info, map, executed, digestMap, digestExecuted);
            }
        } catch (Throwable ignored) {
        }
        return new ExecutedSqlTimeIndex(map, executed, digestMap, digestExecuted, globalMap, globalDigestMap,
                globalDigestExecuted);
    }

    private static void indexScopedEntry(ExecutedTableDataInfo info, Map<String, Long> map, Set<String> executed,
                                         Map<String, Long> digestMap, Set<String> digestExecuted) {
        if (StringUtils.isNotBlank(info.getDsName())) {
            executed.add(info.getDsName());
        }
        if (info.getRow() > 0 && StringUtils.isNotBlank(info.getDsName())) {
            executed.add(info.getDsName());
        }
        long t = info.getSqlTime();
        if (t > 0 && StringUtils.isNotBlank(info.getDsName())) {
            mergeTime(map, info.getDsName(), t);
        }
        if (t > 0 && StringUtils.isNotBlank(info.getSqlDigest())) {
            mergeTime(digestMap, info.getSqlDigest().toLowerCase(), t);
        }
        if (StringUtils.isNotBlank(info.getSqlDigest()) && (t > 0 || info.getRow() > 0)) {
            digestExecuted.add(info.getSqlDigest().toLowerCase());
        }
    }

    private static void indexGlobalEntry(ExecutedTableDataInfo info, Map<String, Long> globalMap,
                                         Map<String, Long> globalDigestMap, Set<String> globalDigestExecuted) {
        long t = info.getSqlTime();
        if (t > 0 && StringUtils.isNotBlank(info.getDsName())) {
            mergeTime(globalMap, info.getDsName(), t);
        }
        if (StringUtils.isNotBlank(info.getSqlDigest())) {
            if (t > 0) {
                mergeTime(globalDigestMap, info.getSqlDigest().toLowerCase(), t);
            }
            if (t > 0 || info.getRow() > 0) {
                globalDigestExecuted.add(info.getSqlDigest().toLowerCase());
            }
        }
    }

    private static ExecutedSqlTimeIndex emptyIndex(Map<String, Long> map, Set<String> executed,
                                                   Map<String, Long> digestMap, Set<String> digestExecuted,
                                                   Map<String, Long> globalMap, Map<String, Long> globalDigestMap,
                                                   Set<String> globalDigestExecuted) {
        return new ExecutedSqlTimeIndex(map, executed, digestMap, digestExecuted, globalMap, globalDigestMap,
                globalDigestExecuted);
    }

    public boolean hasExecutionBySqlDigest(String resolvedSql) {
        return containsDigest(executedDigests, resolvedSql);
    }

    public boolean hasGlobalExecutionBySqlDigest(String resolvedSql) {
        return containsDigest(globalExecutedDigests, resolvedSql);
    }

    public boolean hasExecutionRecord(String datasetName) {
        return matchesDatasetName(executedDatasetNames, datasetName);
    }

    public boolean hasGlobalExecutionRecord(String datasetName) {
        if (StringUtils.isBlank(datasetName)) {
            return false;
        }
        if (globalSqlTimeByDataset.containsKey(datasetName)) {
            return true;
        }
        return matchesDatasetName(globalSqlTimeByDataset.keySet(), datasetName);
    }

    public Long lookupBySqlDigest(String resolvedSql) {
        return lookupDigest(sqlTimeByDigest, resolvedSql);
    }

    public Long lookupGlobalBySqlDigest(String resolvedSql) {
        return lookupDigest(globalSqlTimeByDigest, resolvedSql);
    }

    public Long lookup(String datasetName) {
        return lookupDataset(sqlTimeByDataset, datasetName);
    }

    public Long lookupGlobal(String datasetName) {
        return lookupDataset(globalSqlTimeByDataset, datasetName);
    }

    private static boolean containsDigest(Set<String> digests, String resolvedSql) {
        String digest = SqlFingerprintUtil.sqlDigest(resolvedSql);
        return StringUtils.isNotBlank(digest) && digests.contains(digest.toLowerCase());
    }

    private static Long lookupDigest(Map<String, Long> map, String resolvedSql) {
        String digest = SqlFingerprintUtil.sqlDigest(resolvedSql);
        if (StringUtils.isBlank(digest)) {
            return null;
        }
        return map.get(digest.toLowerCase());
    }

    private static Long lookupDataset(Map<String, Long> map, String datasetName) {
        if (StringUtils.isBlank(datasetName)) {
            return null;
        }
        Long exact = map.get(datasetName);
        if (exact != null) {
            return exact;
        }
        Long best = null;
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            if (DatasetExecutionDetector.datasetNamesMatch(entry.getKey(), datasetName)) {
                if (best == null || entry.getValue() > best) {
                    best = entry.getValue();
                }
            }
        }
        return best;
    }

    private static boolean matchesDatasetName(Set<String> names, String datasetName) {
        if (StringUtils.isBlank(datasetName)) {
            return false;
        }
        if (names.contains(datasetName)) {
            return true;
        }
        for (String name : names) {
            if (DatasetExecutionDetector.datasetNamesMatch(name, datasetName)
                    || DatasetExecutionDetector.datasetNamesMatch(datasetName, name)) {
                return true;
            }
        }
        return false;
    }

    private static void mergeTime(Map<String, Long> map, String key, long t) {
        Long existing = map.get(key);
        if (existing == null || t > existing) {
            map.put(key, t);
        }
    }

    private static boolean pathMatches(String relativePath, String infoPath) {
        if (StringUtils.isBlank(relativePath) || StringUtils.isBlank(infoPath)) {
            return true;
        }
        if (infoPath.contains(relativePath) || relativePath.contains(infoPath)) {
            return true;
        }
        String relFile = fileName(relativePath);
        String infoFile = fileName(infoPath);
        return StringUtils.isNotBlank(relFile) && relFile.equalsIgnoreCase(infoFile);
    }

    private static String fileName(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
