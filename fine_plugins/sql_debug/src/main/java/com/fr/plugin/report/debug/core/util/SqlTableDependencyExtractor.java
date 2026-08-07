package com.fr.plugin.report.debug.core.util;

import com.fr.stable.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 从 SQL 文本中提取依赖表名（去重、保序）。
 * <p>
 * 递归解析各层子查询中的 {@code FROM}/{@code JOIN}，支持 {@code schema.table} 小写命名，
 * 过滤别名.字段、函数名等误识别。
 */
public final class SqlTableDependencyExtractor {

    private static final Pattern PARAM_PLACEHOLDER = Pattern.compile("\\$\\{[^}]*}|\\$\\[[^\\]]*]");
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern TABLE_IDENTIFIER = Pattern.compile(
            "^[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?$");

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS", "OUTER",
            "ON", "AND", "OR", "AS", "BY", "GROUP", "ORDER", "HAVING", "UNION", "ALL", "DISTINCT",
            "INSERT", "INTO", "UPDATE", "DELETE", "SET", "VALUES", "CASE", "WHEN", "THEN", "ELSE", "END",
            "SUM", "COUNT", "AVG", "MIN", "MAX", "NVL", "COALESCE", "DECODE", "CAST", "TRIM",
            "TO_DATE", "TO_CHAR", "TO_NUMBER", "SUBSTR", "INSTR", "LENGTH", "ROUND", "TRUNC",
            "DUAL", "LATERAL", "WITH", "OVER", "PARTITION", "NULL", "IS", "NOT", "IN", "EXISTS",
            "LIKE", "BETWEEN", "ASC", "DESC", "LIMIT", "OFFSET", "FETCH", "ROWNUM"
    ));

    private SqlTableDependencyExtractor() {
    }

    public static List<String> extractUniqueTables(String sql) {
        if (StringUtils.isBlank(sql)) {
            return new ArrayList<String>();
        }
        String cleaned = stripParameters(stripComments(sql));
        Map<String, String> unique = new LinkedHashMap<String, String>();

        collectAllFromJoin(cleaned, unique);
        collectDmlTargetTable(cleaned, unique);

        return new ArrayList<String>(unique.values());
    }

    /** 各 nesting 层级（含子查询）的 FROM / JOIN 均参与解析。 */
    private static void collectAllFromJoin(String sql, Map<String, String> unique) {
        int i = 0;
        while (i < sql.length()) {
            if (sql.charAt(i) == '(') {
                i = collectInsideParenthesis(sql, i, unique);
                continue;
            }
            if (matchKeyword(sql, i, "FROM")) {
                i = consumeFromSegment(sql, i + 4, unique);
                continue;
            }
            if (matchJoinQualifier(sql, i)) {
                i = consumeQualifiedJoin(sql, i, unique);
                continue;
            }
            if (matchKeyword(sql, i, "JOIN")) {
                i = consumeJoinTable(sql, i + 4, unique);
                continue;
            }
            i++;
        }
    }

    /** 递归解析括号内子查询，并跳过至右括号之后。 */
    private static int collectInsideParenthesis(String sql, int pos, Map<String, String> unique) {
        int close = indexOfMatchingClose(sql, pos);
        if (close > pos + 1) {
            collectAllFromJoin(sql.substring(pos + 1, close), unique);
        }
        return close > pos ? close + 1 : pos + 1;
    }

    private static boolean matchJoinQualifier(String sql, int pos) {
        return matchKeyword(sql, pos, "LEFT")
                || matchKeyword(sql, pos, "RIGHT")
                || matchKeyword(sql, pos, "INNER")
                || matchKeyword(sql, pos, "FULL")
                || matchKeyword(sql, pos, "CROSS");
    }

    private static int consumeQualifiedJoin(String sql, int pos, Map<String, String> unique) {
        pos = skipPastJoinQualifierKeyword(sql, pos);
        pos = skipWs(sql, pos);
        if (matchKeyword(sql, pos, "OUTER")) {
            pos = skipWs(sql, pos + 5);
        }
        if (matchKeyword(sql, pos, "JOIN")) {
            pos = skipWs(sql, pos + 4);
        }
        pos = skipWs(sql, pos);
        if (pos < sql.length() && sql.charAt(pos) == '(') {
            return collectInsideParenthesis(sql, pos, unique);
        }
        String table = readIdentifier(sql, pos);
        addIfValid(table, unique);
        return advancePastIdentifier(sql, pos);
    }

    private static int skipPastJoinQualifierKeyword(String sql, int pos) {
        if (matchKeyword(sql, pos, "LEFT")) {
            return pos + 4;
        }
        if (matchKeyword(sql, pos, "RIGHT")) {
            return pos + 5;
        }
        if (matchKeyword(sql, pos, "INNER")) {
            return pos + 5;
        }
        if (matchKeyword(sql, pos, "FULL")) {
            return pos + 4;
        }
        if (matchKeyword(sql, pos, "CROSS")) {
            return pos + 5;
        }
        return pos;
    }

    private static void collectDmlTargetTable(String sql, Map<String, String> unique) {
        String trimmed = sql.trim();
        if (matchKeyword(trimmed, 0, "UPDATE")) {
            addIfValid(readIdentifier(trimmed, skipWs(trimmed, 6)), unique);
            return;
        }
        if (matchKeyword(trimmed, 0, "INSERT")) {
            int intoIdx = indexOfKeyword(trimmed, "INTO");
            if (intoIdx >= 0) {
                addIfValid(readIdentifier(trimmed, skipWs(trimmed, intoIdx + 4)), unique);
            }
        }
    }

    private static int consumeFromSegment(String sql, int pos, Map<String, String> unique) {
        pos = skipWs(sql, pos);
        while (pos < sql.length()) {
            pos = skipWs(sql, pos);
            if (pos >= sql.length()) {
                break;
            }
            if (sql.charAt(pos) == '(') {
                pos = collectInsideParenthesis(sql, pos, unique);
                pos = skipOptionalAlias(sql, pos);
                pos = skipWs(sql, pos);
                if (pos < sql.length() && sql.charAt(pos) == ',') {
                    pos = skipWs(sql, pos + 1);
                    continue;
                }
                break;
            }
            if (isFromSegmentBoundary(sql, pos)) {
                break;
            }
            String table = readIdentifier(sql, pos);
            addIfValid(table, unique);
            pos = advancePastIdentifier(sql, pos);
            pos = skipOptionalAlias(sql, pos);
            pos = skipWs(sql, pos);
            if (pos < sql.length() && sql.charAt(pos) == ',') {
                pos = skipWs(sql, pos + 1);
                continue;
            }
            break;
        }
        return pos;
    }

    private static int consumeJoinTable(String sql, int pos, Map<String, String> unique) {
        pos = skipWs(sql, pos);
        if (pos < sql.length() && sql.charAt(pos) == '(') {
            return collectInsideParenthesis(sql, pos, unique);
        }
        String table = readIdentifier(sql, pos);
        addIfValid(table, unique);
        return advancePastIdentifier(sql, pos);
    }

    private static boolean isFromSegmentBoundary(String sql, int pos) {
        return matchKeyword(sql, pos, "WHERE")
                || matchKeyword(sql, pos, "GROUP")
                || matchKeyword(sql, pos, "ORDER")
                || matchKeyword(sql, pos, "HAVING")
                || matchKeyword(sql, pos, "UNION")
                || matchJoinQualifier(sql, pos)
                || matchKeyword(sql, pos, "JOIN")
                || matchKeyword(sql, pos, "LIMIT")
                || matchKeyword(sql, pos, "FETCH");
    }

    private static String readIdentifier(String sql, int pos) {
        pos = skipWs(sql, pos);
        if (pos >= sql.length()) {
            return null;
        }
        char c = sql.charAt(pos);
        if (c == '(' || c == ',' || c == ';') {
            return null;
        }
        if (c == '`' || c == '"' || c == '[') {
            return readQuotedIdentifier(sql, pos);
        }
        int end = pos;
        while (end < sql.length()) {
            char ch = sql.charAt(end);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$' || ch == '#') {
                end++;
                continue;
            }
            if (ch == '.' && end + 1 < sql.length()) {
                char next = sql.charAt(end + 1);
                if (Character.isLetterOrDigit(next) || next == '_' || next == '$' || next == '#') {
                    end++;
                    continue;
                }
            }
            break;
        }
        if (end <= pos) {
            return null;
        }
        return sql.substring(pos, end);
    }

    private static String readQuotedIdentifier(String sql, int pos) {
        char quote = sql.charAt(pos);
        if (quote == '[') {
            int close = sql.indexOf(']', pos + 1);
            return close > pos ? sql.substring(pos + 1, close).trim() : null;
        }
        int i = pos + 1;
        StringBuilder sb = new StringBuilder();
        while (i < sql.length()) {
            char ch = sql.charAt(i);
            if (ch == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                    sb.append(quote);
                    i += 2;
                    continue;
                }
                break;
            }
            sb.append(ch);
            i++;
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }

    private static int advancePastIdentifier(String sql, int pos) {
        pos = skipWs(sql, pos);
        if (pos >= sql.length()) {
            return pos;
        }
        char c = sql.charAt(pos);
        if (c == '`' || c == '"' || c == '[') {
            if (c == '[') {
                int close = sql.indexOf(']', pos + 1);
                return close > pos ? close + 1 : pos + 1;
            }
            int i = pos + 1;
            while (i < sql.length()) {
                if (sql.charAt(i) == c) {
                    return i + 1;
                }
                i++;
            }
            return i;
        }
        int end = pos;
        while (end < sql.length()) {
            char ch = sql.charAt(end);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$' || ch == '#') {
                end++;
                continue;
            }
            if (ch == '.' && end + 1 < sql.length()) {
                char next = sql.charAt(end + 1);
                if (Character.isLetterOrDigit(next) || next == '_' || next == '$' || next == '#') {
                    end++;
                    continue;
                }
            }
            break;
        }
        return end > pos ? end : pos + 1;
    }

    private static int skipOptionalAlias(String sql, int pos) {
        pos = skipWs(sql, pos);
        if (pos >= sql.length()) {
            return pos;
        }
        if (matchKeyword(sql, pos, "AS")) {
            return advancePastIdentifier(sql, skipWs(sql, pos + 2));
        }
        if (sql.charAt(pos) == '(') {
            return pos;
        }
        if (isFromSegmentBoundary(sql, pos)) {
            return pos;
        }
        char c = sql.charAt(pos);
        if (Character.isLetter(c) || c == '_' || c == '$' || c == '#') {
            String next = readIdentifier(sql, pos);
            if (next != null && !isValidTableName(next)) {
                return advancePastIdentifier(sql, pos);
            }
        }
        return pos;
    }

    private static int indexOfMatchingClose(String sql, int openPos) {
        if (openPos < 0 || openPos >= sql.length() || sql.charAt(openPos) != '(') {
            return -1;
        }
        int depth = 0;
        for (int i = openPos; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int skipParenthesis(String sql, int pos) {
        int close = indexOfMatchingClose(sql, pos);
        return close > pos ? close + 1 : pos + 1;
    }

    private static void addIfValid(String token, Map<String, String> unique) {
        String normalized = normalizeTableToken(token);
        if (!isValidTableName(normalized)) {
            return;
        }
        String key = normalized.toLowerCase(Locale.ROOT);
        if (!unique.containsKey(key)) {
            unique.put(key, key);
        }
    }

    static boolean isValidTableName(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        String value = name.trim();
        if (value.indexOf('(') >= 0 || value.indexOf(')') >= 0
                || value.indexOf('\'') >= 0 || value.indexOf('"') >= 0) {
            return false;
        }
        if (CHINESE.matcher(value).find()) {
            return false;
        }
        if (KEYWORDS.contains(value.toUpperCase(Locale.ROOT))) {
            return false;
        }
        if (!TABLE_IDENTIFIER.matcher(value).matches()) {
            return false;
        }
        int dot = value.indexOf('.');
        if (dot > 0) {
            String schema = value.substring(0, dot);
            String table = value.substring(dot + 1);
            if (StringUtils.isBlank(schema) || StringUtils.isBlank(table)) {
                return false;
            }
            if (KEYWORDS.contains(schema.toUpperCase(Locale.ROOT))
                    || KEYWORDS.contains(table.toUpperCase(Locale.ROOT))) {
                return false;
            }
            return !looksLikeAliasColumn(schema, table);
        }
        return value.length() >= 3 && value.indexOf('_') >= 0;
    }

    /**
     * 过滤 {@code 别名.字段}；保留 {@code dwp.p_sys_user} 等 schema.table。
     */
    private static boolean looksLikeAliasColumn(String schema, String table) {
        if (schema.length() <= 2) {
            return true;
        }
        if (table.indexOf('_') >= 0) {
            return false;
        }
        if (schema.length() <= 3 && table.length() <= 16) {
            return true;
        }
        return false;
    }

    private static String normalizeTableToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String value = token.trim().replaceAll("[;,]+$", "");
        if (value.startsWith("(") || value.startsWith("${") || value.startsWith("$[")) {
            return null;
        }
        return value;
    }

    private static boolean matchKeyword(String sql, int pos, String keyword) {
        if (pos < 0 || pos + keyword.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, pos, keyword, 0, keyword.length())) {
            return false;
        }
        if (pos > 0) {
            char before = sql.charAt(pos - 1);
            if (Character.isLetterOrDigit(before) || before == '_' || before == '$' || before == '#') {
                return false;
            }
        }
        int after = pos + keyword.length();
        if (after < sql.length()) {
            char next = sql.charAt(after);
            if (Character.isLetterOrDigit(next) || next == '_' || next == '$' || next == '#') {
                return false;
            }
        }
        return true;
    }

    private static int indexOfKeyword(String sql, String keyword) {
        int i = 0;
        while (i < sql.length()) {
            if (matchKeyword(sql, i, keyword)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static int skipWs(String sql, int pos) {
        while (pos < sql.length() && Character.isWhitespace(sql.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static String stripComments(String sql) {
        String noBlock = sql.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return noBlock.replaceAll("--[^\r\n]*", " ");
    }

    private static String stripParameters(String sql) {
        return PARAM_PLACEHOLDER.matcher(sql).replaceAll(" ");
    }
}
