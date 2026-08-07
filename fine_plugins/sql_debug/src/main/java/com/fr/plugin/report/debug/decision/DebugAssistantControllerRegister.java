package com.fr.plugin.report.debug.decision;

import com.fr.decision.fun.impl.AbstractControllerRegisterProvider;
import com.fr.plugin.transform.FunctionRecorder;

@FunctionRecorder
public class DebugAssistantControllerRegister extends AbstractControllerRegisterProvider {

    @Override
    public Class<?>[] getControllers() {
        return new Class<?>[]{
                DebugAssistantConfigResource.class,
                DebugAssistantPreviewResource.class
        };
    }
}
