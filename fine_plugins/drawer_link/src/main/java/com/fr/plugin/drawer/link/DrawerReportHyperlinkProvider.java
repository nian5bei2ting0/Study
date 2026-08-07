package com.fr.plugin.drawer.link;

import com.fr.design.beans.BasicBeanPane;
import com.fr.design.fun.impl.AbstractHyperlinkProvider;
import com.fr.design.gui.controlpane.NameableCreator;
import com.fr.design.gui.controlpane.NameObjectCreator;
import com.fr.locale.InterProviderFactory;
import com.fr.plugin.drawer.link.design.DrawerReportHyperlinkPane;

/**
 * 设计器超级链接扩展：抽屉报表。
 */
public class DrawerReportHyperlinkProvider extends AbstractHyperlinkProvider {

    @Override
    public NameableCreator createHyperlinkCreator() {
        return new NameObjectCreator(text(), target(), appearance());
    }

    @Override
    public String text() {
        try {
            return InterProviderFactory.getProvider().getLocText("FR-Plugin-Drawer_Report_Hyperlink");
        } catch (Throwable ignore) {
            return "抽屉报表";
        }
    }

    @Override
    public Class<DrawerReportHyperlink> target() {
        return DrawerReportHyperlink.class;
    }

    @Override
    public Class<? extends BasicBeanPane> appearance() {
        return DrawerReportHyperlinkPane.class;
    }
}
