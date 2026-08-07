package com.fr.plugin.report.debug.web;

import com.fr.stable.StringUtils;

import javax.servlet.http.HttpServletRequest;

final class ReportDebugRequestHelper {

    private ReportDebugRequestHelper() {
    }

    static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }
        try {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.isNotBlank(forwarded)) {
                int comma = forwarded.indexOf(',');
                return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (StringUtils.isNotBlank(realIp)) {
                return realIp.trim();
            }
            String remote = request.getRemoteAddr();
            return StringUtils.isBlank(remote) ? "-" : remote;
        } catch (Throwable ignored) {
            return "-";
        }
    }
}
