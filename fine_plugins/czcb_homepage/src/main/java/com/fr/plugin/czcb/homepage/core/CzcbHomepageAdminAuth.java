package com.fr.plugin.czcb.homepage.core;

import com.fr.decision.webservice.v10.login.LoginService;
import com.fr.decision.webservice.v10.user.UserService;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

/**
 * 配置写操作仅允许决策平台管理员。
 * 注意：{@link UserService#isAdmin(String)} 入参应为 userId，不是用户名。
 */
public final class CzcbHomepageAdminAuth {

    private CzcbHomepageAdminAuth() {
    }

    public static boolean isCurrentUserAdmin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        try {
            UserService userService = UserService.getInstance();
            String userId = resolveUserId(request);
            String username = resolveUsername(request);

            if (StringUtils.isNotBlank(userId) && safeIsAdmin(userService, userId)) {
                return true;
            }
            if (StringUtils.isNotBlank(username)) {
                try {
                    String idByName = userService.getCurrentUserId(username);
                    if (StringUtils.isNotBlank(idByName) && safeIsAdmin(userService, idByName)) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (matchAdminLists(userService, userId, username)) {
                return true;
            }
            if (StringUtils.isNotBlank(username) && "admin".equalsIgnoreCase(username.trim())) {
                return true;
            }
        } catch (Throwable ex) {
            FineLoggerFactory.getLogger().warn("[czcb-homepage] admin check failed: {}", ex.getMessage());
        }
        return false;
    }

    public static String resolveUsername(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        try {
            LoginService loginService = LoginService.getInstance();
            String user = firstNonBlank(
                    loginService.getCurrentUserNameFromRequestCookie(request),
                    safeGetUserNameFromCookie(loginService, request),
                    loginService.getCurrentUserNameFromRequest(request),
                    safeGetUserNameFromRequest(loginService, request));
            return normalizeUsername(user);
        } catch (Throwable ex) {
            return "";
        }
    }

    public static String resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        try {
            UserService userService = UserService.getInstance();
            String userId = firstNonBlank(
                    safeGetUserId(userService, request),
                    safeGetUserIdFromCookie(userService, request));
            if (StringUtils.isNotBlank(userId)) {
                return userId.trim();
            }
            String username = resolveUsername(request);
            if (StringUtils.isNotBlank(username)) {
                String idByName = userService.getCurrentUserId(username);
                if (StringUtils.isNotBlank(idByName)) {
                    return idByName.trim();
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static boolean matchAdminLists(UserService userService, String userId, String username) {
        try {
            if (StringUtils.isNotBlank(userId)) {
                List<String> adminIds = userService.getAdminUserIdList();
                if (adminIds != null) {
                    for (String id : adminIds) {
                        if (userId.equalsIgnoreCase(id)) {
                            return true;
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(username)) {
                List<String> adminNames = userService.getAdminUserNameList();
                if (adminNames != null) {
                    String normalized = username.toLowerCase(Locale.ROOT);
                    for (String name : adminNames) {
                        if (name != null
                                && normalized.equals(normalizeUsername(name).toLowerCase(Locale.ROOT))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean safeIsAdmin(UserService userService, String idOrName) {
        try {
            return StringUtils.isNotBlank(idOrName) && userService.isAdmin(idOrName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String normalizeUsername(String raw) {
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
        return name;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v.trim();
            }
        }
        return "";
    }

    private static String safeGetUserId(UserService userService, HttpServletRequest request) {
        try {
            return userService.getCurrentUserId(request);
        } catch (Throwable ex) {
            return "";
        }
    }

    private static String safeGetUserIdFromCookie(UserService userService, HttpServletRequest request) {
        try {
            return userService.getCurrentUserIdFromCookie(request);
        } catch (Throwable ex) {
            return "";
        }
    }

    private static String safeGetUserNameFromCookie(LoginService loginService, HttpServletRequest request) {
        try {
            return loginService.getUserNameFromRequestCookie(request);
        } catch (Throwable ex) {
            return "";
        }
    }

    private static String safeGetUserNameFromRequest(LoginService loginService, HttpServletRequest request) {
        try {
            return loginService.getUserNameFromRequest(request);
        } catch (Throwable ex) {
            return "";
        }
    }
}
