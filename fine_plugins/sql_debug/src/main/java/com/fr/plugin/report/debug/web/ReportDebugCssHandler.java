package com.fr.plugin.report.debug.web;

import com.fr.plugin.report.debug.core.config.DebugAssistantConfigStore;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractCssFileHandler;

@FunctionRecorder
public class ReportDebugCssHandler extends AbstractCssFileHandler {

    @Override
    @ExecuteFunctionRecord
    public String[] pathsForFiles() {
        DebugAssistantConfigStore store = DebugAssistantConfigStore.getInstance();
        if (!store.isEnabled() || !store.isAllowReportPreview()) {
            return new String[0];
        }
        return new String[]{
                "/com/fr/plugin/report/debug/web/report_debug_assistant.css"
        };
    }
}
