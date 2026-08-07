package com.fr.plugin.drawer.link.css;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractCssFileHandler;

@FunctionRecorder
public class DrawerCssFileHandler extends AbstractCssFileHandler {

    @Override
    @ExecuteFunctionRecord
    public String[] pathsForFiles() {
        return new String[]{
                "/com/fr/plugin/drawer/link/css/drawer_report.css",
                "/com/fr/plugin/drawer/link/css/indicator_tip.css"
        };
    }
}
