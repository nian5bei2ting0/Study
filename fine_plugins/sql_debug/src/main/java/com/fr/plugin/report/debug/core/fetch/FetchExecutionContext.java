package com.fr.plugin.report.debug.core.fetch;

import com.fr.stable.StringUtils;

/**
 * 单次取数线程上下文（sessionId、数据集名）。
 */
final class FetchExecutionContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<State>();

    private FetchExecutionContext() {
    }

    static void open() {
        State state = new State();
        state.sessionId = SessionIdResolver.resolveOnFetchThread();
        HOLDER.set(state);
    }

    static void setDatasetName(String datasetName) {
        State state = HOLDER.get();
        if (state != null && StringUtils.isNotBlank(datasetName)) {
            state.datasetName = datasetName.trim();
        }
    }

    static State current() {
        return HOLDER.get();
    }

    static void close() {
        HOLDER.remove();
    }

    static final class State {
        String sessionId;
        String datasetName;
    }
}
