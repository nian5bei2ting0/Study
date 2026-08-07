package com.fr.plugin.czcb.homepage.locale;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractLocaleFinder;

@FunctionRecorder
public class CzcbHomepageLocaleFinder extends AbstractLocaleFinder {

    public static final String BUNDLE_BASENAME =
            "com/fr/plugin/czcb/homepage/locale/fr-plugin-czcb-homepage";

    @Override
    @ExecuteFunctionRecord
    public String find() {
        return BUNDLE_BASENAME;
    }
}
