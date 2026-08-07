package com.fr.plugin.report.debug.core.config;

import com.fr.log.FineLoggerFactory;

/**
 * 调试快照访问审计（仅写服务端日志，不返回前端）。
 */
public final class DebugAssistantAudit {

    private static final String LOG_PREFIX = "[report-debug-assistant]";

    private DebugAssistantAudit() {
    }

    public static void logSnapshot(String username, String sessionId, String reportPath, boolean success) {
        FineLoggerFactory.getLogger().info(
                LOG_PREFIX + " snapshot user={} session={} report={} success={}",
                safe(username),
                safe(sessionId),
                safe(reportPath),
                success
        );
    }

    public static void logConfigChange(String username, String action) {
        FineLoggerFactory.getLogger().info(
                LOG_PREFIX + " config user={} action={}",
                safe(username),
                safe(action)
        );
    }

    public static void logDenied(String username, String reason) {
        FineLoggerFactory.getLogger().warn(
                LOG_PREFIX + " denied user={} reason={}",
                safe(username),
                safe(reason)
        );
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }
}
