package com.fr.plugin.report.debug.core.fetch;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;

import com.fr.measure.metric.DBMetric;
import com.fr.stable.StringUtils;
import com.fr.web.session.SessionLocalManager;

import java.util.List;

/**
 * 从 {@link SessionLocalManager} 当前线程 SQL 度量中读取执行记录（不依赖抽数缓存）。
 */
final class SessionLocalSqlProbe {

    private SessionLocalSqlProbe() {
    }

    public static void captureToRegistry(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return;
        }
        try {
            List<DBMetric> metrics = SessionLocalManager.getSqlMetric();
            if (metrics == null || metrics.isEmpty()) {
                return;
            }
            FetchExecutionContext.State ctx = FetchExecutionContext.current();
            for (DBMetric metric : metrics) {
                if (metric == null) {
                    continue;
                }
                String dsName = metric.getDsName();
                if (StringUtils.isBlank(dsName) && ctx != null) {
                    dsName = ctx.datasetName;
                }
                String sql = metric.getSql();
                long sqlTime = metric.getSqlTime();
                Long time = sqlTime > 0 ? sqlTime : null;
                if (StringUtils.isNotBlank(dsName)) {
                    DatasetSqlExecutionRegistry.record(sessionId, dsName, sql, time);
                } else if (StringUtils.isNotBlank(sql)) {
                    DatasetSqlExecutionRegistry.record(sessionId, sql, sql, time);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
