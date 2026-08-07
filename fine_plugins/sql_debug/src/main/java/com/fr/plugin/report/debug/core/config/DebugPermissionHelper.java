package com.fr.plugin.report.debug.core.config;

import com.fr.decision.webservice.v10.login.LoginService;
import com.fr.stable.StringUtils;
import com.fr.web.core.SessionPoolManager;
import com.fr.web.core.TemplateSessionIDInfo;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 快照与配置接口的权限校验。
 */
public final class DebugPermissionHelper {

    private static final String[] REPORT_SESSION_USER_KEYS = new String[]{
            "FR_USERNAME", "fine_username", "username", "userName", "$fine_username", "USER_NAME"
    };

    private DebugPermissionHelper() {
    }

    public static boolean isFeatureEnabled() {
        return DebugAssistantConfigStore.getInstance().isEnabled();
    }

    public static boolean allowSnapshot(HttpServletRequest request) {
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        if (!store.isEnabled()) {
            return false;
        }
        if (!store.isAllowReportPreview()) {
            return false;
        }
        return allowSnapshotAccess(request);
    }

    public static boolean allowConfigApi(HttpServletRequest request) {
        return true;
    }

    /** 快照/审计用：解析当前请求对应用户（登录名）。 */
    public static String resolveActorUsername(HttpServletRequest request) {
        String user = resolveUsername(request);
        return StringUtils.isBlank(user) ? "-" : user;
    }

    private static boolean allowSnapshotAccess(HttpServletRequest request) {
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        String sessionId = readReportSessionId(request);
        String username = resolveUsername(request);

        if (StringUtils.isNotBlank(sessionId) && allowSessionTrace(sessionId, username)) {
            return true;
        }

        if (hasConfiguredWhitelist(store)) {
            return isUsernameWhitelisted(username, store);
        }
        if (store.isLoginAuthOpen()) {
            return StringUtils.isNotBlank(username);
        }
        return hasValidReportSession(request);
    }

