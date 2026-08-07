package com.fr.plugin.drawer.link.design;

import com.fr.design.beans.FurtherBasicBeanPane;
import com.fr.design.formula.TinyFormulaPane;
import com.fr.design.gui.icheckbox.UICheckBox;
import com.fr.design.gui.icombobox.UIComboBox;
import com.fr.design.gui.ilable.UILabel;
import com.fr.design.gui.itextfield.UINumberField;
import com.fr.design.layout.FRGUIPaneFactory;
import com.fr.design.layout.TableLayout;
import com.fr.design.layout.TableLayoutHelper;
import com.fr.design.style.color.ColorSelectBox;
import com.fr.design.utils.gui.GUICoreUtils;
import com.fr.plugin.drawer.link.IndicatorTipHyperlink;
import com.fr.plugin.drawer.link.IndicatorTipStyle;
import com.fr.stable.StringUtils;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ItemEvent;

/**
 * 指标提示超链接配置界面（提示内容支持公式，以字符串保存）。
 */
public class IndicatorTipHyperlinkPane extends FurtherBasicBeanPane<IndicatorTipHyperlink> {

    private TinyFormulaPane contentPane;
    private UIComboBox tipStyleComboBox;
    private UICheckBox showCopyCheckBox;
    private UIComboBox fontFamilyComboBox;
    private UINumberField fontSizeField;
    private ColorSelectBox fontColorBox;
    private ColorSelectBox backgroundColorBox;
    private UILabel fontColorLabel;
    private UILabel backgroundColorLabel;

    public IndicatorTipHyperlinkPane() {
        initComponents();
    }

