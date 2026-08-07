package com.fr.plugin.report.debug.decision;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Component;
import com.fr.web.struct.Filter;
import com.fr.web.struct.browser.RequestClient;
import com.fr.web.struct.category.ScriptPath;
import com.fr.web.struct.category.StylePath;

@FunctionRecorder
public class ReportDebugOptionClient extends Component {

    public static final ReportDebugOptionClient KEY = new ReportDebugOptionClient();

    private ReportDebugOptionClient() {
    }

    @ExecuteFunctionRecord
    public ScriptPath script(RequestClient client) {
        return ScriptPath.build("/com/fr/plugin/report/debug/decision/bundle.js");
    }

    @ExecuteFunctionRecord
    public StylePath style(RequestClient client) {
        return null;
    }

    public Filter filter() {
        return new Filter() {
            @Override
            public boolean accept() {
                return true;
            }
        };
    }
}
