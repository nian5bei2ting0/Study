package com.fr.plugin.czcb.homepage.decision;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Component;
import com.fr.web.struct.Filter;
import com.fr.web.struct.browser.RequestClient;
import com.fr.web.struct.category.ScriptPath;
import com.fr.web.struct.category.StylePath;

@FunctionRecorder
public class CzcbHomepageOptionClient extends Component {

    public static final CzcbHomepageOptionClient KEY = new CzcbHomepageOptionClient();

    private CzcbHomepageOptionClient() {
    }

    @ExecuteFunctionRecord
    public ScriptPath script(RequestClient client) {
        return ScriptPath.build("/com/fr/plugin/czcb/homepage/decision/bundle.js");
    }

    @ExecuteFunctionRecord
    public StylePath style(RequestClient client) {
        return null;
    }

    /**
     * 挂载点已由 WebResourceBridge.attach(MainComponent) 约束。
     * 脚本体积小，保持加载；避免不可靠的 Request 探测导致管理页漏载。
     */
    public Filter filter() {
        return new Filter() {
            @Override
            public boolean accept() {
                return true;
            }
        };
    }
}
