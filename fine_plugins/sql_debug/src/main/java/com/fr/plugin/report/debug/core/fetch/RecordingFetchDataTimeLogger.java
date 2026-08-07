package com.fr.plugin.report.debug.core.fetch;

import com.fr.plugin.report.debug.core.config.DebugAssistantTraceGate;
import com.fr.log.FetchDataTimeLogger;

/**
 * 包装引擎 {@link FetchDataTimeLogger}，在每次取数结束时写入插件执行登记。
 */
public class RecordingFetchDataTimeLogger extends FetchDataTimeLogger {

    private boolean traceActive;

    @Override
    public void beforeFetchData() {
        super.beforeFetchData();
        traceActive = DebugAssistantTraceGate.shouldTraceCurrentFetch();
        if (traceActive) {
            FetchExecutionContext.open();
        }
    }

    @Override
    public void setDsName(String dsName) {
        super.setDsName(dsName);
        if (traceActive) {
            FetchExecutionContext.setDatasetName(dsName);
        }
    }

    @Override
    public void postFetchData(String position) {
        super.postFetchData(position);
        if (!traceActive) {
            return;
        }
        try {
            FetchExecutionRecorder.recordFromLogger(this);
        } finally {
            FetchExecutionContext.close();
            traceActive = false;
        }
    }
}
