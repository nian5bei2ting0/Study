package com.fr.plugin.czcb.homepage.decision;

import com.fr.decision.fun.impl.AbstractControllerRegisterProvider;
import com.fr.plugin.transform.FunctionRecorder;

@FunctionRecorder
public class CzcbHomepageControllerRegister extends AbstractControllerRegisterProvider {

    @Override
    public Class<?>[] getControllers() {
        return new Class<?>[]{
                CzcbHomepageConfigResource.class
        };
    }
}
