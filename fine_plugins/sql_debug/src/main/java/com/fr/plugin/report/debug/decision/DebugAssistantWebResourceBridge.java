package com.fr.plugin.report.debug.decision;

import com.fr.decision.fun.impl.AbstractWebResourceProvider;
import com.fr.decision.web.MainComponent;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Atom;

@FunctionRecorder
public class DebugAssistantWebResourceBridge extends AbstractWebResourceProvider {

    @Override
    public Atom attach() {
        return MainComponent.KEY;
    }

    @Override
    @ExecuteFunctionRecord
    public Atom client() {
        return ReportDebugOptionClient.KEY;
    }
}
