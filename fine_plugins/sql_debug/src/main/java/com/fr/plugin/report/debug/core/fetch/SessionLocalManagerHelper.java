package com.fr.plugin.report.debug.core.fetch;

import com.fr.web.session.SessionLocalManager;

/**
 * 薄封装，避免 FetchExecutionRecorder 直接散落 SessionLocalManager 调用。
 */
final class SessionLocalManagerHelper {

    private SessionLocalManagerHelper() {
    }

    static String getSql() {
        return SessionLocalManager.getSql();
    }

    static long getSqlTime() {
        return SessionLocalManager.getSqlTime();
    }
}
