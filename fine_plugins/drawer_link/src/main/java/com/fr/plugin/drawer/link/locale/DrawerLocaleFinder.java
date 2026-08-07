package com.fr.plugin.drawer.link.locale;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractLocaleFinder;

@FunctionRecorder
public class DrawerLocaleFinder extends AbstractLocaleFinder {

    @Override
    @ExecuteFunctionRecord
    public String find() {
        return "com/fr/plugin/drawer/link/locale/drawer";
    }
}
