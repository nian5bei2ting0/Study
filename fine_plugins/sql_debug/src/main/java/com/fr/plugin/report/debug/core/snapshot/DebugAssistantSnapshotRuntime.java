package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;
import com.fr.plugin.report.debug.core.config.DebugAssistantConfigStore;
import com.fr.plugin.report.debug.core.config.DebugAssistantTraceGate;

import com.fr.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 近期快照简易缓存，避免重复构建。
 */
public final class DebugAssistantSnapshotRuntime {

    private static final int MAX_ENTRIES = 32;
    private static final long TTL_MS = 60_000L;

    private static final DebugAssistantSnapshotRuntime INSTANCE = new DebugAssistantSnapshotRuntime();

    private final Map<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public static DebugAssistantSnapshotRuntime getInstance() {
        return INSTANCE;
    }

    public synchronized JSONObject get(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        String key = cacheKey(sessionId);
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.createdAt > TTL_MS) {
            cache.remove(key);
            return null;
        }
        return entry.payload;
    }

    public synchronized void put(String sessionId, JSONObject payload) {
        if (sessionId == null || payload == null) {
            return;
        }
        cache.put(cacheKey(sessionId), new CacheEntry(payload, System.currentTimeMillis()));
    }

    /** 仅丢弃快照 JSON 缓存，保留取数时写入的执行登记。 */
    public synchronized void invalidateSnapshotCache(String sessionId) {
        if (sessionId != null) {
            cache.remove(cacheKey(sessionId));
        }
    }

    /** 丢弃快照缓存并清空该 session 的执行登记（显式 invalidate 命令）。 */
    public synchronized void invalidate(String sessionId) {
        invalidateSnapshotCache(sessionId);
        if (sessionId != null) {
            DatasetSqlExecutionRegistry.clearSession(sessionId);
            DebugAssistantTraceGate.revokeSessionTrace(sessionId);
        }
    }

    private static String cacheKey(String sessionId) {
        return sessionId + "@" + DebugAssistantConfigStore.getInstance().getConfigVersion();
    }

    public synchronized void clearAll() {
        cache.clear();
    }

    private static final class CacheEntry {
        private final JSONObject payload;
        private final long createdAt;

        private CacheEntry(JSONObject payload, long createdAt) {
            this.payload = payload;
            this.createdAt = createdAt;
        }
    }
}
