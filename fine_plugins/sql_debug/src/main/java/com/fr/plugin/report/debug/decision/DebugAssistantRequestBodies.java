package com.fr.plugin.report.debug.decision;

import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.stable.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 解析决策配置保存请求体（兼容 {@code {data:{...}}} 包装及不同 JSON 库反序列化类型）。
 */
final class DebugAssistantRequestBodies {

    private DebugAssistantRequestBodies() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> resolve(HttpServletRequest request, Map<String, Object> springBody) {
        Map<String, Object> body = springBody;
        if (body == null || body.isEmpty()) {
            body = readJsonBody(request);
        }
        if (body == null) {
            return new HashMap<String, Object>();
        }
        return unwrap(body);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map) {
            return normalizePayload((Map<String, Object>) data);
        }
        if (data instanceof JSONObject) {
            return normalizePayload(jsonObjectToMap((JSONObject) data));
        }
        return normalizePayload(body);
    }

    private static Map<String, Object> normalizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return payload == null ? new HashMap<String, Object>() : payload;
        }
        Object users = payload.get("authorizedUsers");
        if (users instanceof JSONArray) {
            payload.put("authorizedUsers", ((JSONArray) users).getList());
        } else if (users instanceof JSONObject) {
            payload.put("authorizedUsers", orderedValuesFromNumericObject((JSONObject) users));
        } else if (users instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) users;
            if (mapContainsNumericKeys(map)) {
                payload.put("authorizedUsers", orderedValuesFromNumericMap(map));
            }
        }
        return payload;
    }

    private static boolean mapContainsNumericKeys(Map<?, ?> map) {
        for (Object key : map.keySet()) {
            if (key != null && String.valueOf(key).trim().matches("\\d+")) {
                return true;
            }
        }
        return false;
    }

    private static List<Object> orderedValuesFromNumericMap(Map<?, ?> map) {
        List<Integer> indexes = new ArrayList<Integer>();
        for (Object key : map.keySet()) {
            if (key == null) {
                continue;
            }
            String s = String.valueOf(key).trim();
            if (s.matches("\\d+")) {
                indexes.add(Integer.parseInt(s));
            }
        }
        Collections.sort(indexes);
        List<Object> values = new ArrayList<Object>();
        for (Integer index : indexes) {
            Object val = map.get(String.valueOf(index));
            if (val == null) {
                val = map.get(index);
            }
            values.add(val);
        }
        return values;
    }

    private static List<Object> orderedValuesFromNumericObject(JSONObject json) {
        Map<String, Object> map = jsonObjectToMap(json);
        return orderedValuesFromNumericMap(map);
    }

    private static Map<String, Object> readJsonBody(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        InputStream in = null;
        try {
            in = request.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            String raw = new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
            if (StringUtils.isBlank(raw)) {
                return null;
            }
            JSONObject json = new JSONObject(raw);
            return jsonObjectToMap(json);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (json == null) {
            return map;
        }
        try {
            Iterator<Map.Entry<String, Object>> it = json.iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                if (entry.getKey() != null) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Throwable ignored) {
            try {
                for (String key : json.fieldNames()) {
                    map.put(key, json.getValue(key));
                }
            } catch (Throwable ignored2) {
            }
        }
        return map;
    }
}
