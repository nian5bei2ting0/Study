/**
 * 导航菜单配置：服务端组织级持久化 + localStorage 缓存。
 * 安装后无内置菜单；报表 URL 默认仅允许同源站内路径。
 */
(function (global) {
  const STORAGE_KEY = "homepage_nav_menu_config_v1";
  const MENU_CONFIG_SAVED_EVENT = "homepage:menu-config-saved";
  const SERVER_MENU_API_CANDIDATES = [
    "/webroot/ReportServer?op=czcb_homepage&cmd=menu_get",
    "/ReportServer?op=czcb_homepage&cmd=menu_get"
  ];
  const SERVER_SAVE_WS_CANDIDATES = [
    "/webroot/ReportServer?op=czcb_homepage&cmd=menu_save",
    "/ReportServer?op=czcb_homepage&cmd=menu_save"
  ];
  const SERVER_SAVE_REST_CANDIDATES = [
    "/webroot/decision/czcb/homepage/config/save",
    "/decision/czcb/homepage/config/save"
  ];
  const DECISION_SAVE_PATH = "/czcb/homepage/config/save";

  const EMPTY_MENU_CONFIG = [];
  const ICON_CLASS_RE = /^fa-(solid|regular|brands)(\s+fa-[a-z0-9-]+)+$/i;

  const ICON_OPTIONS = [
    { value: "fa-solid fa-folder", label: "文件夹" },
    { value: "fa-solid fa-chart-pie", label: "饼图" },
    { value: "fa-solid fa-chart-line", label: "折线" },
    { value: "fa-solid fa-chart-column", label: "柱状" },
    { value: "fa-solid fa-wallet", label: "钱包" },
    { value: "fa-solid fa-briefcase", label: "公文包" },
    { value: "fa-solid fa-building", label: "建筑" },
    { value: "fa-solid fa-building-columns", label: "银行" },
    { value: "fa-solid fa-shield-halved", label: "盾牌" },
    { value: "fa-solid fa-user-shield", label: "用户盾" },
    { value: "fa-solid fa-users", label: "用户组" },
    { value: "fa-solid fa-user", label: "用户" },
    { value: "fa-solid fa-house", label: "首页" },
    { value: "fa-solid fa-gauge-high", label: "仪表盘" },
    { value: "fa-solid fa-wave-square", label: "波形" },
    { value: "fa-solid fa-gears", label: "齿轮" },
    { value: "fa-solid fa-database", label: "数据库" },
    { value: "fa-solid fa-file-lines", label: "文档" },
    { value: "fa-solid fa-table", label: "表格" },
    { value: "fa-solid fa-map-location-dot", label: "地图" },
    { value: "fa-solid fa-globe", label: "地球" },
    { value: "fa-solid fa-coins", label: "硬币" },
    { value: "fa-solid fa-credit-card", label: "信用卡" },
    { value: "fa-solid fa-handshake", label: "握手" },
    { value: "fa-solid fa-bell", label: "铃铛" },
    { value: "fa-solid fa-star", label: "星标" },
    { value: "fa-solid fa-layer-group", label: "图层" },
    { value: "fa-solid fa-sitemap", label: "结构" },
    { value: "fa-solid fa-list", label: "列表" },
    { value: "fa-solid fa-compass", label: "指南针" }
  ];

  const ICON_VALUE_SET = {};
  ICON_OPTIONS.forEach((o) => {
    ICON_VALUE_SET[o.value] = true;
  });

  function allowExternalReportUrl() {
    return global.__HOMEPAGE_ALLOW_EXTERNAL_REPORT_URL__ === true;
  }

  function isHttpOrHttpsUrlString(s) {
    try {
      const base =
        typeof global.location !== "undefined" && global.location.href
          ? global.location.href
          : "http://localhost/";
      const u = new URL(s, base);
      return u.protocol === "http:" || u.protocol === "https:";
    } catch (e) {
      return false;
    }
  }

  /**
   * 默认仅允许空串或以单个 / 开头的站内路径。
   * allowExternal === true（或全局 __HOMEPAGE_ALLOW_EXTERNAL_REPORT_URL__）时允许 http(s)。
   */
  function isSafeReportUrlString(s, allowExternal) {
    if (s == null || typeof s !== "string") return true;
    const t = s.trim();
    if (!t) return true;
    if (t.startsWith("//")) return false;
    if (t.startsWith("/")) return true;
    const extOk = allowExternal === true || allowExternalReportUrl();
    if (!extOk) return false;
    return isHttpOrHttpsUrlString(t);
  }

  function isAllowedIconClass(iconClass) {
    if (typeof iconClass !== "string") return false;
    const t = iconClass.trim();
    if (!t) return false;
    if (ICON_VALUE_SET[t]) return true;
    return ICON_CLASS_RE.test(t);
  }

  function normalizeIconClass(iconClass) {
    if (isAllowedIconClass(iconClass)) return iconClass.trim();
    return ICON_OPTIONS[0].value;
  }

  function createMenuKey() {
    const rand =
      typeof global.crypto !== "undefined" && typeof global.crypto.randomUUID === "function"
        ? global.crypto.randomUUID().replace(/-/g, "").slice(0, 12)
        : `${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
    return `menu_${rand}`;
  }

  function hasEffectiveSubmenu(item) {
    const submenu = Array.isArray(item?.submenu) ? item.submenu : [];
    return submenu.some(
      (s) =>
        (typeof s?.label === "string" && s.label.trim() !== "") ||
        (typeof s?.url === "string" && s.url.trim() !== "")
    );
  }

  /**
   * 仅一级时默认可点；有二级时需 primaryClickable === true 才可点一级跳转。
   */
  function isPrimaryClickable(item) {
    if (!item || typeof item !== "object") return false;
    if (!hasEffectiveSubmenu(item)) return true;
    return item.primaryClickable === true;
  }

  function normalizeMenuConfig(config) {
    if (!Array.isArray(config)) return [];
    return config.map((item) => ({
      key: typeof item?.key === "string" ? item.key.trim() : "",
      iconClass: normalizeIconClass(item?.iconClass),
      label: typeof item?.label === "string" ? item.label : "",
      url: typeof item?.url === "string" ? item.url : "",
      external: item?.external === true,
      primaryClickable: item?.primaryClickable === true,
      submenu: Array.isArray(item?.submenu)
        ? item.submenu.map((s) => ({
            label: typeof s?.label === "string" ? s.label : "",
            url: typeof s?.url === "string" ? s.url : "",
            external: s?.external === true
          }))
        : []
    }));
  }

  function isValidMenuConfig(config) {
    if (!Array.isArray(config)) return false;
    if (config.length === 0) return true;
    return config.every((item) => {
      const submenuEntries = Array.isArray(item?.submenu) ? item.submenu : [];
      const primaryUrlOk =
        item.url === undefined ||
        (typeof item.url === "string" &&
          isSafeReportUrlString(item.url, item.external === true));
      const clickableOk =
        item.primaryClickable === undefined || typeof item.primaryClickable === "boolean";
      const externalOk =
        item.external === undefined || typeof item.external === "boolean";
      return (
        item &&
        typeof item.key === "string" &&
        item.key.trim() !== "" &&
        isAllowedIconClass(item.iconClass) &&
        typeof item.label === "string" &&
        primaryUrlOk &&
        clickableOk &&
        externalOk &&
        submenuEntries.every(
          (entry) =>
            entry &&
            typeof entry === "object" &&
            typeof entry.label === "string" &&
            typeof entry.url === "string" &&
            (entry.external === undefined || typeof entry.external === "boolean") &&
            isSafeReportUrlString(entry.url, entry.external === true)
        )
      );
    });
  }

  function cloneEmptyConfig() {
    return EMPTY_MENU_CONFIG.slice();
  }

  function cloneDefaultConfig() {
    return cloneEmptyConfig();
  }

  function loadSavedMenuConfig() {
    try {
      const raw = global.localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = normalizeMenuConfig(JSON.parse(raw));
      return isValidMenuConfig(parsed) ? parsed : null;
    } catch (e) {
      return null;
    }
  }

  function getEffectiveMenuConfig() {
    const saved = loadSavedMenuConfig();
    if (saved) return saved;
    return cloneEmptyConfig();
  }

  function hasSavedMenuConfig() {
    return loadSavedMenuConfig() !== null;
  }

  function cacheLocalMenuConfig(config) {
    try {
      global.localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
      return true;
    } catch (e) {
      console.error("cacheLocalMenuConfig failed", e);
      return false;
    }
  }

  function notifyMenuConfigSaved() {
    try {
      global.dispatchEvent(new CustomEvent(MENU_CONFIG_SAVED_EVENT));
    } catch (e) {
      /* ignore */
    }
  }

  function parseMenuPayload(json) {
    let raw = json?.menuConfigJson ?? json?.menuConfig ?? json?.data?.menuConfigJson ?? json?.data?.menuConfig;
    if (raw == null && json?.data && typeof json.data === "object") {
      raw = json.data.menuConfigJson ?? json.data.menuConfig;
    }
    if (typeof raw === "string") {
      try {
        raw = JSON.parse(raw);
      } catch (e) {
        return null;
      }
    }
    const normalized = normalizeMenuConfig(raw);
    return isValidMenuConfig(normalized) ? normalized : null;
  }

  async function fetchServerMenuConfig() {
    if (typeof global.fetch !== "function") return null;
    for (let i = 0; i < SERVER_MENU_API_CANDIDATES.length; i++) {
      try {
        const res = await global.fetch(SERVER_MENU_API_CANDIDATES[i], {
          credentials: "same-origin",
          cache: "no-store"
        });
        if (!res.ok) continue;
        const json = await res.json();
        const cfg = parseMenuPayload(json);
        if (cfg) return cfg;
        if (json && (json.status === "success" || json.data?.status === "success")) {
          const empty = parseMenuPayload({ menuConfigJson: "[]" });
          return empty || [];
        }
      } catch (e) {
        /* try next */
      }
    }
    return null;
  }

  function normalizeSaveResult(json) {
    if (!json) return { ok: false, reason: "empty" };
    const status = json.status || (json.data && json.data.status);
    if (status === "success" || json.success === true) {
      return { ok: true };
    }
    if (status === "error" || json.success === false) {
      return {
        ok: false,
        reason: json.errorMsg || (json.data && json.data.errorMsg) || json.errorCode || "error"
      };
    }
    return { ok: false, reason: "unknown" };
  }

  function saveViaParentDec(config) {
    return new Promise(function (resolve) {
      try {
        const parentWin = global.parent && global.parent !== global ? global.parent : null;
        const Dec = (parentWin && parentWin.Dec) || global.Dec;
        if (!Dec || typeof Dec.reqPost !== "function") {
          resolve({ ok: false, reason: "no_dec" });
          return;
        }
        Dec.reqPost(
          DECISION_SAVE_PATH,
          { menuConfigJson: JSON.stringify(config) },
          function (res) {
            const payload = res && res.data && (res.data.status || res.data.menuConfigJson) ? res.data : res;
            resolve(normalizeSaveResult(payload || res));
          }
        );
      } catch (e) {
        resolve({ ok: false, reason: "dec_error" });
      }
    });
  }

  async function saveViaReportServer(config) {
    if (typeof global.fetch !== "function") {
      return { ok: false, reason: "nofetch" };
    }
    const formBody =
      "menuConfigJson=" + encodeURIComponent(JSON.stringify(config));
    for (let i = 0; i < SERVER_SAVE_WS_CANDIDATES.length; i++) {
      try {
        const res = await global.fetch(SERVER_SAVE_WS_CANDIDATES[i], {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
          body: formBody
        });
        if (!res.ok) {
          if (res.status === 401 || res.status === 403) {
            return { ok: false, reason: "unauthorized" };
          }
          continue;
        }
        const json = await res.json();
        const result = normalizeSaveResult(json);
        if (result.ok) return result;
        if (result.reason === "login required" || result.reason === "unauthorized") {
          return { ok: false, reason: "unauthorized" };
        }
      } catch (e) {
        /* try next */
      }
    }
    return { ok: false, reason: "network" };
  }

  async function saveViaDecisionRest(config) {
    if (typeof global.fetch !== "function") {
      return { ok: false, reason: "nofetch" };
    }
    const body = JSON.stringify({ menuConfigJson: JSON.stringify(config) });
    for (let i = 0; i < SERVER_SAVE_REST_CANDIDATES.length; i++) {
      try {
        const res = await global.fetch(SERVER_SAVE_REST_CANDIDATES[i], {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json" },
          body
        });
        if (!res.ok) {
          if (res.status === 401 || res.status === 403) {
            return { ok: false, reason: "unauthorized" };
          }
          continue;
        }
        const json = await res.json();
        const payload = json && json.data && json.data.status ? json.data : json;
        const result = normalizeSaveResult(payload);
        if (result.ok) return result;
      } catch (e) {
        /* try next */
      }
    }
    return { ok: false, reason: "network" };
  }

  async function saveMenuConfigToServer(config) {
    // 1) 管理系统 iframe：优先走父页 Dec.reqPost（自带决策鉴权）
    const viaDec = await saveViaParentDec(config);
    if (viaDec.ok) return viaDec;
    if (viaDec.reason && viaDec.reason !== "no_dec" && viaDec.reason !== "dec_error") {
      // Dec 已调用但业务失败，不再盲试
      if (viaDec.reason !== "unknown") return viaDec;
    }
    // 2) ReportServer menu_save（校验登录 + 管理员）
    const viaWs = await saveViaReportServer(config);
    if (viaWs.ok) return viaWs;
    if (
      viaWs.reason === "unauthorized" ||
      viaWs.reason === "login required" ||
      viaWs.reason === "admin required" ||
      viaWs.reason === "forbidden"
    ) {
      return viaWs;
    }
    // 3) 决策 REST 兜底
    return saveViaDecisionRest(config);
  }

  /**
   * 优先写服务端，成功后再写 localStorage 缓存。
   * @returns {Promise<{ ok: boolean, reason?: string }>}
   */
  async function persistMenuConfig(config) {
    const normalized = normalizeMenuConfig(config);
    if (!isValidMenuConfig(normalized)) {
      return { ok: false, reason: "invalid" };
    }
    const server = await saveMenuConfigToServer(normalized);
    if (!server.ok) {
      return server;
    }
    cacheLocalMenuConfig(normalized);
    notifyMenuConfigSaved();
    return { ok: true };
  }

  function saveMenuConfig(config) {
    const normalized = normalizeMenuConfig(config);
    if (!isValidMenuConfig(normalized)) return false;
    return cacheLocalMenuConfig(normalized);
  }

  async function clearSavedMenuConfig() {
    const server = await saveMenuConfigToServer([]);
    try {
      global.localStorage.removeItem(STORAGE_KEY);
    } catch (e) {
      console.warn("clearSavedMenuConfig local failed", e);
    }
    notifyMenuConfigSaved();
    return server.ok;
  }

  global.HomepageMenuConfigApi = {
    STORAGE_KEY,
    MENU_CONFIG_SAVED_EVENT,
    DEFAULT_NAV_MENU_CONFIG: EMPTY_MENU_CONFIG,
    ICON_OPTIONS,
    SERVER_MENU_API_CANDIDATES,
    isSafeReportUrlString,
    isAllowedIconClass,
    normalizeIconClass,
    hasEffectiveSubmenu,
    isPrimaryClickable,
    isValidMenuConfig,
    normalizeMenuConfig,
    createMenuKey,
    cloneDefaultConfig,
    loadSavedMenuConfig,
    getEffectiveMenuConfig,
    hasSavedMenuConfig,
    saveMenuConfig,
    persistMenuConfig,
    fetchServerMenuConfig,
    clearSavedMenuConfig
  };
})(typeof window !== "undefined" ? window : globalThis);
