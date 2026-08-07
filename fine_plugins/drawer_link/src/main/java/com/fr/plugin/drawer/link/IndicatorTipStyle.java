package com.fr.plugin.drawer.link;

/**
 * 指标提示内置样式。
 */
public enum IndicatorTipStyle {

    DARK_CYAN("darkCyan", "深色青边"),
    DARK_SIMPLE("darkSimple", "深色简约"),
    LIGHT_WARM("lightWarm", "浅黄提示"),
    LIGHT_INFO("lightInfo", "浅蓝信息"),
    SUCCESS("success", "绿色成功"),
    WARNING("warning", "橙色警告"),
    CUSTOM("custom", "自定义");

    private final String id;
    private final String label;

    IndicatorTipStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static IndicatorTipStyle fromId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return DARK_CYAN;
        }
        for (IndicatorTipStyle style : values()) {
            if (style.id.equalsIgnoreCase(id.trim())) {
                return style;
            }
        }
        return DARK_CYAN;
    }

    public static String[] labels() {
        IndicatorTipStyle[] all = values();
        String[] labels = new String[all.length];
        for (int i = 0; i < all.length; i++) {
            labels[i] = all[i].label;
        }
        return labels;
    }

    public static IndicatorTipStyle fromLabel(String label) {
        if (label == null) {
            return DARK_CYAN;
        }
        for (IndicatorTipStyle style : values()) {
            if (style.label.equals(label)) {
                return style;
            }
        }
        return fromId(label);
    }
}
