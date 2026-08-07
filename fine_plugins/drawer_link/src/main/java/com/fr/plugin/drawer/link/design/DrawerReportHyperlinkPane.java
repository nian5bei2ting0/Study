package com.fr.plugin.drawer.link.design;

import com.fr.design.dialog.DialogActionAdapter;
import com.fr.design.gui.frpane.ReportletParameterViewPane;
import com.fr.design.gui.ibutton.UIButton;
import com.fr.design.gui.icheckbox.UICheckBox;
import com.fr.design.gui.icombobox.UIComboBox;
import com.fr.design.gui.ilable.UILabel;
import com.fr.design.gui.itextfield.UINumberField;
import com.fr.design.gui.itextfield.UITextField;
import com.fr.design.gui.itree.filetree.ReportletPane;
import com.fr.design.hyperlink.AbstractHyperLinkPane;
import com.fr.design.i18n.Toolkit;
import com.fr.design.layout.FRGUIPaneFactory;
import com.fr.design.layout.TableLayout;
import com.fr.design.layout.TableLayoutHelper;
import com.fr.design.utils.gui.GUICoreUtils;
import com.fr.plugin.drawer.link.DrawerDirection;
import com.fr.plugin.drawer.link.DrawerReportHyperlink;
import com.fr.stable.ParameterProvider;
import com.fr.stable.StringUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;

/**
 * 抽屉报表超链接配置界面：网络报表、方向、尺寸、参数。
 */
public class DrawerReportHyperlinkPane extends AbstractHyperLinkPane<DrawerReportHyperlink> {

    private UITextField reportPathTextField;
    private UIButton browserButton;
    private UIComboBox directionComboBox;
    private UILabel sizeLabel;
    private UINumberField sizeField;
    private UITextField titleField;
    private UIComboBox paramMethodComboBox;
    private UICheckBox extendParametersCheckBox;
    private UICheckBox showParameterInterfaceCheckBox;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    public DrawerReportHyperlinkPane() {
        initComponents();
    }

    public DrawerReportHyperlinkPane(HashMap hyperLinkEditorMap, boolean needRenamePane) {
        super(hyperLinkEditorMap, needRenamePane);
        initComponents();
    }

    private void initComponents() {
        setLayout(FRGUIPaneFactory.createBorderLayout());
        add(createNorthPane(), BorderLayout.NORTH);

        setParameterViewPane(new ReportletParameterViewPane(getChartParaType(), getValueEditorPane(), getValueEditorPane()));
        getParameterViewPane().setBorder(GUICoreUtils.createTitledBorder(
                Toolkit.i18nText("Fine-Design_Basic_Parameter"), null));
        add(getParameterViewPane(), BorderLayout.CENTER);

        JPanel southPane = FRGUIPaneFactory.createY_AXISBoxInnerContainer_L_Pane();
        extendParametersCheckBox = new UICheckBox(
                Toolkit.i18nText("Fine-Design_Basic_Hyperlink_Extends_Report_Parameters"));
        showParameterInterfaceCheckBox = new UICheckBox("显示参数界面");
        southPane.add(extendParametersCheckBox);
        southPane.add(showParameterInterfaceCheckBox);
        add(southPane, BorderLayout.SOUTH);
    }

