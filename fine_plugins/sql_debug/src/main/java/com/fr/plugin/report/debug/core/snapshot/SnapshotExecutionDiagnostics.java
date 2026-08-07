package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;
import com.fr.plugin.report.debug.core.registry.PendingFetchRegistry;
import com.fr.plugin.report.debug.core.probe.DatasetExecutionDetector;
import com.fr.plugin.report.debug.core.probe.FetchLoggerHistoryProbe;
import com.fr.plugin.report.debug.core.fetch.FetchExecutionHook;
import com.fr.plugin.report.debug.core.config.DebugAssistantConfigStore;

import com.fr.esd.cache.runtime.ExecutedTableDataInfo;
import com.fr.esd.cache.runtime.ExecutedTableDataInfoManager;
import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.util.Collection;
import java.util.Map;

/**
 * 快照调试诊断：帮助确认 FR 会话侧暴露了哪些执行痕迹（仅 diag=1 时返回）。
 */
final class SnapshotExecutionDiagnostics {

    private SnapshotExecutionDiagnostics() {
    }

    public static JSONObject build(String sessionId, TemplateSessionIDInfo session) {
        JSONObject diag = JSONObject.create();
        diag.put("registryEntries", DatasetSqlExecutionRegistry.entryCount(sessionId));
        int cachedCount = 0;
        JSONArray cachedKeys = JSONArray.create();
        Map<String, ?> cached = DatasetExecutionDetector.readCachedDataModelsMap(session);
        if (cached != null) {
            cachedCount = cached.size();
            for (String key : cached.keySet()) {
                if (cachedKeys.size() >= 12) {
                    break;
                }
                cachedKeys.add(key);
            }
        }
        diag.put("cachedModelCount", cachedCount);
        diag.put("cachedModelKeys", cachedKeys);

        Map<String, Integer> sizeMap = session == null ? null : session.getTableDataSizeMap();
        diag.put("sizeMapCount", sizeMap == null ? 0 : sizeMap.size());
        JSONArray sizeKeys = JSONArray.create();
        if (sizeMap != null) {
            for (Map.Entry<String, Integer> entry : sizeMap.entrySet()) {
                if (sizeKeys.size() >= 12) {
                    break;
                }
                sizeKeys.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        diag.put("sizeMapKeys", sizeKeys);

        int esdCount = 0;
        JSONArray esdSamples = JSONArray.create();
        try {
            Collection<ExecutedTableDataInfo> list = ExecutedTableDataInfoManager.getInstance().getList();
            if (list != null) {
                esdCount = list.size();
                for (ExecutedTableDataInfo info : list) {
                    if (esdSamples.size() >= 6 || info == null) {
                        continue;
                    }
                    JSONObject row = JSONObject.create();
                    row.put("dsName", safe(info.getDsName()));
                    row.put("path", safe(info.getPath()));
                    row.put("sqlTime", info.getSqlTime());
                    row.put("row", info.getRow());
                    row.put("sqlDigest", safe(info.getSqlDigest()));
                    esdSamples.add(row);
                }
            }
        } catch (Throwable ignored) {
        }
        diag.put("esdCount", esdCount);
        diag.put("esdSamples", esdSamples);

        diag.put("fetchInforCount", FetchLoggerHistoryProbe.countInformEntries(session));
        diag.put("loggerPropertyCount", FetchLoggerHistoryProbe.countLoggerProperties(session));
        diag.put("activeFetchHookInstalled", FetchExecutionHook.isInstalled());
        diag.put("activeFetchHookError", FetchExecutionHook.getInstallError());
        diag.put("pendingFetchCount", PendingFetchRegistry.pendingCount());
        diag.put("sessionCachedProcessorLikelyUsed", DatasetSqlExecutionRegistry.entryCount(sessionId) > 0);
        return diag;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
