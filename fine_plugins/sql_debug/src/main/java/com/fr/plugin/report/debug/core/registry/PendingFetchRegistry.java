package com.fr.plugin.report.debug.core.registry;
import com.fr.plugin.report.debug.core.snapshot.ReportDebugExecutedSqlResolver;

import com.fr.plugin.report.debug.core.util.SqlFingerprintUtil;

import com.fr.base.TableData;
import com.fr.data.TableDataSource;
import com.fr.data.impl.DBTableData;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 取数时未能解析 sessionId 时暂存记录，构建快照时按 SQL 指纹匹配到数据集并写入正式登记。
 */
public final class PendingFetchRegistry {

    private static final CopyOnWriteArrayList<PendingRecord> PENDING = new CopyOnWriteArrayList<PendingRecord>();

    private PendingFetchRegistry() {
    }

    public static void add(String datasetName, String sql, Long sqlTimeMs) {
        if (StringUtils.isBlank(sql) && (sqlTimeMs == null || sqlTimeMs <= 0)) {
            return;
        }
        PendingRecord record = new PendingRecord();
        record.datasetName = datasetName;
        record.sql = sql;
        record.sqlTimeMs = sqlTimeMs;
        record.createdAt = System.currentTimeMillis();
        PENDING.add(record);
        if (PENDING.size() > 200) {
            PENDING.remove(0);
        }
    }

    public static int pendingCount() {
        return PENDING.size();
    }

    public static void flushToSession(String sessionId, TemplateSessionIDInfo session) {
        if (StringUtils.isBlank(sessionId) || session == null || PENDING.isEmpty()) {
            return;
        }
        TableDataSource source = session.getTableDataSource();
        if (source == null) {
            return;
        }
        List<PendingRecord> matched = new ArrayList<PendingRecord>();
        Iterator<String> names = source.getTableDataNameIterator();
        while (names != null && names.hasNext()) {
            String datasetName = names.next();
            TableData tableData = source.getTableData(datasetName);
            if (!(tableData instanceof DBTableData)) {
                continue;
            }
            String template = ((DBTableData) tableData).getQuery();
            String resolved = resolvePreviewSql((DBTableData) tableData, session, template);
            PendingRecord hit = findMatch(datasetName, resolved, template);
            if (hit != null) {
                DatasetSqlExecutionRegistry.record(sessionId, datasetName, hit.sql, hit.sqlTimeMs);
                matched.add(hit);
            }
        }
        PENDING.removeAll(matched);
    }

    private static String resolvePreviewSql(DBTableData db, TemplateSessionIDInfo session, String template) {
        try {
            return ReportDebugExecutedSqlResolver.resolve(db, "x", session, template, false, null);
        } catch (Throwable ignored) {
            return template;
        }
    }

    private static PendingRecord findMatch(String datasetName, String resolved, String template) {
        for (PendingRecord pending : PENDING) {
            if (pending == null) {
                continue;
            }
            if (StringUtils.isNotBlank(pending.datasetName)
                    && pending.datasetName.equals(datasetName)) {
                return pending;
            }
            if (sqlMatches(pending.sql, resolved) || sqlMatches(pending.sql, template)) {
                PendingRecord copy = new PendingRecord();
                copy.datasetName = datasetName;
                copy.sql = pending.sql;
                copy.sqlTimeMs = pending.sqlTimeMs;
                return copy;
            }
        }
        return null;
    }

    private static boolean sqlMatches(String left, String right) {
        if (StringUtils.isBlank(left) || StringUtils.isBlank(right)) {
            return false;
        }
        String fingerprint = SqlFingerprintUtil.extractFingerprint(left);
        if (StringUtils.isBlank(fingerprint)) {
            return normalize(left).equals(normalize(right));
        }
        return SqlFingerprintUtil.sqlMatchesFingerprint(right, fingerprint)
                || SqlFingerprintUtil.sqlMatchesFingerprint(left, SqlFingerprintUtil.extractFingerprint(right));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static final class PendingRecord {
        String datasetName;
        String sql;
        Long sqlTimeMs;
        long createdAt;
    }
}
