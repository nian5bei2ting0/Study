package com.fr.plugin.drawer.link;

/**
 * 抽屉弹出方向。
 */
public enum DrawerDirection {
    LEFT("left", "左", true),
    RIGHT("right", "右", true),
    TOP("top", "上", false),
    BOTTOM("bottom", "下", false);

    private final String code;
    private final String label;
    private final boolean horizontal;

    DrawerDirection(String code, String label, boolean horizontal) {
        this.code = code;
        this.label = label;
        this.horizontal = horizontal;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public static DrawerDirection fromCode(String code) {
        if (code == null) {
            return RIGHT;
        }
        for (DrawerDirection direction : values()) {
            if (direction.code.equalsIgnoreCase(code)) {
                return direction;
            }
        }
        return RIGHT;
    }

    public static String[] labels() {
        DrawerDirection[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }

    public static DrawerDirection fromLabel(String label) {
        if (label == null) {
            return RIGHT;
        }
        for (DrawerDirection direction : values()) {
            if (direction.label.equals(label) || direction.code.equalsIgnoreCase(label)) {
                return direction;
            }
        }
        return RIGHT;
    }
}
