package com.fr.plugin.drawer.link.js;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractJavaScriptFileHandler;

@FunctionRecorder
public class DrawerJavaScriptFileHandler extends AbstractJavaScriptFileHandler {

    @Override
    @ExecuteFunctionRecord
    public String[] pathsForFiles() {
        return new String[]{
                "/com/fr/plugin/drawer/link/js/drawer_report.js",
                "/com/fr/plugin/drawer/link/js/indicator_tip.js"
        };
    }
}
