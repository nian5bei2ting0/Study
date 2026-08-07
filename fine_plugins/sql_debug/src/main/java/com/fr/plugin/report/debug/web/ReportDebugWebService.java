package com.fr.plugin.report.debug.web;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.StringUtils;
import com.fr.stable.fun.impl.NoSessionIDService;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 注册 op=report_debug，按 cmd 分发给 {@link ReportDebugSnapshotAction}。
 */
@FunctionRecorder
public class ReportDebugWebService extends NoSessionIDService {

    public static final String OP = "report_debug";

    private final ReportDebugSnapshotAction snapshotAction = new ReportDebugSnapshotAction();

    @Override
    public String actionOP() {
        return OP;
    }

    @Override
    @ExecuteFunctionRecord
    public void process(HttpServletRequest request, HttpServletResponse response, String frameworkOp) throws Exception {
        // Service.process 第三参是 op（report_debug），不是 URL 的 cmd；必须从请求参数读取
        String effectiveCmd = WebUtils.getHTTPRequestParameter(request, "cmd");
        if (StringUtils.isBlank(effectiveCmd) || OP.equalsIgnoreCase(effectiveCmd)) {
            if (StringUtils.isNotBlank(frameworkOp) && !OP.equalsIgnoreCase(frameworkOp)) {
                effectiveCmd = frameworkOp;
            } else {
                effectiveCmd = ReportDebugSnapshotAction.CMD_SNAPSHOT;
            }
        }
        snapshotAction.actionCMD(request, response, effectiveCmd);
    }
}
