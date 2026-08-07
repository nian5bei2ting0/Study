package com.fr.plugin.report.debug.core.probe;
import com.fr.plugin.report.debug.core.snapshot.ReportDebugExecutedSqlResolver;

import com.fr.plugin.report.debug.core.snapshot.SnapshotBuildContext;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;

import com.fr.general.data.DataModel;
import com.fr.measure.metric.DBMetric;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 判断数据集是否已在当前预览会话中执行（兼容缓存 key 与模板名不一致的情况）。
 */
public final class DatasetExecutionDetector {

    private DatasetExecutionDetector() {
    }

    public static boolean isDatasetExecuted(String datasetName, TemplateSessionIDInfo session,
                                       SnapshotBuildContext buildContext) {
        if (StringUtils.isBlank(datasetName) || session == null) {
            return false;
        }
        if (hasCachedModel(session, datasetName)) {
            return true;
        }
        if (hasSizeMapEntry(session, datasetName)) {
            return true;
        }
        if (buildContext != null) {
            if (buildContext.hasEsdExecutionRecord(datasetName)) {
                return true;
            }
        } else if (ExecutedSqlTimeIndex.build(session.getRelativePath()).hasExecutionRecord(datasetName)) {
            return true;
        }
        if (ReportDebugExecutedSqlResolver.hasExecutedSqlInSessionLogger(session, datasetName)) {
            return true;
        }
        if (hasExecutedDbMetric(datasetName, session)) {
            return true;
        }
        return false;
    }

    private static boolean hasExecutedDbMetric(String datasetName, TemplateSessionIDInfo session) {
        Map<String, DataModel> map = readCachedDataModelsMap(session);
        if (map == null || map.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, DataModel> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!datasetNamesMatch(entry.getKey(), datasetName)) {
                continue;
            }
            try {
                DBMetric metric = entry.getValue().getMetric();
                if (metric == null) {
                    continue;
                }
                long sqlTime = metric.getSqlTime();
                long queryTime = metric.getQueryTime();
                if (sqlTime > 0 || queryTime > 0) {
                    return true;
                }
                if (StringUtils.isNotBlank(metric.getSql())) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * 返回会话中实际缓存该数据集的 key（用于读取执行后 SQL），无则 null。
     */
    public static String resolveCachedModelKey(String datasetName, TemplateSessionIDInfo session) {
        if (StringUtils.isBlank(datasetName) || session == null) {
            return null;
        }
        try {
            if (session.getCachedDataModel(datasetName) != null) {
                return datasetName;
            }
        } catch (Throwable ignored) {
        }
        Map<String, DataModel> map = readCachedDataModelsMap(session);
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, DataModel> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            String key = entry.getKey();
            if (datasetNamesMatch(key, datasetName)) {
                return key;
            }
        }
        return null;
    }

    private static boolean hasCachedModel(TemplateSessionIDInfo session, String datasetName) {
        return resolveCachedModelKey(datasetName, session) != null;
    }

    private static boolean hasSizeMapEntry(TemplateSessionIDInfo session, String datasetName) {
        Map<String, Integer> sizeMap = session.getTableDataSizeMap();
        if (sizeMap == null || sizeMap.isEmpty()) {
            return false;
        }
        if (sizeMap.containsKey(datasetName)) {
            Integer size = sizeMap.get(datasetName);
            return size != null && size >= 0;
        }
        for (Map.Entry<String, Integer> entry : sizeMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue() < 0) {
                continue;
            }
            if (datasetNamesMatch(entry.getKey(), datasetName)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, DataModel> readCachedDataModelsMap(TemplateSessionIDInfo session) {
        try {
            Field field = TemplateSessionIDInfo.class.getDeclaredField("cachedDataModels");
            field.setAccessible(true);
            Object value = field.get(session);
            if (value instanceof Map) {
                return (Map<String, DataModel>) value;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean datasetNamesMatch(String cacheKey, String datasetName) {
        if (StringUtils.isBlank(cacheKey) || StringUtils.isBlank(datasetName)) {
            return false;
        }
        if (cacheKey.equals(datasetName)) {
            return true;
        }
        if (cacheKey.equalsIgnoreCase(datasetName)) {
            return true;
        }
        String key = cacheKey.trim();
        String name = datasetName.trim();
        if (key.length() <= name.length()) {
            return false;
        }
        int idx = key.toLowerCase().lastIndexOf(name.toLowerCase());
        if (idx != key.length() - name.length()) {
            return false;
        }
        if (idx == 0) {
            return true;
        }
        char sep = key.charAt(idx - 1);
        return sep == '.' || sep == '#' || sep == '/' || sep == '\\' || sep == '@' || sep == ':';
    }
}
