package com.fr.plugin.report.debug.core.config;

import com.fr.plugin.report.debug.core.fetch.FetchExecutionHook;
import com.fr.plugin.report.debug.core.fetch.SessionIdResolver;
import com.fr.stable.StringUtils;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 取数链路旁路：仅在「快照构建等显式调试窗口」内登记 SQL/耗时，避免授权用户预览报表时全程追踪。
 */
public final class DebugAssistantTraceGate {

    private static final ConcurrentHashMap<String, Boolean> SESSION_TRACE_CACHE = new ConcurrentHashMap<String, Boolean>();
    private static final Set<String> GRANTED_SESSIONS = ConcurrentHashMap.newKeySet();
    private static volatile long cachedConfigVersion = -1L;

    private DebugAssistantTraceGate() {
    }

    public static boolean isFeatureActive() {
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        return store.isEnabled() && store.isAllowReportPreview();
    }

    public static boolean shouldTraceSession(String sessionId) {
        if (!isFeatureActive() || StringUtils.isBlank(sessionId)) {
            return false;
        }
        ensureCacheGeneration();
        Boolean cached = SESSION_TRACE_CACHE.get(sessionId);
        if (cached != null) {
            return cached;
        }
        boolean trace = GRANTED_SESSIONS.contains(sessionId.trim());
        SESSION_TRACE_CACHE.put(sessionId, trace);
        return trace;
    }

    public static boolean shouldTraceCurrentFetch() {
        if (!isFeatureActive()) {
            return false;
        }
        String sessionId = SessionIdResolver.resolveOnFetchThread();
        return shouldTraceSession(sessionId);
    }

    /**
     * 在快照构建等短窗口内临时开启 session 追踪，结束后立即撤销。
     */
    public static <T> T callWithSessionTrace(String sessionId, Callable<T> task) throws Exception {
        if (task == null) {
            return null;
        }
        if (!isFeatureActive() || StringUtils.isBlank(sessionId)) {
            return task.call();
        }
        String id = sessionId.trim();
        GRANTED_SESSIONS.add(id);
        SESSION_TRACE_CACHE.put(id, Boolean.TRUE);
        ensureHookInstalled();
        try {
            return task.call();
        } finally {
            GRANTED_SESSIONS.remove(id);
            SESSION_TRACE_CACHE.remove(id);
        }
    }

    /** @deprecated 请使用 {@link #callWithSessionTrace}，避免预览全程追踪 */
    @Deprecated
    public static void grantSessionTrace(String sessionId) {
        if (!isFeatureActive() || StringUtils.isBlank(sessionId)) {
            return;
        }
        String id = sessionId.trim();
        GRANTED_SESSIONS.add(id);
        SESSION_TRACE_CACHE.put(id, Boolean.TRUE);
        ensureHookInstalled();
    }

    public static void revokeSessionTrace(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return;
        }
        String id = sessionId.trim();
        GRANTED_SESSIONS.remove(id);
        SESSION_TRACE_CACHE.remove(id);
    }

    public static void clearAllSessionTraces() {
        GRANTED_SESSIONS.clear();
        SESSION_TRACE_CACHE.clear();
    }

    public static void ensureHookInstalled() {
        if (isFeatureActive()) {
            FetchExecutionHook.install();
        }
    }

    private static void ensureCacheGeneration() {
        long version = DebugAssistantConfigStore.getInstance().getConfigVersion();
        if (version == cachedConfigVersion) {
            return;
        }
        synchronized (DebugAssistantTraceGate.class) {
            if (version != cachedConfigVersion) {
                SESSION_TRACE_CACHE.clear();
                GRANTED_SESSIONS.clear();
                cachedConfigVersion = version;
            }
        }
    }
}