    private JPanel createNorthPane() {
        reportPathTextField = new UITextField();
        browserButton = new UIButton(Toolkit.i18nText("Fine-Design_Basic_Select"));
        browserButton.setPreferredSize(new Dimension(browserButton.getPreferredSize().width, 20));
        browserButton.addActionListener(e -> chooseReportlet());

        JPanel pathPane = new JPanel(FRGUIPaneFactory.createBorderLayout());
        pathPane.add(reportPathTextField, BorderLayout.CENTER);
        pathPane.add(browserButton, BorderLayout.EAST);

        directionComboBox = new UIComboBox(DrawerDirection.labels());
        directionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                refreshSizeLabel();
            }
        });

        sizeLabel = new UILabel("宽度(px)");
        sizeField = new UINumberField();
        sizeField.setInteger(true);
        sizeField.setValue(DrawerReportHyperlink.DEFAULT_SIZE);

        titleField = new UITextField();

        paramMethodComboBox = new UIComboBox(new String[]{METHOD_GET, METHOD_POST});
        paramMethodComboBox.setSelectedItem(METHOD_GET);

        double[] rowSize = {
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED
        };
        double[] columnSize = {TableLayout.PREFERRED, TableLayout.FILL};
        Component[][] components = {
                {new UILabel("网络报表"), pathPane},
                {new UILabel("抽屉方向"), directionComboBox},
                {sizeLabel, sizeField},
                {new UILabel("标题"), titleField},
                {new UILabel("参数传递方式"), paramMethodComboBox}
        };
        JPanel north = TableLayoutHelper.createGapTableLayoutPane(components, rowSize, columnSize, 8, 6);
        north.setBorder(GUICoreUtils.createTitledBorder("抽屉报表", null));
        refreshSizeLabel();
        return north;
    }

    private void refreshSizeLabel() {
        DrawerDirection direction = DrawerDirection.fromLabel(String.valueOf(directionComboBox.getSelectedItem()));
        sizeLabel.setText(direction.isHorizontal() ? "宽度(px)" : "高度(px)");
    }

    private void chooseReportlet() {
        final ReportletPane reportletPane = new ReportletPane();
        reportletPane.setSelectedReportletPath(reportPathTextField.getText());
        reportletPane.showWindow(SwingUtilities.getWindowAncestor(this), new DialogActionAdapter() {
            @Override
            public void doOk() {
                String path = reportletPane.getSelectedReportletPath();
                if (StringUtils.isNotBlank(path)) {
                    reportPathTextField.setText(path);
                }
            }
        }).setVisible(true);
    }

    @Override
    public String title4PopupWindow() {
        return "抽屉报表";
    }

    @Override
    public void checkValid() throws Exception {
        String path = reportPathTextField.getText();
        if (StringUtils.isBlank(path)) {
            throw new Exception("请选择网络报表");
        }
        DrawerReportHyperlink.PathParts parts = DrawerReportHyperlink.splitReportPath(path.trim());
        if (StringUtils.isBlank(parts.path)) {
            throw new Exception("网络报表路径无效");
        }
        String lower = parts.path.toLowerCase();
        if (!(lower.endsWith(".cpt") || lower.endsWith(".frm") || lower.endsWith(".fvs")
                || lower.endsWith(".xlsx") || lower.endsWith(".xls"))) {
            throw new Exception("请选择有效的报表模板（cpt/frm/fvs等）");
        }
        try {
            double size = sizeField.getValue();
            if (size <= 0 || size > DrawerReportHyperlink.MAX_SIZE) {
                throw new Exception("尺寸需在 1 ~ " + DrawerReportHyperlink.MAX_SIZE + " px 之间");
            }
        } catch (NumberFormatException e) {
            throw new Exception("请输入有效的尺寸数值");
        }
    }

    @Override
    public void populateBean(DrawerReportHyperlink bean) {
        if (bean == null) {
            return;
        }
        reportPathTextField.setText(bean.getReportletPath());
        directionComboBox.setSelectedItem(bean.getDrawerDirection().getLabel());
        sizeField.setValue(bean.getDrawerSize());
        titleField.setText(bean.getDrawerTitle());
        paramMethodComboBox.setSelectedItem(bean.isByPost() ? METHOD_POST : METHOD_GET);
        extendParametersCheckBox.setSelected(bean.isExtendParameters());
        showParameterInterfaceCheckBox.setSelected(bean.isShowParameterInterface());
        getParameterViewPane().populate(bean.getParameters());
        refreshSizeLabel();
    }

    @Override
    public DrawerReportHyperlink updateBean() {
        DrawerReportHyperlink hyperlink = new DrawerReportHyperlink();
        updateBean(hyperlink);
        return hyperlink;
    }

    @Override
    public void updateBean(DrawerReportHyperlink bean) {
        if (bean == null) {
            return;
        }
        String rawPath = reportPathTextField.getText();
        if (rawPath != null) {
            rawPath = rawPath.trim();
        }
        // 规范化：模板后缀后的 &/? 查询串保留在路径中由运行期拆分合并
        bean.setReportletPath(rawPath);
        DrawerDirection direction = DrawerDirection.fromLabel(String.valueOf(directionComboBox.getSelectedItem()));
        bean.setDrawerDirection(direction);
        try {
            bean.setDrawerSize((int) sizeField.getValue());
        } catch (NumberFormatException ignore) {
            bean.setDrawerSize(DrawerReportHyperlink.DEFAULT_SIZE);
        }
        bean.setDrawerTitle(titleField.getText());
        bean.setExtendParameters(extendParametersCheckBox.isSelected());
        bean.setShowParameterInterface(showParameterInterfaceCheckBox.isSelected());
        bean.setByPost(METHOD_POST.equals(String.valueOf(paramMethodComboBox.getSelectedItem())));

        List<ParameterProvider> parameters = getParameterViewPane().update();
        bean.setParameters(parameters.toArray(new ParameterProvider[0]));
    }
}
