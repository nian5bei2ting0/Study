package com.fr.plugin.report.debug.web;

import com.fr.json.JSONObject;
import com.fr.plugin.report.debug.core.config.DebugAssistantAudit;
import com.fr.plugin.report.debug.core.config.DebugPermissionHelper;
import com.fr.plugin.report.debug.core.snapshot.DebugAssistantSnapshotRuntime;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.StringUtils;
import com.fr.web.core.ActionNoSessionCMD;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 快照子命令：cmd=snapshot | invalidate | access
 */
@FunctionRecorder
public class ReportDebugSnapshotAction extends ActionNoSessionCMD {

    public static final String CMD_SNAPSHOT = "snapshot";
    public static final String CMD_INVALIDATE = "invalidate";
    public static final String CMD_ACCESS = "access";

    @Override
    public String getCMD() {
        return CMD_SNAPSHOT;
    }

    @Override
    @ExecuteFunctionRecord
    public void actionCMD(HttpServletRequest request, HttpServletResponse response, String cmd) throws Exception {
        if (!DebugPermissionHelper.isFeatureEnabled()) {
            JsonResponses.writeError(response, HttpServletResponse.SC_FORBIDDEN, "disabled",
                    "\u63d2\u4ef6\u5df2\u7981\u7528");
            return;
        }
        if (CMD_ACCESS.equalsIgnoreCase(cmd)) {
            ReportDebugSnapshotSupport.writeAccess(request, response);
            return;
        }

        String actor = DebugPermissionHelper.resolveActorUsername(request);

        String sessionId = WebUtils.getHTTPRequestParameter(request, "sessionID");
        if (CMD_INVALIDATE.equalsIgnoreCase(cmd)) {
            if (StringUtils.isBlank(sessionId)) {
                JsonResponses.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "bad_request",
                        "\u7f3a\u5c11 sessionID\uff0c\u8bf7\u5728\u62a5\u8868\u9884\u89c8\u9875\u6253\u5f00\u540e\u518d\u8bd5");
                return;
            }
            DebugAssistantSnapshotRuntime.getInstance().invalidate(sessionId);
            JsonResponses.writeOk(response, JSONObject.create().put("invalidated", true));
            DebugAssistantAudit.logSnapshot(actor, sessionId, "-", true);
            return;
        }

        if (!CMD_SNAPSHOT.equalsIgnoreCase(cmd)) {
            JsonResponses.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "unknown_cmd", "unknown cmd");
            return;
        }

        ReportDebugSnapshotSupport.writeSnapshot(request, response);
    }

    @Override
    public void actionCMD(HttpServletRequest request, HttpServletResponse response) throws Exception {
        actionCMD(request, response, CMD_SNAPSHOT);
    }
}
