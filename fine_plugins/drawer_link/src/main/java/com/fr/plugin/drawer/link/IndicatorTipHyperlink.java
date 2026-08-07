package com.fr.plugin.drawer.link;

import com.fr.base.BaseFormula;
import com.fr.base.ResultFormula;
import com.fr.general.ComparatorUtils;
import com.fr.general.GeneralUtils;
import com.fr.general.PageCalObj;
import com.fr.json.JSONObject;
import com.fr.js.Hyperlink;
import com.fr.log.FineLoggerFactory;
import com.fr.report.core.utils.ScriptUtils;
import com.fr.script.Calculator;
import com.fr.stable.StringUtils;
import com.fr.stable.web.Repository;
import com.fr.stable.xml.XMLPrintWriter;
import com.fr.stable.xml.XMLReadable;
import com.fr.stable.xml.XMLableReader;

/**
 * 指标提示超级链接。
 * <p>
 * 内容用 textNode 字符串持久化。公式仅在 renderContent 内用 ScriptUtils 安全求值（全捕获），
 * 不注册 getExtraParameterizedConfig。
 */
public class IndicatorTipHyperlink extends Hyperlink {

    public static final String DEFAULT_FONT = "Microsoft YaHei";
    public static final int DEFAULT_FONT_SIZE = 14;
    public static final String DEFAULT_FONT_COLOR = "#FFFFFF";
    public static final String DEFAULT_BG_COLOR = "#1A1F2C";
    public static final String DEFAULT_STYLE = IndicatorTipStyle.DARK_CYAN.getId();

    private String content = "";
    private String tipStyle = DEFAULT_STYLE;
    private boolean showCopyButton = false;
    private String fontFamily = DEFAULT_FONT;
    private int fontSize = DEFAULT_FONT_SIZE;
    private String fontColor = DEFAULT_FONT_COLOR;
    private String backgroundColor = DEFAULT_BG_COLOR;

    private transient String evaluatedText;

    public IndicatorTipHyperlink() {
        super();
        setTargetFrame("_self");
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
        this.evaluatedText = null;
    }

    public String getTipStyle() {
        return IndicatorTipStyle.fromId(tipStyle).getId();
    }

    public void setTipStyle(String tipStyle) {
        this.tipStyle = IndicatorTipStyle.fromId(tipStyle).getId();
    }

    public boolean isShowCopyButton() {
        return showCopyButton;
    }

    public void setShowCopyButton(boolean showCopyButton) {
        this.showCopyButton = showCopyButton;
    }

