package com.fr.plugin.czcb.homepage.core;

public final class CzcbHomepageConstants {

    public static final String PLUGIN_ID = "com.fr.plugin.czcb.homepage";
    public static final String PLUGIN_VERSION = "1.0.11";
    public static final String PLUGIN_DIR_PREFIX = "plugin-com.fr.plugin.czcb.homepage";
    public static final String WEBSERVICE_OP = "czcb_homepage";

    public static final String HOMEPAGE_WEB_PATH = "/webroot/help/czcb_homepage/index.html";
    public static final String HOMEPAGE_CONFIG_PATH = "/webroot/help/czcb_homepage/config.html";
    public static final String HOMEPAGE_DEPLOY_DIR = "help/czcb_homepage";

    public static final String DEPLOY_MARKER_FILE = ".czcb-homepage-deploy-version";
    public static final String CONFIG_FILE = "czcb-homepage.properties";
    public static final String MENU_CONFIG_FILE = "nav-menu-config.json";
    public static final String CLASSPATH_HOMEPAGE_PREFIX = "com/fr/plugin/czcb/homepage/web/homepage/";

    /** 目录管理中显示的链接名称 */
    public static final String DIRECTORY_LINK_NAME = "综合大屏首页";
    public static final String DIRECTORY_LINK_DESC = "CZCB homepage plugin";

    private CzcbHomepageConstants() {
    }
}
