package com.fr.plugin.online.save.decision;

import com.fr.decision.authority.base.constant.AuthorityStaticItemId;
import com.fr.decision.authority.base.constant.type.authority.ViewAuthorityType;
import com.fr.decision.webservice.annotation.LoginStatusChecker;
import com.fr.decision.webservice.annotation.VisitRefer;
import com.fr.decision.webservice.v10.entry.EntryService;
import com.fr.decision.webservice.v10.login.TokenResource;
import com.fr.decision.webservice.v10.user.UserService;
import com.fr.log.FineLoggerFactory;
import com.fr.plugin.online.save.core.OnlineSaveConstants;
import com.fr.plugin.online.save.service.DirectorySaveService;
import com.fr.stable.StringUtils;
import com.fr.third.springframework.stereotype.Controller;
import com.fr.third.springframework.web.bind.annotation.RequestMapping;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.fr.third.springframework.web.bind.annotation.RequestParam;
import com.fr.third.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 目录管理「保存」等价接口 + entry 查询接口。
 * <ul>
 *   <li>POST {@code /online/save/directory/apply} — 刷新目录树缓存（仅 POST）</li>
 *   <li>GET  {@code /online/save/entry/lookup?displayName=xxx} — 按显示名查 entryId</li>
 *   <li>GET  {@code /online/save/directory/ping} — 探活</li>
 * </ul>
 */
@Controller
@RequestMapping(value = OnlineSaveConstants.API_PREFIX)
@LoginStatusChecker(required = true, tokenResource = TokenResource.COOKIE)
@VisitRefer(required = false)
public class OnlineSaveDirectoryResource {

    @ResponseBody
    @RequestMapping(value = "/directory/ping", method = RequestMethod.GET)
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "success");
        body.put("message", "online-save ok");
        return body;
    }

    /**
     * 模拟目录管理点击保存：刷新 EntryTreeCache / EntryTreeNodeCache。
     * 仅 POST，避免浏览器地址栏 GET 触发 login?origin 死循环。
     */
    @ResponseBody
    @RequestMapping(value = "/directory/apply", method = RequestMethod.POST)
    public Map<String, Object> applyDirectorySave(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        try {
            String userId = resolveUserId(request);
            if (StringUtils.isBlank(userId)) {
                FineLoggerFactory.getLogger().warn("[online-save] directory/apply denied: not logged in");
                body.put("status", "fail");
                body.put("errorCode", "NOT_LOGIN");
                body.put("errorMsg", "login required");
                return body;
            }
            if (!hasDirectoryManagePrivilege(userId)) {
                FineLoggerFactory.getLogger().warn(
                        "[online-save] directory/apply denied: no directory management privilege, userId={}", userId);
                body.put("status", "fail");
                body.put("errorCode", "NO_DIRECTORY_PRIVILEGE");
                body.put("errorMsg", "directory management privilege required");
                return body;
            }
            Map<String, Object> data = DirectorySaveService.getInstance().applyDirectorySave();
            body.put("status", "success");
            body.put("data", data);
            return body;
        } catch (Exception e) {
            FineLoggerFactory.getLogger().error("[online-save] directory/apply failed: " + e.getMessage(), e);
            body.put("status", "fail");
            body.put("errorCode", "INTERNAL_ERROR");
            body.put("errorMsg", e.getMessage());
            return body;
        }
    }

    /**
     * 按 displayName 查询 fine_authority_object，返回匹配的 entry 列表。
     * 用于自动化场景：挂模板前/后根据名称拿到 entryId。
     */
    @ResponseBody
    @RequestMapping(value = "/entry/lookup", method = RequestMethod.GET)
    public Map<String, Object> lookupEntry(@RequestParam("displayName") String displayName) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        try {
            if (StringUtils.isBlank(displayName)) {
                body.put("status", "fail");
                body.put("errorCode", "PARAM_INVALID");
                body.put("errorMsg", "displayName is required");
                return body;
            }
            List<Map<String, Object>> entries = DirectorySaveService.getInstance().lookupByDisplayName(displayName);
            body.put("status", "success");
            body.put("data", entries);
            body.put("count", entries.size());
            return body;
        } catch (Exception e) {
            FineLoggerFactory.getLogger().error("[online-save] entry/lookup failed: " + e.getMessage(), e);
            body.put("status", "fail");
            body.put("errorCode", "INTERNAL_ERROR");
            body.put("errorMsg", e.getMessage());
            return body;
        }
    }

    private static String resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        UserService userService = UserService.getInstance();
        try {
            String userId = userService.getCurrentUserId(request);
            if (StringUtils.isNotBlank(userId)) {
                return userId;
            }
        } catch (Throwable ignored) {
        }
        try {
            return userService.getCurrentUserIdFromCookie(request);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasDirectoryManagePrivilege(String userId) {
        try {
            UserService userService = UserService.getInstance();
            if (userService.isAdmin(userId)) {
                return true;
            }
            return EntryService.getInstance().checkAuthority(
                    userId,
                    AuthorityStaticItemId.DEC_MANAGEMENT_DIRECTORY_ID,
                    ViewAuthorityType.TYPE);
        } catch (Throwable t) {
            FineLoggerFactory.getLogger().warn(
                    "[online-save] directory privilege check failed: {}", t.getMessage());
            return false;
        }
    }
}
