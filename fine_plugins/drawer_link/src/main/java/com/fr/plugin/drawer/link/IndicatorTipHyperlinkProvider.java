package com.fr.plugin.drawer.link;

import com.fr.design.beans.BasicBeanPane;
import com.fr.design.fun.impl.AbstractHyperlinkProvider;
import com.fr.design.gui.controlpane.NameableCreator;
import com.fr.design.gui.controlpane.NameObjectCreator;
import com.fr.locale.InterProviderFactory;
import com.fr.plugin.drawer.link.design.IndicatorTipHyperlinkPane;

/**
 * 设计器超级链接扩展：指标提示。
 */
public class IndicatorTipHyperlinkProvider extends AbstractHyperlinkProvider {

    @Override
    public NameableCreator createHyperlinkCreator() {
        return new NameObjectCreator(text(), target(), appearance());
    }

    @Override
    public String text() {
        try {
            return InterProviderFactory.getProvider().getLocText("FR-Plugin-Indicator_Tip_Hyperlink");
        } catch (Throwable ignore) {
            return "指标提示";
        }
    }

    @Override
    public Class<IndicatorTipHyperlink> target() {
        return IndicatorTipHyperlink.class;
    }

    @Override
    public Class<? extends BasicBeanPane> appearance() {
        return IndicatorTipHyperlinkPane.class;
    }
}
