package com.fr.plugin.report.debug.web;

import com.fr.json.JSONObject;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletResponse;

/**
 * 统一 JSON 响应（避免向前端泄露内部异常细节）。
 */
public final class JsonResponses {

    private JsonResponses() {
    }

    public static void writeOk(HttpServletResponse response, JSONObject data) throws Exception {
        JSONObject body = JSONObject.create();
        body.put("success", true);
        body.put("data", data);
        writeJson(response, HttpServletResponse.SC_OK, body);
    }

    public static void writeError(HttpServletResponse response, int httpStatus, String code, String message)
            throws Exception {
        JSONObject body = JSONObject.create();
        body.put("success", false);
        body.put("errorCode", code);
        body.put("errorMsg", message);
        writeJson(response, httpStatus, body);
    }

    private static void writeJson(HttpServletResponse response, int httpStatus, JSONObject body) throws Exception {
        response.setStatus(httpStatus);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        WebUtils.printAsJSON(response, body);
    }
}
