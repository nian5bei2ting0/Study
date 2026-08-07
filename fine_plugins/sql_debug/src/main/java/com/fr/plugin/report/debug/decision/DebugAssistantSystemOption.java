package com.fr.plugin.report.debug.decision;

import com.fr.decision.fun.impl.AbstractSystemOptionProvider;
import com.fr.decision.web.MainComponent;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Atom;

@FunctionRecorder
public class DebugAssistantSystemOption extends AbstractSystemOptionProvider {

    public static final String OPTION_ID = "decision-management-report-debug-assistant";
    /**
     * 须返回 i18n 键名（勿 getLocText），与数据门户 Fine-Plugin_Data_Portal 一致；
     * 否则 FINE_AUTHORITY_OBJECT 会写入 UTF-8 字节导致权限树乱码。
     */
    public static final String I18N_NAME_KEY = "Fine-Plugin_Report_Debug_Assistant";
    /** 根级菜单，与数据门户 decision-management-portal 同级，排在列表最底部 */
    private static final String PARENT_ID = "decision-management-root";
    private static final String FULL_PATH = "decision-management-root";
    /** 排在数据门户(1022)、数据预警(1030) 之后 */
    private static final int SORT_INDEX = 2035;

    @Override
    public String id() {
        return OPTION_ID;
    }

    @Override
    public String parentId() {
        return PARENT_ID;
    }

    @Override
    public String fullPath() {
        return FULL_PATH;
    }

    @Override
    @ExecuteFunctionRecord
    public String displayName() {
        return I18N_NAME_KEY;
    }

    @Override
    public int sortIndex() {
        return SORT_INDEX;
    }

    @Override
    public Atom attach() {
        return MainComponent.KEY;
    }

    @Override
    public Atom client() {
        return ReportDebugOptionClient.KEY;
    }
}
