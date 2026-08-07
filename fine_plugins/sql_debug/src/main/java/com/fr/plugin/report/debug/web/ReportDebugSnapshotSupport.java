package com.fr.plugin.report.debug.web;

import com.fr.json.JSONObject;
import com.fr.plugin.report.debug.core.config.DebugAssistantAudit;
import com.fr.plugin.report.debug.core.config.DebugAssistantRateLimiter;
import com.fr.plugin.report.debug.core.config.DebugPermissionHelper;
import com.fr.plugin.report.debug.core.snapshot.ReportDebugSnapshotBuilder;
import com.fr.stable.StringUtils;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 快照 access / snapshot 共用逻辑，供 ReportServer WebService 与决策 REST 预览接口复用。
 */
public final class ReportDebugSnapshotSupport {

    private ReportDebugSnapshotSupport() {
    }

    public static void writeAccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!ensureFeatureEnabled(response)) {
            return;
        }
        boolean allowed = DebugPermissionHelper.allowSnapshot(request);
        JsonResponses.writeOk(response, JSONObject.create().put("allowed", allowed));
    }

    public static void writeSnapshot(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!ensureFeatureEnabled(response)) {
            return;
        }
        String actor = DebugPermissionHelper.resolveActorUsername(request);
        if (!DebugPermissionHelper.allowSnapshot(request)) {
            DebugAssistantAudit.logDenied(actor, "snapshot forbidden");
            JsonResponses.writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden",
                    "\u5f53\u524d\u7528\u6237\u672a\u5728\u6388\u6743\u5217\u8868\u4e2d\uff0c\u65e0\u6cd5\u4f7f\u7528\u6570\u636e\u96c6\u8c03\u8bd5\u52a9\u624b");
            return;
        }

        String sessionId = WebUtils.getHTTPRequestParameter(request, "sessionID");
        if (StringUtils.isBlank(sessionId)) {
            JsonResponses.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "bad_request",
                    "\u7f3a\u5c11 sessionID\uff0c\u8bf7\u5728\u62a5\u8868\u9884\u89c8\u9875\u6253\u5f00\u540e\u518d\u8bd5");
            return;
        }

        if (!DebugAssistantRateLimiter.getInstance().tryAcquireSnapshot(sessionId,
                ReportDebugRequestHelper.clientIp(request))) {
            DebugAssistantAudit.logDenied(actor, "snapshot rate limited");
            JsonResponses.writeError(response, 429, "rate_limited",
                    "\u5feb\u7167\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
            return;
        }

        String pageTitle = WebUtils.getHTTPRequestParameter(request, "pageTitle");
        boolean refresh = "1".equals(WebUtils.getHTTPRequestParameter(request, "refresh"))
                || "true".equalsIgnoreCase(WebUtils.getHTTPRequestParameter(request, "refresh"));
        boolean diag = "1".equals(WebUtils.getHTTPRequestParameter(request, "diag"))
                || "true".equalsIgnoreCase(WebUtils.getHTTPRequestParameter(request, "diag"));

        try {
            JSONObject snapshot = ReportDebugSnapshotBuilder.build(sessionId, pageTitle, refresh, diag);
            String reportPath = snapshot == null ? "-" : String.valueOf(snapshot.get("relativePath"));
            JsonResponses.writeOk(response, snapshot);
            DebugAssistantAudit.logSnapshot(actor, sessionId, reportPath, true);
        } catch (Exception ex) {
            DebugAssistantAudit.logSnapshot(actor, sessionId, "-", false);
            JsonResponses.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "snapshot_failed",
                    "snapshot failed");
        }
    }

    private static boolean ensureFeatureEnabled(HttpServletResponse response) throws Exception {
        if (!DebugPermissionHelper.isFeatureEnabled()) {
            JsonResponses.writeError(response, HttpServletResponse.SC_FORBIDDEN, "disabled",
                    "\u63d2\u4ef6\u5df2\u7981\u7528");
            return false;
        }
        return true;
    }
}