    public String getFontFamily() {
        return StringUtils.isBlank(fontFamily) ? DEFAULT_FONT : fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getFontSize() {
        return fontSize <= 0 ? DEFAULT_FONT_SIZE : fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize <= 0 ? DEFAULT_FONT_SIZE : fontSize;
    }

    public String getFontColor() {
        return StringUtils.isBlank(fontColor) ? DEFAULT_FONT_COLOR : fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public String getBackgroundColor() {
        return StringUtils.isBlank(backgroundColor) ? DEFAULT_BG_COLOR : backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    private static boolean isFormulaExpr(String text) {
        return text != null && text.trim().startsWith("=");
    }

    @Override
    public void renderContent(Calculator calculator) {
        evaluatedText = null;
        String expr = getContent();
        if (calculator == null || !isFormulaExpr(expr)) {
            return;
        }
        try {
            BaseFormula formula = BaseFormula.createFormulaBuilder().build(expr.trim());
            Object result = ScriptUtils.executeNormalFormula(calculator, formula);
            if (result instanceof ResultFormula) {
                result = ((ResultFormula) result).getResult();
            }
            if (result != null && !(result instanceof PageCalObj)) {
                String text = GeneralUtils.objectToString(result);
                if (StringUtils.isNotBlank(text)) {
                    evaluatedText = text;
                }
            }
        } catch (Throwable t) {
            FineLoggerFactory.getLogger().error(
                    "IndicatorTip formula failed: " + expr + ", " + t.getMessage(), t);
            evaluatedText = null;
        }
    }

    public String resolveContent() {
        if (StringUtils.isNotBlank(evaluatedText)) {
            return evaluatedText;
        }
        String expr = getContent();
        if (isFormulaExpr(expr)) {
            return "";
        }
        return expr.replace("\\n", "\n");
    }

    @Override
    public String actionJS(Repository repo) {
        try {
            JSONObject args = JSONObject.create();
            args.put("content", resolveContent());
            args.put("tipStyle", getTipStyle());
            args.put("showCopy", isShowCopyButton());
            args.put("fontFamily", getFontFamily());
            args.put("fontSize", getFontSize());
            args.put("fontColor", getFontColor());
            args.put("backgroundColor", getBackgroundColor());
            String payload = java.util.Base64.getEncoder()
                    .encodeToString(args.toString().getBytes("UTF-8"));
            return "/*FR_INDICATOR_TIP:" + payload + "*/return false;";
        } catch (Throwable e) {
            FineLoggerFactory.getLogger().error(e.getMessage(), e);
            return "return false;";
        }
    }

    @Override
    public JSONObject createJSONObject(Repository repo) throws com.fr.json.JSONException {
        // 只输出超链 JS，不再附加额外 tip 字段，降低前端 JSON 解析风险
        return super.createJSONObject(repo);
    }

    @Override
    protected String getHyperlinkType() {
        return "indicatorTip";
    }

    @Override
    public void writeXML(XMLPrintWriter writer) {
        writer.startTAG("JavaScript").attr("class", getClass().getName());
        super.writeXML(writer);
        writer.startTAG("IndicatorTip")
                .attr("tipStyle", getTipStyle())
                .attr("showCopy", String.valueOf(isShowCopyButton()))
                .attr("fontFamily", getFontFamily())
                .attr("fontSize", getFontSize())
                .attr("fontColor", getFontColor())
                .attr("backgroundColor", getBackgroundColor());
        if (StringUtils.isNotBlank(getContent())) {
            writer.textNode(getContent());
        }
        writer.end();
        writer.end();
    }

    @Override
    public void readXML(XMLableReader reader) {
        if (ComparatorUtils.equals("JavaScript", reader.getTagName())) {
            reader.readXMLObject(new XMLReadable() {
                @Override
                public void readXML(XMLableReader reader) {
                    if (reader.isChildNode()) {
                        String tag = reader.getTagName();
                        if ("IndicatorTip".equals(tag)) {
                            setTipStyle(reader.getAttrAsString("tipStyle", DEFAULT_STYLE));
                            setShowCopyButton(reader.getAttrAsBoolean("showCopy", false));
                            setFontFamily(reader.getAttrAsString("fontFamily", DEFAULT_FONT));
                            setFontSize(reader.getAttrAsInt("fontSize", DEFAULT_FONT_SIZE));
                            setFontColor(reader.getAttrAsString("fontColor", DEFAULT_FONT_COLOR));
                            setBackgroundColor(reader.getAttrAsString("backgroundColor", DEFAULT_BG_COLOR));
                            String value = reader.getElementValue();
                            if (StringUtils.isNotBlank(value)) {
                                setContent(value);
                            } else {
                                String legacy = reader.getAttrAsString("formula", null);
                                if (StringUtils.isNotBlank(legacy)) {
                                    setContent(legacy);
                                }
                            }
                        } else {
                            IndicatorTipHyperlink.super.readXML(reader);
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj)
                && obj instanceof IndicatorTipHyperlink
                && ComparatorUtils.equals(getContent(), ((IndicatorTipHyperlink) obj).getContent())
                && ComparatorUtils.equals(getTipStyle(), ((IndicatorTipHyperlink) obj).getTipStyle())
                && isShowCopyButton() == ((IndicatorTipHyperlink) obj).isShowCopyButton()
                && ComparatorUtils.equals(getFontFamily(), ((IndicatorTipHyperlink) obj).getFontFamily())
                && getFontSize() == ((IndicatorTipHyperlink) obj).getFontSize()
                && ComparatorUtils.equals(getFontColor(), ((IndicatorTipHyperlink) obj).getFontColor())
                && ComparatorUtils.equals(getBackgroundColor(), ((IndicatorTipHyperlink) obj).getBackgroundColor());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        IndicatorTipHyperlink cloned = (IndicatorTipHyperlink) super.clone();
        cloned.setContent(getContent());
        cloned.setTipStyle(getTipStyle());
        cloned.setShowCopyButton(isShowCopyButton());
        cloned.setFontFamily(getFontFamily());
        cloned.setFontSize(getFontSize());
        cloned.setFontColor(getFontColor());
        cloned.setBackgroundColor(getBackgroundColor());
        cloned.evaluatedText = this.evaluatedText;
        return cloned;
    }
}
