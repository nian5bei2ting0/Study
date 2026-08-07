package com.fr.plugin.report.debug.core.config;

import com.fr.json.JSONArray;
import com.fr.stable.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 决策配置 API 与 {@code dec.case.platform.user} 组件的数据格式转换。
 * <p>
 * 平台用户选择器的 item.value 为 {@code 显示名(用户名)}（如 {@code 艾可(eoco)}），
 * 不能仅用用户名，否则回显为 {@code [object Object]}。
 */
public final class DebugAssistantConfigApiHelper {

    private static final String ENTRY_SEP = "\n";
    private static final String FIELD_SEP = "|";

    private DebugAssistantConfigApiHelper() {
    }

    /**
     * 供前端使用的授权用户名列表（仅登录名，不含「显示名(用户名)」）。
     * 显示文案由前端根据 {@link Dec.Utils#reqPlatformUsers} 结果拼装。
     */
    public static List<String> authorizedUserValuesToClient(String stored) {
        return new ArrayList<String>(parseAuthorizedUsernames(stored));
    }

    public static List<Map<String, Object>> authorizedUsersToClient(String stored) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (StringUtils.isBlank(stored)) {
            return result;
        }
        String trimmed = stored.trim();
        if (trimmed.contains(FIELD_SEP)) {
            String[] entries = trimmed.split("[\\r\\n;]+");
            for (String entry : entries) {
                if (StringUtils.isBlank(entry)) {
                    continue;
                }
                String[] fields = entry.split("\\|", 2);
                String username = fields[0].trim();
                if (StringUtils.isBlank(username) || isCorruptUsername(username)) {
                    continue;
                }
                String display = fields.length > 1 && StringUtils.isNotBlank(fields[1])
                        ? fields[1].trim()
                        : platformDisplay(username, username);
                result.add(toClientItem(username, display, null, null));
            }
            return result;
        }
        String[] parts = trimmed.split("[,;\\s]+");
        for (String part : parts) {
            if (StringUtils.isBlank(part) || isCorruptUsername(part)) {
                continue;
            }
            String username = DebugPermissionHelper.normalizeUsername(part.trim());
            result.add(toClientItem(username, platformDisplay(username, username), null, null));
        }
        return result;
    }

    /**
     * 从保存请求体解析授权用户（优先 {@code authorizedUsersText}，避免部分环境数组反序列化丢失）。
     */
    public static String authorizedUsersFromSavePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object text = payload.get("authorizedUsersText");
        if (text != null && StringUtils.isNotBlank(String.valueOf(text))) {
            return authorizedUsersFromClient(String.valueOf(text).trim());
        }
        if (payload.containsKey("authorizedUsers")) {
            return authorizedUsersFromClient(payload.get("authorizedUsers"));
        }
        return null;
    }

    public static String joinAuthorizedUsersText(List<String> users) {
        if (users == null || users.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String user : users) {
            if (StringUtils.isBlank(user)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(user.trim());
        }
        return sb.toString();
    }

    /**
     * 供决策平台用户选择器回显：返回「显示名(用户名)」列表；存储为 {@code 用户名|显示名(用户名)} 时保留中文显示名。
     */
    public static List<String> authorizedUserDisplayValuesToClient(String stored) {
        List<String> result = new ArrayList<String>();
        if (StringUtils.isBlank(stored)) {
            return result;
        }
        String[] entries = stored.trim().split("[\\r\\n;]+");
        for (String entry : entries) {
            if (StringUtils.isBlank(entry)) {
                continue;
            }
            String trimmed = entry.trim();
            if (trimmed.contains(FIELD_SEP)) {
                String[] fields = trimmed.split("\\|", 2);
                String username = fields[0].trim();
                if (isCorruptUsername(username)) {
                    continue;
                }
                String display = fields.length > 1 && StringUtils.isNotBlank(fields[1])
                        ? fields[1].trim()
                        : platformDisplay(username, username);
                result.add(display);
                continue;
            }
            if (isCorruptUsername(trimmed)) {
                continue;
            }
            if (trimmed.contains("(") && trimmed.contains(")")) {
                result.add(trimmed);
            } else {
                String username = DebugPermissionHelper.normalizeUsername(trimmed);
                result.add(platformDisplay(username, username));
            }
        }
        return result;
    }

    static String toStoredEntry(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String s = raw.trim();
        if (s.contains(FIELD_SEP)) {
            String[] fields = s.split("\\|", 2);
            String username = DebugPermissionHelper.normalizeUsername(fields[0].trim());
            if (isCorruptUsername(username)) {
                return "";
            }
            String display = fields.length > 1 && StringUtils.isNotBlank(fields[1])
                    ? fields[1].trim()
                    : platformDisplay(username, username);
            return username + FIELD_SEP + display;
        }
        if (isCorruptUsername(s) && !s.contains("(")) {
            return "";
        }
        String username = DebugPermissionHelper.normalizeUsername(s);
        if (isCorruptUsername(username)) {
            return "";
        }
        String display = s.contains("(") && s.contains(")")
                ? s
                : platformDisplay(username, username);
        return username + FIELD_SEP + display;
    }

    public static String authorizedUsersFromClient(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof String) {
            String single = ((String) raw).trim();
            if (StringUtils.isBlank(single)) {
                return "";
            }
            if (single.contains("\n") || single.contains("\r") || single.contains(";")) {
                return authorizedUsersFromClientEntries(splitStoredLines(single));
            }
            return toStoredEntry(single);
        }
        if (raw instanceof JSONArray) {
            return authorizedUsersFromClient(((JSONArray) raw).getList());
        }
        if (raw instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) raw;
            if (!map.isEmpty() && mapContainsNumericKeys(map)) {
                return authorizedUsersFromClientEntries(map.values());
            }
        }
        if (raw instanceof Collection) {
            return authorizedUsersFromClientEntries((Collection<?>) raw);
        }
        if (raw instanceof List) {
            return authorizedUsersFromClientEntries((List<?>) raw);
        }
        if (raw instanceof Object[]) {
            List<Object> list = new ArrayList<Object>();
            for (Object o : (Object[]) raw) {
                list.add(o);
            }
            return authorizedUsersFromClientEntries(list);
        }
        return "";
    }

    private static boolean mapContainsNumericKeys(Map<?, ?> map) {
        for (Object key : map.keySet()) {
            if (key == null) {
                continue;
            }
            String s = String.valueOf(key).trim();
            if (s.matches("\\d+")) {
                return true;
            }
        }
        return false;
    }

    private static String authorizedUsersFromClientEntries(Iterable<?> entries) {
        StringBuilder sb = new StringBuilder();
        for (Object entry : entries) {
            if (entry instanceof String) {
                String s = ((String) entry).trim();
                if (StringUtils.isBlank(s)) {
                    continue;
                }
                String line = toStoredEntry(s);
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(ENTRY_SEP);
                }
                sb.append(line);
                continue;
            }
            String username = extractUsername(entry);
            if (StringUtils.isBlank(username) || isCorruptUsername(username)) {
                continue;
            }
            String display = extractDisplayValue(entry, username);
            if (sb.length() > 0) {
                sb.append(ENTRY_SEP);
            }
            sb.append(username.trim()).append(FIELD_SEP).append(display);
        }
        return sb.toString();
    }

    private static List<String> splitStoredLines(String raw) {
        List<String> lines = new ArrayList<String>();
        if (StringUtils.isBlank(raw)) {
            return lines;
        }
        String[] parts = raw.split("[\\r\\n;]+");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                lines.add(part.trim());
            }
        }
        return lines;
    }

    public static Set<String> parseAuthorizedUsernames(String stored) {
        Set<String> names = new HashSet<String>();
        if (StringUtils.isBlank(stored)) {
            return names;
        }
        String trimmed = stored.trim();
        if (trimmed.contains(FIELD_SEP)) {
            String[] entries = trimmed.split("[\\r\\n;]+");
            for (String entry : entries) {
                if (StringUtils.isBlank(entry)) {
                    continue;
                }
                String[] fields = entry.split("\\|", 2);
                if (StringUtils.isNotBlank(fields[0]) && !isCorruptUsername(fields[0])) {
                    names.add(DebugPermissionHelper.normalizeUsername(fields[0].trim()));
                }
            }
            return names;
        }
        String[] parts = trimmed.split("[,;\\s]+");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part) && !isCorruptUsername(part)) {
                names.add(DebugPermissionHelper.normalizeUsername(part.trim()));
            }
        }
        return names;
    }

    private static Map<String, Object> toClientItem(String username, String display,
                                                    String realName, String id) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("username", username);
        item.put("value", display);
        item.put("text", display);
        item.put("title", display);
        item.put("selected", Boolean.FALSE);
        if (StringUtils.isNotBlank(realName)) {
            item.put("realName", realName);
        }
        if (StringUtils.isNotBlank(id)) {
            item.put("id", id);
        }
        return item;
    }

    private static String platformDisplay(String realNameOrUsername, String username) {
        String label = StringUtils.isBlank(realNameOrUsername) ? username : realNameOrUsername.trim();
        return label + "(" + username.trim() + ")";
    }

    private static String extractDisplayValue(Object entry, String username) {
        if (entry instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) entry;
            Object value = map.get("value");
            if (value != null) {
                String s = String.valueOf(value).trim();
                if (s.contains("(") && s.contains(")")) {
                    return s;
                }
            }
            Object text = map.get("text");
            if (text != null) {
                String s = String.valueOf(text).trim();
                if (s.contains("(") && s.contains(")")) {
                    return s;
                }
            }
            Object realName = map.get("realName");
            if (realName != null && StringUtils.isNotBlank(String.valueOf(realName))) {
                return platformDisplay(String.valueOf(realName), username);
            }
        }
        return platformDisplay(username, username);
    }

    private static boolean isCorruptUsername(String username) {
        if (username == null) {
            return true;
        }
        String u = username.trim();
        if (u.isEmpty() || u.toLowerCase().contains("[object")) {
            return true;
        }
        // 多选组件偶发回传数组下标（0、1），非真实登录名；纯数字工号如 05120 须保留
        return u.matches("\\d{1,2}");
    }

    private static String extractUsername(Object entry) {
        if (entry == null) {
            return null;
        }
        if (entry instanceof String) {
            return DebugPermissionHelper.normalizeUsername((String) entry);
        }
        if (entry instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) entry;
            Object username = map.get("username");
            if (username != null && StringUtils.isNotBlank(String.valueOf(username))) {
                return DebugPermissionHelper.normalizeUsername(String.valueOf(username));
            }
            Object value = map.get("value");
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return DebugPermissionHelper.normalizeUsername(String.valueOf(value));
            }
            Object text = map.get("text");
            if (text != null && StringUtils.isNotBlank(String.valueOf(text))) {
                return DebugPermissionHelper.normalizeUsername(String.valueOf(text));
            }
        }
        return DebugPermissionHelper.normalizeUsername(String.valueOf(entry));
    }
}
