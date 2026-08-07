package com.fr.plugin.report.debug.decision;

import com.fr.decision.webservice.annotation.VisitRefer;
import com.fr.plugin.report.debug.web.ReportDebugSnapshotSupport;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.third.springframework.stereotype.Controller;
import com.fr.third.springframework.web.bind.annotation.RequestMapping;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 决策平台嵌入预览的 REST 备用入口。
 * <p>
 * 当 {@code /decision/ReportServer?op=report_debug} 被登录过滤器拦截时，
 * 预览页改走 {@code /decision/report/debug/assistant/preview/*}（与预览 servlet 同决策上下文）。
 */
@Controller
@RequestMapping(value = "/report/debug/assistant/preview")
@VisitRefer(required = false)
@FunctionRecorder
public class DebugAssistantPreviewResource {

    @RequestMapping(value = "/access", method = RequestMethod.GET)
    @ExecuteFunctionRecord
    public void access(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ReportDebugSnapshotSupport.writeAccess(request, response);
    }

    @RequestMapping(value = "/snapshot", method = RequestMethod.GET)
    @ExecuteFunctionRecord
    public void snapshot(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ReportDebugSnapshotSupport.writeSnapshot(request, response);
    }
}
