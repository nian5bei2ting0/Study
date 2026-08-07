package com.fr.plugin.czcb.homepage.web;

import com.fr.json.JSONObject;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

final class CzcbHomepageJsonResponses {

    private CzcbHomepageJsonResponses() {
    }

    static void writeMap(HttpServletResponse response, Map<String, Object> map) throws Exception {
        JSONObject body = JSONObject.create();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                body.put(entry.getKey(), entry.getValue());
            }
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        WebUtils.printAsJSON(response, body);
    }
}
