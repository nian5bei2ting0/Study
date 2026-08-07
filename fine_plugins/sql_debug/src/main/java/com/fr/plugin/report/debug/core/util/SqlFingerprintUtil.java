package com.fr.plugin.report.debug.core.util;

import com.fr.io.utils.MD5Calculator;
import com.fr.stable.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 与 ESD {@code ExecutedTableDataInfo.sqlDigest} 一致的 SQL 指纹/摘要工具。
 */
public final class SqlFingerprintUtil {

    private static final Pattern PROJECT_CODE = Pattern.compile(
            "project_code\\s*=\\s*'([^']+)'", Pattern.CASE_INSENSITIVE);

    private SqlFingerprintUtil() {
    }

    public static String extractFingerprint(String sql) {
        if (StringUtils.isBlank(sql)) {
            return null;
        }
        Matcher matcher = PROJECT_CODE.matcher(sql);
        if (matcher.find()) {
            return "project_code = '" + matcher.group(1) + "'";
        }
        String normalized = normalize(sql);
        return normalized.length() > 160 ? normalized.substring(0, 160) : normalized;
    }

    public static String sqlDigest(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        try {
            return MD5Calculator.calculateMD5(sql.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean sqlMatchesFingerprint(String sql, String fingerprint) {
        if (StringUtils.isBlank(sql) || StringUtils.isBlank(fingerprint)) {
            return false;
        }
        return normalize(sql).contains(normalize(fingerprint));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
