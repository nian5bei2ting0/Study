!(function () {
    window.__CZCB_HOMEPAGE_BUILD__ = "1.0.11";
    var OPTION_ID = "decision-management-czcb-homepage";
    var CARD_TYPE = "dec.management.plugin.czcb.homepage";
    var CONFIG_GET = "/czcb/homepage/config/get";
    var CONFIG_DEPLOY = "/czcb/homepage/config/deploy";
    var DEFAULT_CONFIG_URL = "/webroot/help/czcb_homepage/config.html";
    var DEFAULT_HOME_URL = "/webroot/help/czcb_homepage/index.html";

    var TOOLBAR_BG = "#0f172a";
    var TOOLBAR_BORDER = "1px solid rgba(148, 163, 184, 0.22)";
    var ACCENT = "#06b6d4";
    var TEXT_MUTED = "#94a3b8";

    BI.config("dec.constant.management.navigation", function (items) {
        items.push({
            value: "czcb-homepage",
            id: OPTION_ID,
            text: BI.i18nText("Fine-Plugin_Czcb_Homepage"),
            cardType: CARD_TYPE,
            // 大屏/视图类图标，区别于通用 management-log-font
            cls: "management-look-font"
        });
        return items;
    });

    function ensureToolbarStyles() {
        if (document.getElementById("czcb-homepage-mgmt-style")) {
            return;
        }
        var style = document.createElement("style");
        style.id = "czcb-homepage-mgmt-style";
        style.type = "text/css";
        style.appendChild(document.createTextNode([
            ".czcb-homepage-management,",
            ".czcb-homepage-management .bi-vtape,",
            ".czcb-homepage-management .bi-absolute,",
            ".czcb-homepage-management .bi-layout {",
            "  background: " + TOOLBAR_BG + " !important;",
            "}",
            ".czcb-homepage-management .czcb-homepage-toolbar {",
            "  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%) !important;",
            "  border-bottom: " + TOOLBAR_BORDER + ";",
            "  box-sizing: border-box;",
            "}",
            ".czcb-homepage-management .czcb-homepage-toolbar .bi-button {",
            "  background: rgba(6, 182, 212, 0.18) !important;",
            "  border: 1px solid rgba(6, 182, 212, 0.45) !important;",
            "  color: #ecfeff !important;",
            "  border-radius: 6px !important;",
            "}",
            ".czcb-homepage-management .czcb-homepage-toolbar .bi-button:hover {",
            "  background: rgba(6, 182, 212, 0.32) !important;",
            "  border-color: " + ACCENT + " !important;",
            "}",
            ".czcb-homepage-management .czcb-homepage-toolbar-hint {",
            "  color: " + TEXT_MUTED + " !important;",
            "}",
            ".czcb-homepage-management iframe {",
            "  background: " + TOOLBAR_BG + " !important;",
            "}"
        ].join("\n")));
        document.head.appendChild(style);
    }

    function normalizeRes(res) {
        if (!res) {
            return null;
        }
        if (res.status === "success" || res.status === "error") {
            return res;
        }
        if (res.data && (res.data.status === "success" || res.data.status === "error")) {
            return res.data;
        }
        if (res.data) {
            return BI.extend({status: "success"}, res.data);
        }
        return res;
    }

    var CzcbHomepageManagement = BI.inherit(BI.Pane, {
        props: {
            baseCls: "czcb-homepage-management"
        },

        beforeInit: function (callback) {
            var self = this;
            ensureToolbarStyles();
            self._cfg = {
                homepageUrl: DEFAULT_HOME_URL,
                configUrl: DEFAULT_CONFIG_URL,
                deployed: false,
                deployPath: "",
                pluginVersion: ""
            };
            Dec.reqGet(CONFIG_GET, "", function (res) {
                var data = normalizeRes(res);
                if (data && data.status === "success") {
                    self._cfg = BI.extend(self._cfg, data);
                }
                callback();
            });
        },

        render: function () {
            var self = this;
            var cfg = self._cfg || {};
            var configUrl = cfg.configUrl || DEFAULT_CONFIG_URL;
            var homeUrl = cfg.homepageUrl || DEFAULT_HOME_URL;

            return {
                type: "bi.vtape",
                css: {
                    background: TOOLBAR_BG
                },
                items: [
                    {
                        el: {
                            type: "bi.left",
                            cls: "czcb-homepage-toolbar",
                            lgap: 12,
                            vgap: 10,
                            css: {
                                background: "linear-gradient(180deg, #1e293b 0%, #0f172a 100%)",
                                borderBottom: TOOLBAR_BORDER,
                                paddingLeft: "8px",
                                paddingRight: "12px"
                            },
                            items: [
                                {
                                    type: "bi.button",
                                    text: BI.i18nText("Czcb_Homepage_Open_Index"),
                                    height: 28,
                                    handler: function () {
                                        window.open(homeUrl, "_blank");
                                    }
                                },
                                {
                                    type: "bi.button",
                                    text: BI.i18nText("Czcb_Homepage_Redeploy"),
                                    height: 28,
                                    handler: function () {
                                        Dec.reqPost(CONFIG_DEPLOY + "?force=true", {}, function (res) {
                                            var data = normalizeRes(res);
                                            if (data && data.status === "success") {
                                                BI.Msg.toast(BI.i18nText("Czcb_Homepage_Deploy_Ok"), {level: "success"});
                                                if (self.iframe) {
                                                    self.iframe.attr("src", configUrl + "?_t=" + Date.now());
                                                }
                                            } else {
                                                BI.Msg.toast(
                                                    (data && data.errorMsg) || BI.i18nText("Czcb_Homepage_Deploy_Fail"),
                                                    {level: "error"}
                                                );
                                            }
                                        });
                                    }
                                },
                                {
                                    type: "bi.label",
                                    cls: "czcb-homepage-toolbar-hint",
                                    text: BI.i18nText("Czcb_Homepage_Hint"),
                                    height: 28,
                                    css: {
                                        "line-height": "28px",
                                        color: TEXT_MUTED
                                    }
                                }
                            ]
                        },
                        height: 48
                    },
                    {
                        el: {
                            type: "bi.absolute",
                            css: {
                                background: TOOLBAR_BG
                            },
                            ref: function (ref) {
                                self.frameWrap = ref;
                            },
                            items: [{
                                el: {
                                    type: "bi.layout",
                                    css: {
                                        background: TOOLBAR_BG
                                    },
                                    ref: function (ref) {
                                        self.frameHost = ref;
                                    }
                                },
                                top: 0,
                                left: 0,
                                right: 0,
                                bottom: 0
                            }]
                        }
                    }
                ]
            };
        },

        mounted: function () {
            var self = this;
            ensureToolbarStyles();
            var cfg = self._cfg || {};
            var configUrl = cfg.configUrl || DEFAULT_CONFIG_URL;
            if (!self.frameHost || !self.frameHost.element) {
                return;
            }
            var $iframe = $("<iframe frameborder='0' allowfullscreen='true'></iframe>");
            $iframe.css({
                width: "100%",
                height: "100%",
                border: "0",
                display: "block",
                background: TOOLBAR_BG
            });
            $iframe.attr("src", configUrl);
            self.frameHost.element.empty().append($iframe);
            self.iframe = $iframe;
        }
    });

    BI.shortcut(CARD_TYPE, CzcbHomepageManagement);
}());
