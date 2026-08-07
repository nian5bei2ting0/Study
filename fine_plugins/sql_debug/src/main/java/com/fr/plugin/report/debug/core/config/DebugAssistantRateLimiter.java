package com.fr.plugin.report.debug.core.config;

import com.fr.stable.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 快照 API 简易限流（仅 {@code cmd=snapshot}），避免频繁全量构建。
 */
public final class DebugAssistantRateLimiter {

    private static final int MAX_SNAPSHOTS_PER_WINDOW = 24;
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 256;

    private static final DebugAssistantRateLimiter INSTANCE = new DebugAssistantRateLimiter();

    private final Map<String, List<Long>> windows = new LinkedHashMap<String, List<Long>>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Long>> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public static DebugAssistantRateLimiter getInstance() {
        return INSTANCE;
    }

    /**
     * @return true 允许继续；false 触发限流
     */
    public synchronized boolean tryAcquireSnapshot(String sessionId, String clientIp) {
        String key = buildKey(sessionId, clientIp);
        long now = System.currentTimeMillis();
        List<Long> hits = windows.get(key);
        if (hits == null) {
            hits = new ArrayList<Long>();
            windows.put(key, hits);
        }
        prune(hits, now);
        if (hits.size() >= MAX_SNAPSHOTS_PER_WINDOW) {
            return false;
        }
        hits.add(now);
        return true;
    }

    private static void prune(List<Long> hits, long now) {
        int i = 0;
        while (i < hits.size()) {
            if (now - hits.get(i) > WINDOW_MS) {
                hits.remove(i);
            } else {
                i++;
            }
        }
    }

    private static String buildKey(String sessionId, String clientIp) {
        String sid = StringUtils.isBlank(sessionId) ? "-" : sessionId.trim();
        String ip = StringUtils.isBlank(clientIp) ? "-" : clientIp.trim();
        return sid + "|" + ip;
    }
}
