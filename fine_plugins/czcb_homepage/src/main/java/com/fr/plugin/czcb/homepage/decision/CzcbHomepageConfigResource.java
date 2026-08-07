package com.fr.plugin.czcb.homepage.decision;

import com.fr.decision.webservice.annotation.VisitRefer;
import com.fr.log.FineLoggerFactory;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageAdminAuth;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageConstants;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageDirectoryRegistrar;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageMenuConfigStore;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageResourceDeployer;
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
import java.util.Map;

@Controller
@RequestMapping(value = "/czcb/homepage/config")
@VisitRefer(required = true)
@FunctionRecorder
public class CzcbHomepageConfigResource {

    @ResponseBody
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ExecuteFunctionRecord
    public Map<String, Object> getConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CzcbHomepageResourceDeployer.ensureDeployed(false);
        CzcbHomepageDirectoryRegistrar.ensureDirectoryLink();
        CzcbHomepageResourceDeployer.DeployResult deployStatus = CzcbHomepageResourceDeployer.getStatus();
        CzcbHomepageMenuConfigStore menuStore = CzcbHomepageMenuConfigStore.getInstance();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", "success");
        data.put("homepageUrl", CzcbHomepageConstants.HOMEPAGE_WEB_PATH);
        data.put("configUrl", CzcbHomepageConstants.HOMEPAGE_CONFIG_PATH);
        data.put("deployed", deployStatus.isSuccess());
        data.put("deployVersion", deployStatus.getDeployedVersion());
        data.put("pluginVersion", CzcbHomepageConstants.PLUGIN_VERSION);
        data.put("directoryLinkName", CzcbHomepageConstants.DIRECTORY_LINK_NAME);
        data.put("menuConfigJson", menuStore.getMenuConfigJson());
        data.put("configVersion", menuStore.getConfigVersion());
        data.put("operator", safeUsername(request));
        data.put("canEdit", CzcbHomepageAdminAuth.isCurrentUserAdmin(request));
        return data;
    }

    @ResponseBody
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ExecuteFunctionRecord
    public Map<String, Object> saveConfig(HttpServletRequest request,
                                          HttpServletResponse response,
                                          @RequestBody(required = false) Map<String, Object> body) throws Exception {
        Map<String, Object> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> payload = body == null ? new HashMap<String, Object>() : body;
        Object raw = payload.get("menuConfigJson");
        if (raw == null) {
            raw = payload.get("menuConfig");
        }
        String json;
        if (raw == null) {
            json = "[]";
        } else if (raw instanceof String) {
            json = (String) raw;
        } else {
            json = String.valueOf(raw);
        }
        CzcbHomepageMenuConfigStore store = CzcbHomepageMenuConfigStore.getInstance();
        store.saveMenuConfigJson(json);
        Map<String, Object> ok = new HashMap<String, Object>();
        ok.put("status", "success");
        ok.put("configVersion", store.getConfigVersion());
        ok.put("menuConfigJson", store.getMenuConfigJson());
        ok.put("operator", safeUsername(request));
        return ok;
    }

    @ResponseBody
    @RequestMapping(value = "/deploy", method = RequestMethod.POST)
    @ExecuteFunctionRecord
    public Map<String, Object> deploy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> denied = requireAdmin(request);
        if (denied != null) {
            return denied;
        }
        boolean force = "1".equals(request.getParameter("force"))
                || "true".equalsIgnoreCase(request.getParameter("force"));
        CzcbHomepageResourceDeployer.DeployResult result = CzcbHomepageResourceDeployer.ensureDeployed(force);
        if (result.isSuccess()) {
            CzcbHomepageDirectoryRegistrar.invalidateCache();
            CzcbHomepageDirectoryRegistrar.ensureDirectoryLink();
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", result.isSuccess() ? "success" : "error");
        data.put("deployed", result.isSuccess());
        data.put("fresh", result.isFreshlyDeployed());
        data.put("homepageUrl", CzcbHomepageConstants.HOMEPAGE_WEB_PATH);
        data.put("configUrl", CzcbHomepageConstants.HOMEPAGE_CONFIG_PATH);
        if (!result.isSuccess()) {
            data.put("errorMsg", result.getMessage());
        }
        return data;
    }

    private static Map<String, Object> requireAdmin(HttpServletRequest request) {
        String user = CzcbHomepageAdminAuth.resolveUsername(request);
        if (StringUtils.isBlank(user)) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("status", "error");
            err.put("errorMsg", "login required");
            err.put("errorCode", "unauthorized");
            return err;
        }
        if (!CzcbHomepageAdminAuth.isCurrentUserAdmin(request)) {
            FineLoggerFactory.getLogger().warn(
                    "[czcb-homepage] config write denied, user={} not admin", user);
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("status", "error");
            err.put("errorMsg", "admin required");
            err.put("errorCode", "forbidden");
            return err;
        }
        return null;
    }

    private static String safeUsername(HttpServletRequest request) {
        String user = CzcbHomepageAdminAuth.resolveUsername(request);
        return StringUtils.isBlank(user) ? "-" : user;
    }
}
