package com.fr.plugin.online.save.locale;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractLocaleFinder;

@FunctionRecorder
public class OnlineSaveLocaleFinder extends AbstractLocaleFinder {

    public static final String BUNDLE_BASENAME = "com/fr/plugin/online/save/locale/fr-plugin-online-save";

    @Override
    @ExecuteFunctionRecord
    public String find() {
        return BUNDLE_BASENAME;
    }
}
