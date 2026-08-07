package com.fr.plugin.czcb.homepage.decision;

import com.fr.decision.fun.impl.AbstractSystemOptionProvider;
import com.fr.decision.web.MainComponent;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Atom;

@FunctionRecorder
public class CzcbHomepageSystemOption extends AbstractSystemOptionProvider {

    public static final String OPTION_ID = "decision-management-czcb-homepage";
    public static final String I18N_NAME_KEY = "Fine-Plugin_Czcb_Homepage";
    private static final String PARENT_ID = "decision-management-root";
    private static final String FULL_PATH = "decision-management-root";
    private static final int SORT_INDEX = 2050;

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
        return CzcbHomepageOptionClient.KEY;
    }
}