    private void initComponents() {
        setLayout(FRGUIPaneFactory.createBorderLayout());

        contentPane = new TinyFormulaPane();
        contentPane.setPreferredSize(new Dimension(280, 24));

        tipStyleComboBox = new UIComboBox(IndicatorTipStyle.labels());
        tipStyleComboBox.setSelectedItem(IndicatorTipStyle.DARK_CYAN.getLabel());
        tipStyleComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                refreshCustomColorEnabled();
            }
        });

        showCopyCheckBox = new UICheckBox("显示复制按钮");

        fontFamilyComboBox = new UIComboBox(buildFontFamilies());
        fontFamilyComboBox.setSelectedItem(IndicatorTipHyperlink.DEFAULT_FONT);

        fontSizeField = new UINumberField();
        fontSizeField.setInteger(true);
        fontSizeField.setValue(IndicatorTipHyperlink.DEFAULT_FONT_SIZE);

        fontColorLabel = new UILabel("字体颜色");
        fontColorBox = new ColorSelectBox(60);
        fontColorBox.setSelectObject(Color.decode(IndicatorTipHyperlink.DEFAULT_FONT_COLOR));

        backgroundColorLabel = new UILabel("提示框背景");
        backgroundColorBox = new ColorSelectBox(60);
        backgroundColorBox.setSelectObject(Color.decode(IndicatorTipHyperlink.DEFAULT_BG_COLOR));

        double[] rowSize = {
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED
        };
        double[] columnSize = {TableLayout.PREFERRED, TableLayout.FILL};
        Component[][] components = {
                {new UILabel("提示内容"), contentPane},
                {new UILabel("提示框样式"), tipStyleComboBox},
                {new UILabel("复制功能"), showCopyCheckBox},
                {new UILabel("字体"), fontFamilyComboBox},
                {new UILabel("字体大小"), fontSizeField},
                {fontColorLabel, fontColorBox},
                {backgroundColorLabel, backgroundColorBox}
        };
        JPanel center = TableLayoutHelper.createGapTableLayoutPane(components, rowSize, columnSize, 8, 6);
        center.setBorder(GUICoreUtils.createTitledBorder("指标提示", null));
        add(center, BorderLayout.CENTER);
        refreshCustomColorEnabled();
    }

    private void refreshCustomColorEnabled() {
        boolean custom = IndicatorTipStyle.fromLabel(String.valueOf(tipStyleComboBox.getSelectedItem()))
                == IndicatorTipStyle.CUSTOM;
        fontColorBox.setEnabled(custom);
        backgroundColorBox.setEnabled(custom);
        fontColorLabel.setEnabled(custom);
        backgroundColorLabel.setEnabled(custom);
    }

    private String[] buildFontFamilies() {
        String[] preferred = new String[]{
                "Microsoft YaHei", "微软雅黑", "SimSun", "宋体", "SimHei", "黑体",
                "KaiTi", "楷体", "FangSong", "仿宋", "Arial", "Tahoma",
                "Times New Roman", "Courier New", "Verdana"
        };
        try {
            String[] all = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<String>();
            for (String font : preferred) {
                set.add(font);
            }
            if (all != null) {
                for (String font : all) {
                    set.add(font);
                }
            }
            return set.toArray(new String[0]);
        } catch (Throwable ignore) {
            return preferred;
        }
    }

    @Override
    public String title4PopupWindow() {
        return "指标提示";
    }

    @Override
    public boolean accept(Object object) {
        return object instanceof IndicatorTipHyperlink;
    }

    @Override
    public void reset() {
        contentPane.populateBean("");
        tipStyleComboBox.setSelectedItem(IndicatorTipStyle.DARK_CYAN.getLabel());
        showCopyCheckBox.setSelected(false);
        fontFamilyComboBox.setSelectedItem(IndicatorTipHyperlink.DEFAULT_FONT);
        fontSizeField.setValue(IndicatorTipHyperlink.DEFAULT_FONT_SIZE);
        fontColorBox.setSelectObject(Color.decode(IndicatorTipHyperlink.DEFAULT_FONT_COLOR));
        backgroundColorBox.setSelectObject(Color.decode(IndicatorTipHyperlink.DEFAULT_BG_COLOR));
        refreshCustomColorEnabled();
    }

    @Override
    public void checkValid() throws Exception {
        try {
            double size = fontSizeField.getValue();
            if (size < 8 || size > 72) {
                throw new Exception("字体大小需在 8 ~ 72 之间");
            }
        } catch (NumberFormatException e) {
            throw new Exception("请输入有效的字体大小");
        }
    }

    @Override
    public void populateBean(IndicatorTipHyperlink bean) {
        if (bean == null) {
            return;
        }
        contentPane.populateBean(bean.getContent());
        tipStyleComboBox.setSelectedItem(IndicatorTipStyle.fromId(bean.getTipStyle()).getLabel());
        showCopyCheckBox.setSelected(bean.isShowCopyButton());
        fontFamilyComboBox.setSelectedItem(bean.getFontFamily());
        fontSizeField.setValue(bean.getFontSize());
        fontColorBox.setSelectObject(decodeColor(bean.getFontColor(), IndicatorTipHyperlink.DEFAULT_FONT_COLOR));
        backgroundColorBox.setSelectObject(decodeColor(bean.getBackgroundColor(), IndicatorTipHyperlink.DEFAULT_BG_COLOR));
        refreshCustomColorEnabled();
    }

    @Override
    public IndicatorTipHyperlink updateBean() {
        IndicatorTipHyperlink bean = new IndicatorTipHyperlink();
        updateBean(bean);
        return bean;
    }

    @Override
    public void updateBean(IndicatorTipHyperlink bean) {
        if (bean == null) {
            return;
        }
        // 公式与文本一律存字符串，避免 Formula 对象进入引擎危险求值路径
        bean.setContent(contentPane.updateBean());
        bean.setTipStyle(IndicatorTipStyle.fromLabel(String.valueOf(tipStyleComboBox.getSelectedItem())).getId());
        bean.setShowCopyButton(showCopyCheckBox.isSelected());
        Object font = fontFamilyComboBox.getSelectedItem();
        bean.setFontFamily(font == null ? IndicatorTipHyperlink.DEFAULT_FONT : String.valueOf(font));
        try {
            bean.setFontSize((int) fontSizeField.getValue());
        } catch (NumberFormatException e) {
            bean.setFontSize(IndicatorTipHyperlink.DEFAULT_FONT_SIZE);
        }
        bean.setFontColor(toHex(fontColorBox.getSelectObject(), IndicatorTipHyperlink.DEFAULT_FONT_COLOR));
        bean.setBackgroundColor(toHex(backgroundColorBox.getSelectObject(), IndicatorTipHyperlink.DEFAULT_BG_COLOR));
    }

    private Color decodeColor(String hex, String fallback) {
        try {
            return Color.decode(StringUtils.isBlank(hex) ? fallback : hex);
        } catch (Exception e) {
            return Color.decode(fallback);
        }
    }

    private String toHex(Color color, String fallback) {
        if (color == null) {
            return fallback;
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
