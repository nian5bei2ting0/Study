package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.config.DebugAssistantConfigStore;
import com.fr.plugin.report.debug.core.config.DebugAssistantTraceGate;
import com.fr.plugin.report.debug.core.registry.PendingFetchRegistry;

import com.fr.base.TableData;
import com.fr.data.TableDataSource;
import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.stable.StableUtils;
import com.fr.stable.StringUtils;
import com.fr.web.core.ReportSession;
import com.fr.web.core.ReportSessionIDInfor;
import com.fr.web.core.SessionPoolManager;
import com.fr.web.core.TemplateSessionIDInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * 从当前报表会话构建调试快照（与报表调试助手 UI 字段对齐）。
 */
public final class ReportDebugSnapshotBuilder {

    private ReportDebugSnapshotBuilder() {
    }

    public static JSONObject build(String sessionId, String pageTitle, boolean refresh) throws Exception {
        return build(sessionId, pageTitle, refresh, false);
    }

    public static JSONObject build(String sessionId, String pageTitle, boolean refresh, boolean includeDiagnostics)
            throws Exception {
        if (StringUtils.isBlank(sessionId)) {
            throw new IllegalArgumentException("sessionID is required");
        }
        if (!refresh) {
            JSONObject cached = DebugAssistantSnapshotRuntime.getInstance().get(sessionId);
            if (cached != null) {
                return cached;
            }
        } else {
            DebugAssistantSnapshotRuntime.getInstance().invalidateSnapshotCache(sessionId);
        }
        return DebugAssistantTraceGate.callWithSessionTrace(sessionId, new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                return buildSnapshotBody(sessionId, pageTitle, includeDiagnostics);
            }
        });
    }

    private static JSONObject buildSnapshotBody(String sessionId, String pageTitle, boolean includeDiagnostics)
            throws Exception {
        TemplateSessionIDInfo session = SessionPoolManager.getSessionIDInfor(sessionId, TemplateSessionIDInfo.class);
        if (session == null) {
            throw new IllegalStateException("session not found");
        }

        boolean showAbsolutePath = DebugAssistantConfigStore.getInstance().isDisplayAbsolutePath();
        JSONObject root = JSONObject.create();
        root.put("sessionId", sessionId);
        root.put("reportName", resolveReportName(session, pageTitle));
        root.put("relativePath", safe(session.getRelativePath()));
        root.put("displayAbsolutePath", showAbsolutePath);
        root.put("absolutePath", showAbsolutePath ? resolveAbsolutePath(session) : "");

        PendingFetchRegistry.flushToSession(sessionId, session);

        SnapshotBuildContext buildContext = new SnapshotBuildContext(session);
        JSONArray datasets = JSONArray.create();
        TableDataSource source = session.getTableDataSource();
        int index = 1;
        if (source != null) {
            Iterator<String> names = source.getTableDataNameIterator();
            while (names != null && names.hasNext()) {
                String name = names.next();
                TableData tableData = source.getTableData(name);
                datasets.add(ReportDebugDatasetInspector.inspect(sessionId, name, tableData, session, index++,
                        buildContext));
            }
        }
        root.put("datasets", datasets);
        if (includeDiagnostics) {
            root.put("diag", SnapshotExecutionDiagnostics.build(sessionId, session));
        }

        DebugAssistantSnapshotRuntime.getInstance().put(sessionId, root);
        return root;
    }

    private static String resolveReportName(TemplateSessionIDInfo session, String pageTitle) {
        if (StringUtils.isNotBlank(pageTitle)) {
            return pageTitle.trim();
        }
        if (session instanceof ReportSession) {
            ReportSession rs = (ReportSession) session;
            String title = rs.getWebTitle();
            if (StringUtils.isNotBlank(title)) {
                return title.trim();
            }
            try {
                if (rs.getReportCount() > 0) {
                    String name = rs.getReportName(0);
                    if (StringUtils.isNotBlank(name)) {
                        return name.trim();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        String relative = session.getRelativePath();
        if (StringUtils.isNotBlank(relative)) {
            int slash = Math.max(relative.lastIndexOf('/'), relative.lastIndexOf('\\'));
            return slash >= 0 ? relative.substring(slash + 1) : relative;
        }
        return "";
    }

    private static String resolveAbsolutePath(TemplateSessionIDInfo session) {
        String relative = session.getRelativePath();
        String bookPath = readBookPath(session);
        if (StringUtils.isNotBlank(bookPath)) {
            File f = new File(bookPath);
            if (f.isAbsolute()) {
                return bookPath;
            }
        }
        if (StringUtils.isBlank(relative)) {
            return safe(bookPath);
        }
        if (relative.contains(":") || relative.startsWith("/") || relative.startsWith("\\")) {
            return relative;
        }
        return StableUtils.pathJoin(StableUtils.getInstallHome(), "WEB-INF", "reportlets", relative);
    }

    private static String readBookPath(TemplateSessionIDInfo session) {
        try {
            Field field = TemplateSessionIDInfo.class.getDeclaredField("bookPath");
            field.setAccessible(true);
            Object value = field.get(session);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            if (session instanceof ReportSessionIDInfor) {
                return safe(session.getRelativePath());
            }
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