    /**
     * 取数线程旁路：仅依据 session 与配置判断，不访问 HttpServletRequest。
     */
    public static boolean allowSessionTrace(String sessionId) {
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        if (!store.isEnabled() || !store.isAllowReportPreview() || StringUtils.isBlank(sessionId)) {
            return false;
        }
        try {
            TemplateSessionIDInfo session =
                    SessionPoolManager.getSessionIDInfor(sessionId, TemplateSessionIDInfo.class);
            if (session == null) {
                return false;
            }
            return allowSessionTrace(sessionId, resolveUsernameFromTemplateSession(session));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean allowSessionTrace(String sessionId, String username) {
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        if (!store.isEnabled() || !store.isAllowReportPreview() || StringUtils.isBlank(sessionId)) {
            return false;
        }
        if (hasConfiguredWhitelist(store)) {
            if (StringUtils.isBlank(username)) {
                return false;
            }
            return isUsernameWhitelisted(username, store);
        }
        if (store.isLoginAuthOpen()) {
            return StringUtils.isNotBlank(username);
        }
        return SessionPoolManager.getSessionIDInfor(sessionId, TemplateSessionIDInfo.class) != null;
    }

    private static boolean hasConfiguredWhitelist(DebugAssistantConfigStore store) {
        Set<String> whitelist = DebugAssistantConfigApiHelper.parseAuthorizedUsernames(store.getAuthorizedUsers());
        return whitelist != null && !whitelist.isEmpty();
    }

    private static boolean isUsernameWhitelisted(String username, DebugAssistantConfigStore store) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        Set<String> whitelist = DebugAssistantConfigApiHelper.parseAuthorizedUsernames(store.getAuthorizedUsers());
        String normalized = normalizeUsername(username);
        for (String allowed : whitelist) {
            if (allowed.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String readReportSessionId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String sessionId = WebUtils.getHTTPRequestParameter(request, "sessionID");
        if (StringUtils.isBlank(sessionId)) {
            sessionId = WebUtils.getHTTPRequestParameter(request, "sessionId");
        }
        return sessionId;
    }

    private static boolean hasValidReportSession(HttpServletRequest request) {
        String sessionId = readReportSessionId(request);
        if (StringUtils.isBlank(sessionId)) {
            return false;
        }
        try {
            TemplateSessionIDInfo session =
                    SessionPoolManager.getSessionIDInfor(sessionId, TemplateSessionIDInfo.class);
            return session != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 归一化为登录名。支持 {@code 艾可(eoco)} → {@code eoco}，{@code admin(admin)} → {@code admin}。
     */
    public static String normalizeUsername(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String name = raw.trim();
        int left = name.lastIndexOf('(');
        int right = name.lastIndexOf(')');
        if (left > 0 && right > left) {
            String inside = name.substring(left + 1, right).trim();
            if (StringUtils.isNotBlank(inside)) {
                return inside;
            }
        }
        int paren = name.indexOf('(');
        if (paren > 0) {
            return name.substring(0, paren).trim();
        }
        return name;
    }

    private static String resolveUsername(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String user = resolveFromLoginService(request);
        if (StringUtils.isNotBlank(user)) {
            return user;
        }
        user = resolveFromHttpSessionAndCookies(request);
        if (StringUtils.isNotBlank(user)) {
            return user;
        }
        user = resolveUsernameFromReportSession(request);
        if (StringUtils.isNotBlank(user)) {
            return user;
        }
        return resolveUsernameFromTrustedRequestParam(request);
    }

    private static String resolveFromLoginService(HttpServletRequest request) {
        try {
            LoginService loginService = LoginService.getInstance();
            String user = firstNonBlank(
                    loginService.getCurrentUserNameFromRequest(request),
                    loginService.getCurrentUserNameFromRequestCookie(request),
                    loginService.getUserNameFromRequest(request),
                    loginService.getUserNameFromRequestCookie(request));
            if (StringUtils.isNotBlank(user)) {
                return normalizeUsername(user);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String resolveFromHttpSessionAndCookies(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String[] keys = new String[]{"username", "fine_username", "FR_USERNAME", "userName"};
                for (String key : keys) {
                    Object attr = session.getAttribute(key);
                    if (attr != null && StringUtils.isNotBlank(String.valueOf(attr))) {
                        return normalizeUsername(String.valueOf(attr));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie == null || StringUtils.isBlank(cookie.getName())) {
                        continue;
                    }
                    String name = cookie.getName().toLowerCase(Locale.ROOT);
                    if ("fine_username".equals(name) || "username".equals(name) || "fr_username".equals(name)) {
                        String value = cookie.getValue();
                        if (StringUtils.isNotBlank(value)) {
                            return normalizeUsername(value);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * 预览页由前端传入 {@code fine_username}（来自页面 Cookie/FR 对象），须与 LoginService/Cookie 之一一致才采纳。
     */
    private static String resolveUsernameFromTrustedRequestParam(HttpServletRequest request) {
        String param = WebUtils.getHTTPRequestParameter(request, "fine_username");
        if (StringUtils.isBlank(param)) {
            param = WebUtils.getHTTPRequestParameter(request, "username");
        }
        if (StringUtils.isBlank(param)) {
            return null;
        }
        String paramUser = normalizeUsername(param);

        String serverUser = resolveFromLoginService(request);
        if (StringUtils.isBlank(serverUser)) {
            serverUser = resolveFromHttpSessionAndCookies(request);
        }
        if (StringUtils.isNotBlank(serverUser)) {
            if (paramUser.equalsIgnoreCase(serverUser)) {
                return paramUser;
            }
            return serverUser;
        }
        return null;
    }

    private static String resolveUsernameFromReportSession(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String sessionId = WebUtils.getHTTPRequestParameter(request, "sessionID");
        if (StringUtils.isBlank(sessionId)) {
            sessionId = WebUtils.getHTTPRequestParameter(request, "sessionId");
        }
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        try {
            TemplateSessionIDInfo session =
                    SessionPoolManager.getSessionIDInfor(sessionId, TemplateSessionIDInfo.class);
            if (session == null) {
                return null;
            }
            return resolveUsernameFromTemplateSession(session);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String resolveUsernameFromTemplateSession(TemplateSessionIDInfo session) {
        if (session == null) {
            return null;
        }
        for (String key : REPORT_SESSION_USER_KEYS) {
            try {
                Object value = session.getParameterValue(key);
                if (value == null) {
                    value = session.getAttribute(key);
                }
                if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                    return normalizeUsername(String.valueOf(value));
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Map<String, Object> all = session.getAllPara();
            if (all != null) {
                for (Map.Entry<String, Object> entry : all.entrySet()) {
                    if (entry == null || entry.getKey() == null) {
                        continue;
                    }
                    String key = entry.getKey().toLowerCase(Locale.ROOT);
                    if (key.contains("username") || key.contains("fine_username")) {
                        Object value = entry.getValue();
                        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                            return normalizeUsername(String.valueOf(value));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return resolveUsernameFromSessionObject(session);
    }

    private static String resolveUsernameFromSessionObject(Object session) {
        if (session == null) {
            return null;
        }
        String[] methods = new String[]{
                "getWebUsername", "getUsername", "getUserName", "getUser", "getDisplayName"
        };
        for (String methodName : methods) {
            try {
                Method method = session.getClass().getMethod(methodName);
                Object value = method.invoke(session);
                if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                    return normalizeUsername(String.valueOf(value));
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
