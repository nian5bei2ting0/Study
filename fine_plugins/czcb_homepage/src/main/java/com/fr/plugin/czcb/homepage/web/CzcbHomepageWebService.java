package com.fr.plugin.czcb.homepage.web;

import com.fr.plugin.czcb.homepage.core.CzcbHomepageAdminAuth;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageConstants;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageMenuConfigStore;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageResourceDeployer;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.StringUtils;
import com.fr.stable.fun.impl.NoSessionIDService;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * info / menu_get 公开只读；menu_save 需决策平台管理员；deploy 禁止匿名。
 */
@FunctionRecorder
public class CzcbHomepageWebService extends NoSessionIDService {

    static {
        try {
            CzcbHomepageResourceDeployer.ensureDeployed(false);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String actionOP() {
        return CzcbHomepageConstants.WEBSERVICE_OP;
    }

    @Override
    @ExecuteFunctionRecord
    public void process(HttpServletRequest request, HttpServletResponse response, String frameworkOp) throws Exception {
        String cmd = WebUtils.getHTTPRequestParameter(request, "cmd");
        if (StringUtils.isBlank(cmd)) {
            cmd = "info";
        }
        Map<String, Object> data = new HashMap<String, Object>();
        if ("deploy".equalsIgnoreCase(cmd)) {
            data.put("status", "error");
            data.put("errorMsg", "deploy requires authenticated decision API");
            data.put("hint", "POST /decision/czcb/homepage/config/deploy");
        } else if ("menu_save".equalsIgnoreCase(cmd)) {
            handleMenuSave(request, data);
        } else if ("menu_get".equalsIgnoreCase(cmd) || "menu".equalsIgnoreCase(cmd)) {
            CzcbHomepageResourceDeployer.ensureDeployed(false);
            CzcbHomepageMenuConfigStore store = CzcbHomepageMenuConfigStore.getInstance();
            data.put("status", "success");
            data.put("menuConfigJson", store.getMenuConfigJson());
            data.put("configVersion", store.getConfigVersion());
            data.put("pluginVersion", CzcbHomepageConstants.PLUGIN_VERSION);
        } else {
            CzcbHomepageResourceDeployer.ensureDeployed(false);
            CzcbHomepageResourceDeployer.DeployResult status = CzcbHomepageResourceDeployer.getStatus();
            data.put("status", "success");
            data.put("pluginId", CzcbHomepageConstants.PLUGIN_ID);
            data.put("pluginVersion", CzcbHomepageConstants.PLUGIN_VERSION);
            data.put("homepageUrl", CzcbHomepageConstants.HOMEPAGE_WEB_PATH);
            data.put("configUrl", CzcbHomepageConstants.HOMEPAGE_CONFIG_PATH);
            data.put("deployed", status.isSuccess());
            data.put("deployVersion", status.getDeployedVersion());
            data.put("configVersion", CzcbHomepageMenuConfigStore.getInstance().getConfigVersion());
        }
        writeJson(response, data);
    }

    private static void handleMenuSave(HttpServletRequest request, Map<String, Object> data) {
        String user = CzcbHomepageAdminAuth.resolveUsername(request);
        if (StringUtils.isBlank(user)) {
            data.put("status", "error");
            data.put("errorMsg", "login required");
            data.put("errorCode", "unauthorized");
            return;
        }
        if (!CzcbHomepageAdminAuth.isCurrentUserAdmin(request)) {
            data.put("status", "error");
            data.put("errorMsg", "admin required");
            data.put("errorCode", "forbidden");
            return;
        }
        try {
            String json = resolveMenuConfigJson(request);
            if (StringUtils.isBlank(json)) {
                json = "[]";
            }
            CzcbHomepageMenuConfigStore store = CzcbHomepageMenuConfigStore.getInstance();
            store.saveMenuConfigJson(json);
            data.put("status", "success");
            data.put("configVersion", store.getConfigVersion());
            data.put("menuConfigJson", store.getMenuConfigJson());
            data.put("operator", user);
        } catch (Exception ex) {
            data.put("status", "error");
            data.put("errorMsg", ex.getMessage() == null ? "save failed" : ex.getMessage());
        }
    }

    private static String resolveMenuConfigJson(HttpServletRequest request) {
        String fromParam = WebUtils.getHTTPRequestParameter(request, "menuConfigJson");
        if (StringUtils.isNotBlank(fromParam)) {
            return fromParam.trim();
        }
        String fromMenu = WebUtils.getHTTPRequestParameter(request, "menuConfig");
        if (StringUtils.isNotBlank(fromMenu)) {
            return fromMenu.trim();
        }
        String body = readBody(request);
        if (StringUtils.isBlank(body)) {
            return "";
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            return trimmed;
        }
        // 简易提取 "menuConfigJson":"..." 或 "menuConfigJson":[...]
        String key = "\"menuConfigJson\"";
        int idx = trimmed.indexOf(key);
        if (idx < 0) {
            key = "\"menuConfig\"";
            idx = trimmed.indexOf(key);
        }
        if (idx < 0) {
            return "";
        }
        int colon = trimmed.indexOf(':', idx + key.length());
        if (colon < 0) {
            return "";
        }
        int i = colon + 1;
        while (i < trimmed.length() && Character.isWhitespace(trimmed.charAt(i))) {
            i++;
        }
        if (i >= trimmed.length()) {
            return "";
        }
        if (trimmed.charAt(i) == '"') {
            return unquoteJsonString(trimmed, i);
        }
        if (trimmed.charAt(i) == '[') {
            return extractJsonArray(trimmed, i);
        }
        return "";
    }

    private static String unquoteJsonString(String src, int quoteIdx) {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = quoteIdx + 1; i < src.length(); i++) {
            char c = src.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String extractJsonArray(String src, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < src.length(); i++) {
            char c = src.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return src.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    private static String readBody(HttpServletRequest request) {
        InputStream in = null;
        try {
            in = request.getInputStream();
            if (in == null) {
                return "";
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void writeJson(HttpServletResponse response, Map<String, Object> data) throws Exception {
        CzcbHomepageJsonResponses.writeMap(response, data);
    }
}
