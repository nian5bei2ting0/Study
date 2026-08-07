package com.fr.plugin.report.debug.core.snapshot;
import com.fr.plugin.report.debug.core.config.DebugAssistantConfigStore;
import com.fr.plugin.report.debug.core.util.SqlTableDependencyExtractor;

import com.fr.base.TableData;
import com.fr.data.impl.DBTableData;
import com.fr.json.JSONArray;
import com.fr.json.JSONObject;
import com.fr.script.Calculator;
import com.fr.stable.ParameterProvider;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.util.List;
import java.util.Map;

/**
 * 从会话中抽取单个数据集的调试信息。
 */
public final class ReportDebugDatasetInspector {

    private ReportDebugDatasetInspector() {
    }

    public static JSONObject inspect(String datasetName, TableData tableData, TemplateSessionIDInfo session, int index) {
        return inspect(null, datasetName, tableData, session, index, null);
    }

    public static JSONObject inspect(String sessionId, String datasetName, TableData tableData,
                            TemplateSessionIDInfo session, int index, SnapshotBuildContext buildContext) {
        JSONObject item = JSONObject.create();
        item.put("index", index);
        item.put("name", datasetName);

        boolean databaseQuery = tableData instanceof DBTableData;
        item.put("typeCategory", databaseQuery ? "database" : "other");
        item.put("typeLabel", databaseQuery ? "\u6570\u636e\u5e93\u67e5\u8be2" : "\u975e\u6570\u636e\u5e93\u67e5\u8be2");
        item.put("showSql", databaseQuery);

        DBTableData dbForParams = databaseQuery && tableData instanceof DBTableData ? (DBTableData) tableData : null;
        String queryTemplate = dbForParams != null ? safe(dbForParams.getQuery()) : null;

        String previewSql = null;
        boolean sqlParameterResolved = false;
        if (dbForParams != null) {
            previewSql = ReportDebugExecutedSqlResolver.resolve(dbForParams, datasetName, session,
                    queryTemplate, false, buildContext);
            sqlParameterResolved = !containsPlaceholder(previewSql);
        }

        DatasetSqlMetricsResolver.Metrics metrics = DatasetSqlMetricsResolver.resolve(
                sessionId, datasetName, session, dbForParams, buildContext, sqlParameterResolved, previewSql);
        boolean executed = metrics.executed;
        item.put("executed", executed);
        item.put("runStatusLabel", metrics.runStatusLabel);
        if (metrics.sqlTimeMs != null && metrics.sqlTimeMs > 0) {
            item.put("sqlTimeMs", metrics.sqlTimeMs);
        }
        boolean maskSensitive = DebugAssistantConfigStore.getInstance().isMaskSqlInResponse();
        item.put("parametersMasked", maskSensitive);
        item.put("parameters", buildParameters(tableData, dbForParams, queryTemplate, session, executed,
                maskSensitive, buildContext));

        if (dbForParams != null) {
            DBTableData db = dbForParams;
            String originalSql = safe(db.getQuery());
            String displaySql = ReportDebugExecutedSqlResolver.resolve(db, datasetName, session, originalSql,
                    executed, buildContext);
            item.put("sqlMasked", maskSensitive);
            if (maskSensitive) {
                item.put("querySql", maskIfNeeded(displaySql));
                item.put("originalSql", maskIfNeeded(originalSql));
            } else {
                item.put("querySql", displaySql);
                item.put("originalSql", originalSql);
            }
            item.put("sqlResolved", !containsPlaceholder(displaySql));
            item.put("dependentTables", toTableArray(
                    SqlTableDependencyExtractor.extractUniqueTables(displaySql)));
            item.put("showDependencies", true);
        }

        return item;
    }

    private static JSONArray toTableArray(List<String> tables) {
        JSONArray arr = JSONArray.create();
        if (tables == null) {
            return arr;
        }
        for (String table : tables) {
            if (StringUtils.isNotBlank(table)) {
                arr.add(table.trim());
            }
        }
        return arr;
    }

    private static JSONArray buildParameters(TableData tableData, DBTableData dbTableData, String queryTemplate,
                                             TemplateSessionIDInfo session, boolean executed,
                                             boolean maskSensitive, SnapshotBuildContext buildContext) {
        JSONArray arr = JSONArray.create();
        if (tableData == null) {
            return arr;
        }
        try {
            Calculator calculator = buildContext != null
                    ? buildContext.getOrCreateParameterCalculator(session)
                    : createParameterCalculator(session);
            ParameterProvider[] providers = tableData.getParameters(calculator);
            if ((providers == null || providers.length == 0) && dbTableData != null) {
                providers = dbTableData.getParameters();
            }
            ParameterProvider[] processedProviders = providers;
            if (providers != null && providers.length > 0) {
                processedProviders = Calculator.processParameters(calculator, providers);
            }
            if (processedProviders != null) {
                for (ParameterProvider provider : processedProviders) {
                    if (provider == null) {
                        continue;
                    }
                    String name = provider.getName();
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    JSONObject p = JSONObject.create();
                    p.put("name", name);
                    String original = ReportDebugParameterResolver.resolveOriginalValue(provider, calculator);
                    String display = ReportDebugParameterResolver.resolveDisplayValue(
                            name, provider, processedProviders, queryTemplate, dbTableData,
                            session, calculator, executed);
                    if (StringUtils.isBlank(display) && StringUtils.isNotBlank(original)) {
                        display = original;
                    }
                    if (StringUtils.isBlank(original) && StringUtils.isNotBlank(display)) {
                        original = display;
                    }
                    if (maskSensitive) {
                        original = maskIfNeeded(original);
                        display = maskIfNeeded(display);
                    }
                    p.put("original", original);
                    p.put("value", display);
                    arr.add(p);
                }
            }
        } catch (Throwable ignored) {
        }
        return arr;
    }

    public static Calculator createParameterCalculator(TemplateSessionIDInfo session) {
        Map<String, Object> executeMap = null;
        try {
            executeMap = session.getParameterMap4ExecuteAll();
            if (executeMap == null || executeMap.isEmpty()) {
                executeMap = session.getParameterMap4Execute();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (executeMap != null && !executeMap.isEmpty()) {
                return session.createSessionCalculator(null, null, executeMap);
            }
        } catch (Throwable ignored) {
        }
        Calculator calculator = session.createSessionCalculator(null, null);
        try {
            Map<String, Object> all = session.getAllPara();
            if (all != null) {
                for (Map.Entry<String, Object> entry : all.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        calculator.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return calculator;
    }

    private static boolean containsPlaceholder(String sql) {
        if (sql == null) {
            return false;
        }
        return sql.indexOf("${") >= 0 || sql.indexOf("$[") >= 0;
    }

    private static String maskIfNeeded(String sql) {
        if (sql == null) {
            return "";
        }
        if (!DebugAssistantConfigStore.getInstance().isMaskSqlInResponse()) {
            return sql;
        }
        if (sql.length() <= 16) {
            return "****";
        }
        return sql.substring(0, 8) + " ... " + sql.substring(sql.length() - 8);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
