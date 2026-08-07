package com.fr.plugin.report.debug.locale;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractLocaleFinder;

@FunctionRecorder
public class DebugAssistantLocaleFinder extends AbstractLocaleFinder {

    /** 路径式 basename，与 basic.log 中 Init bundle path 及 JAR 内 resources 目录一致 */
    public static final String BUNDLE_BASENAME =
            "com/fr/plugin/report/debug/locale/fr-plugin-report-debug-assistant";

    @Override
    @ExecuteFunctionRecord
    public String find() {
        return BUNDLE_BASENAME;
    }
}
