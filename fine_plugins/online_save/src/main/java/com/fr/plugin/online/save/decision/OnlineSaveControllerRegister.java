package com.fr.plugin.online.save.decision;

import com.fr.decision.fun.impl.AbstractControllerRegisterProvider;

public class OnlineSaveControllerRegister extends AbstractControllerRegisterProvider {

    @Override
    public Class<?>[] getControllers() {
        return new Class<?>[]{
                OnlineSaveDirectoryResource.class
        };
    }
}
