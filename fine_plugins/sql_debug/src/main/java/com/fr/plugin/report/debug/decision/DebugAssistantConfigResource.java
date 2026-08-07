package com.fr.plugin.report.debug.decision;

import com.fr.decision.webservice.annotation.VisitRefer;
import com.fr.decision.webservice.v10.login.LoginService;
import com.fr.plugin.report.debug.core.config.DebugAssistantAudit;
import com.fr.plugin.report.debug.core.config.DebugAssistantConfigApiHelper;
import com.fr.plugin.report.debug.core.config.DebugAssistantConfigStore;
import com.fr.plugin.report.debug.core.snapshot.DebugAssistantSnapshotRuntime;
import com.fr.plugin.report.debug.core.config.DebugPermissionHelper;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.StringUtils;
import com.fr.third.springframework.stereotype.Controller;
import com.fr.third.springframework.web.bind.annotation.RequestBody;
import com.fr.third.springframework.web.bind.annotation.RequestMapping;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.fr.third.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/report/debug/assistant/config")
@VisitRefer(required = true)
@FunctionRecorder
public class DebugAssistantConfigResource {

    @ResponseBody
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ExecuteFunctionRecord
    public Map<String, Object> getConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!DebugPermissionHelper.allowConfigApi(request)) {
            return error("forbidden");
        }
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", "success");
        data.put("enabled", store.isEnabled());
        data.put("allowReportPreview", store.isAllowReportPreview());
        data.put("maskSqlInResponse", store.isMaskSqlInResponse());
        data.put("loginAuthOpen", store.isLoginAuthOpen());
        data.put("displayAbsolutePath", store.isDisplayAbsolutePath());
        List<String> clientUsers = DebugAssistantConfigApiHelper.authorizedUserValuesToClient(
                store.getAuthorizedUsers());
        List<String> displayUsers = DebugAssistantConfigApiHelper.authorizedUserDisplayValuesToClient(
                store.getAuthorizedUsers());
        data.put("authorizedUsernames", clientUsers);
        data.put("authorizedUsers", displayUsers);
        data.put("authorizedUsersText", store.getAuthorizedUsers());
        return data;
    }

    @ResponseBody
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ExecuteFunctionRecord
    public Map<String, Object> saveConfig(HttpServletRequest request,
                                          HttpServletResponse response,
                                          @RequestBody(required = false) Map<String, Object> body) throws Exception {
        if (!DebugPermissionHelper.allowConfigApi(request)) {
            return error("forbidden");
        }
        Map<String, Object> payload = DebugAssistantRequestBodies.resolve(request, body);
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        if (payload.containsKey("enabled")) {
            store.setEnabled(Boolean.TRUE.equals(payload.get("enabled")));
        }
        if (payload.containsKey("allowReportPreview")) {
            store.setAllowReportPreview(Boolean.TRUE.equals(payload.get("allowReportPreview")));
        }
        if (payload.containsKey("maskSqlInResponse")) {
            store.setMaskSqlInResponse(Boolean.TRUE.equals(payload.get("maskSqlInResponse")));
        }
        if (payload.containsKey("loginAuthOpen")) {
            store.setLoginAuthOpen(Boolean.TRUE.equals(payload.get("loginAuthOpen")));
        }
        if (payload.containsKey("displayAbsolutePath")) {
            store.setDisplayAbsolutePath(Boolean.TRUE.equals(payload.get("displayAbsolutePath")));
        }
        String usersFromPayload = DebugAssistantConfigApiHelper.authorizedUsersFromSavePayload(payload);
        if (usersFromPayload != null) {
            store.setAuthorizedUsers(usersFromPayload);
        }
        store.save();
        DebugAssistantSnapshotRuntime.getInstance().clearAll();
        String username = safeUsername(request);
        DebugAssistantAudit.logConfigChange(username, "save");
        Map<String, Object> ok = new HashMap<String, Object>();
        ok.put("status", "success");
        List<String> savedUsers = DebugAssistantConfigApiHelper.authorizedUserValuesToClient(
                store.getAuthorizedUsers());
        List<String> savedDisplayUsers = DebugAssistantConfigApiHelper.authorizedUserDisplayValuesToClient(
                store.getAuthorizedUsers());
        ok.put("authorizedUsernames", savedUsers);
        ok.put("authorizedUsers", savedDisplayUsers);
        ok.put("authorizedUsersText", store.getAuthorizedUsers());
        return ok;
    }

    private static Map<String, Object> error(String msg) {
        Map<String, Object> err = new HashMap<String, Object>();
        err.put("status", "error");
        err.put("errorMsg", msg);
        return err;
    }

    private static String safeUsername(HttpServletRequest request) {
        try {
            String user = LoginService.getInstance().getCurrentUserNameFromRequest(request);
            return StringUtils.isBlank(user) ? "-" : user;
        } catch (Throwable ex) {
            return "-";
        }
    }
}
