/**
 * 报表地址默认「本站相对路径」；配置项勾选「外链」后允许 http(s)。
 * 可选全局钩子（在 main.js 之前设置）：
 * - window.__HOMEPAGE_REPORT_URL_RESOLVER__ = (url) => "完整报表URL" — 跨域决策平台时重写地址
 * - window.__HOMEPAGE_REPORT_LOAD_TIMEOUT_MS__ = 20000 — 报表加载超时（毫秒），默认 12000
 * - window.__HOMEPAGE_ALLOW_EXTERNAL_REPORT_URL__ = true — 全局允许外链（兼容旧用法）
 * 插件版已去除登录校验 / SSO / 管理员门禁；首页加载后默认打开第一个可用报表。
 */
function reportUrlPassesSafetyCheck(u, allowExternal) {
  const api = window.HomepageMenuConfigApi;
  if (api && typeof api.isSafeReportUrlString === "function") {
    return api.isSafeReportUrlString(u, allowExternal);
  }
  if (u == null || typeof u !== "string") return true;
  const t = u.trim();
  if (!t) return true;
  if (t.startsWith("//")) return false;
  if (t.startsWith("/")) return true;
  if (allowExternal === true || window.__HOMEPAGE_ALLOW_EXTERNAL_REPORT_URL__ === true) {
    try {
      const parsed = new URL(t, window.location.href);
      return parsed.protocol === "http:" || parsed.protocol === "https:";
    } catch (e) {
      return false;
    }
  }
  return false;
}

function normalizeReportUrl(url, allowExternal = false) {
  if (url == null || typeof url !== "string") return "";
  const trimmed = url.trim();
  if (!trimmed) return "";

  if (!reportUrlPassesSafetyCheck(trimmed, allowExternal)) {
    console.warn("[homepage] blocked unsafe report URL");
    return "";
  }

  if (typeof window.__HOMEPAGE_REPORT_URL_RESOLVER__ === "function") {
    try {
      const resolved = window.__HOMEPAGE_REPORT_URL_RESOLVER__(trimmed);
      if (typeof resolved === "string" && resolved.trim()) {
        const out = resolved.trim();
        if (!reportUrlPassesSafetyCheck(out, allowExternal)) {
          console.warn("[homepage] HOMEPAGE_REPORT_URL_RESOLVER returned unsafe URL");
          return "";
        }
        return out;
      }
    } catch (e) {
      console.warn("HOMEPAGE_REPORT_URL_RESOLVER failed", e);
    }
  }

  if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
    return trimmed;
  }

  if (
    allowExternal === true ||
    window.__HOMEPAGE_ALLOW_EXTERNAL_REPORT_URL__ === true
  ) {
    return trimmed;
  }
  console.warn("[homepage] blocked non-same-origin report URL");
  return "";
}

function getReportLoadTimeoutMs() {
  const raw = window.__HOMEPAGE_REPORT_LOAD_TIMEOUT_MS__;
  const n = typeof raw === "number" ? raw : 12000;
  return Number.isFinite(n) && n >= 0 ? n : 12000;
}

const MenuApi = window.HomepageMenuConfigApi;
if (!MenuApi) {
  console.error(
    "[homepage] menu-config-api.js 未加载或 HomepageMenuConfigApi 不可用，菜单将为空。请确认 ./js/menu-config-api.js 先于 main.js 引入。"
  );
}
let currentMenuConfig = MenuApi ? MenuApi.getEffectiveMenuConfig() : [];

const topHeader = document.getElementById("topHeader");
const menuList = document.getElementById("menuList");
const breadcrumbPath = document.getElementById("breadcrumbPath");
const reportFrame = document.getElementById("reportFrame");
const reportEmpty = document.getElementById("reportEmpty");
const reportLoading = document.getElementById("reportLoading");
const reportError = document.getElementById("reportError");
const retryReportBtn = document.getElementById("retryReportBtn");
const closeTimers = new WeakMap();
let menuItems = [];
let submenuLinks = [];
let activePath = { primaryKey: "", primary: "", secondary: "" };
let currentReportUrl = "";
let currentReportAllowExternal = false;
let reportLoadTimeoutId = null;
let loadingRequestId = 0;
let reportOverlayDismissTimer = 0;

function updateBreadcrumb(primary, secondary = "") {
  if (!breadcrumbPath) return;
  const textNode = breadcrumbPath.querySelector("span");
  if (!textNode) return;
  const p = (primary || "").trim();
  const s = (secondary || "").trim();
  if (!p) {
    textNode.textContent = "当前：未选择";
    return;
  }
  textNode.textContent = s ? `当前：${p} / ${s}` : `当前：${p}`;
}

function setActivePath(primaryKeyOrLabel, secondary = "", primaryLabelOpt) {
  // 兼容：setActivePath(key, secondary, label) 或旧调用 setActivePath(label, secondary)
  let primaryKey = "";
  let primaryLabel = "";
  if (arguments.length >= 3) {
    primaryKey = primaryKeyOrLabel || "";
    primaryLabel = primaryLabelOpt || "";
  } else if (primaryKeyOrLabel && currentMenuConfig.some((m) => m.key === primaryKeyOrLabel)) {
    primaryKey = primaryKeyOrLabel;
    primaryLabel = currentMenuConfig.find((m) => m.key === primaryKey)?.label || "";
  } else {
    primaryLabel = primaryKeyOrLabel || "";
    const hit = currentMenuConfig.find((m) => m.label === primaryLabel);
    primaryKey = hit?.key || "";
  }
  activePath = { primaryKey, primary: primaryLabel, secondary: secondary || "" };
  updateBreadcrumb(primaryLabel, secondary);
}

function clearReportFeedback() {
  reportLoading?.classList.add("hide");
  reportError?.classList.add("hide");
  if (currentReportUrl) {
    reportEmpty?.classList.add("hide");
  }
}

function cancelReportOverlayDismiss() {
  if (reportOverlayDismissTimer) {
    clearTimeout(reportOverlayDismissTimer);
    reportOverlayDismissTimer = 0;
  }
}

function onReportFrameLoad() {
  clearReportTimeout();
  clearReportFeedback();
}

function showReportLoading() {
  reportLoading?.classList.remove("hide");
  reportError?.classList.add("hide");
  reportEmpty?.classList.add("hide");
}

function showReportError() {
  reportLoading?.classList.add("hide");
  reportError?.classList.remove("hide");
}

function clearReportTimeout() {
  if (!reportLoadTimeoutId) return;
  clearTimeout(reportLoadTimeoutId);
  reportLoadTimeoutId = null;
}

function setReportUrl(url = "", allowExternal = false, forceReload = false) {
  if (!reportFrame || !reportEmpty) return;
  cancelReportOverlayDismiss();
  const safeUrl = normalizeReportUrl(
    typeof url === "string" ? url : "",
    allowExternal === true
  );
  const nextAllowExternal = allowExternal === true && !!safeUrl;

  // 同一地址不再写 iframe.src，避免切标签/同步菜单时整页重载
  if (
    !forceReload &&
    safeUrl &&
    safeUrl === currentReportUrl &&
    nextAllowExternal === currentReportAllowExternal &&
    reportFrame.getAttribute("src")
  ) {
    return;
  }

  currentReportUrl = safeUrl;
  currentReportAllowExternal = nextAllowExternal;
  clearReportTimeout();
  clearReportFeedback();

  if (!safeUrl) {
    reportFrame.removeAttribute("src");
    reportEmpty.classList.remove("hide");
    return;
  }

  const requestId = ++loadingRequestId;
  showReportLoading();
  reportFrame.src = safeUrl;

  // 某些跨域场景 iframe onerror 不稳定，使用超时兜底（时长见 __HOMEPAGE_REPORT_LOAD_TIMEOUT_MS__）
  reportLoadTimeoutId = setTimeout(() => {
    if (requestId !== loadingRequestId) return;
    showReportError();
  }, getReportLoadTimeoutMs());
}

function isMenuPrimaryClickable(menu) {
  if (MenuApi && typeof MenuApi.isPrimaryClickable === "function") {
    return MenuApi.isPrimaryClickable(menu);
  }
  const submenu = Array.isArray(menu?.submenu) ? menu.submenu : [];
  const hasSub = submenu.some(
    (s) =>
      (typeof s?.label === "string" && s.label.trim() !== "") ||
      (typeof s?.url === "string" && s.url.trim() !== "")
  );
  return !hasSub || menu?.primaryClickable === true;
}

function getFirstSubWithUrl(menu) {
  const submenu = Array.isArray(menu?.submenu) ? menu.submenu : [];
  for (const entry of submenu) {
    const url = typeof entry?.url === "string" ? entry.url.trim() : "";
    if (!url) continue;
    const label = typeof entry?.label === "string" ? entry.label.trim() : "";
    return {
      url,
      label: label || url,
      external: entry?.external === true
    };
  }
  return null;
}

/**
 * 默认打开策略：第一个一级目录。
 * - 一级可点且配置了 URL → 打开一级
 * - 一级未勾选可点（有二级）或一级无 URL → 打开该级下第一个有 URL 的二级
 */
function resolveDefaultOpenForMenu(menu) {
  if (!menu) return null;
  const primaryUrl = typeof menu.url === "string" ? menu.url.trim() : "";
  const firstSub = getFirstSubWithUrl(menu);
  const clickable = isMenuPrimaryClickable(menu);

  if (clickable && primaryUrl) {
    return {
      kind: "primary",
      url: primaryUrl,
      secondaryLabel: "",
      external: menu.external === true
    };
  }
  if (firstSub) {
    return {
      kind: "secondary",
      url: firstSub.url,
      secondaryLabel: firstSub.label,
      external: firstSub.external === true
    };
  }
  if (primaryUrl) {
    return {
      kind: "primary",
      url: primaryUrl,
      secondaryLabel: "",
      external: menu.external === true
    };
  }
  return null;
}

function openDefaultMenuEntry() {
  refreshMenuRefs();
  if (!menuItems.length || !Array.isArray(currentMenuConfig) || currentMenuConfig.length === 0) {
    setActivePath("", "", "");
    setReportUrl("");
    return false;
  }

  const menu = currentMenuConfig[0];
  const item =
    menuItems.find((el) => el.dataset.menu === menu.key) || menuItems[0] || null;
  const target = resolveDefaultOpenForMenu(menu);

  if (!item) {
    setActivePath("", "", "");
    setReportUrl("");
    return false;
  }

  if (!target) {
    clearMenuActiveState();
    item.classList.add("active");
    setActivePath(menu.key || item.dataset.menu || "", "", menu.label || "");
    setReportUrl("");
    return false;
  }

  if (target.kind === "secondary") {
    const links = Array.from(item.querySelectorAll(".submenu-card a"));
    const link =
      links.find((a) => (a.dataset.url || "").trim() === target.url) || links[0];
    if (link) {
      activateSubmenuItem(link, item);
      return true;
    }
  }

  activatePrimaryItem(item, true);
  return true;
}

function reapplyActiveOrOpenDefault() {
  refreshMenuRefs();
  const key = activePath.primaryKey;
  if (key) {
    const item = menuItems.find((el) => el.dataset.menu === key);
    const menu = getPrimaryMenuConfig(item);
    if (item && menu) {
      if (activePath.secondary) {
        const link = Array.from(item.querySelectorAll(".submenu-card a")).find(
          (node) => node.textContent?.trim() === activePath.secondary
        );
        if (link) {
          activateSubmenuItem(link, item);
          return true;
        }
      }
      const target = resolveDefaultOpenForMenu(menu);
      if (target?.kind === "primary") {
        activatePrimaryItem(item, true);
        return true;
      }
      if (target?.kind === "secondary") {
        const link =
          Array.from(item.querySelectorAll(".submenu-card a")).find(
            (a) => (a.dataset.url || "").trim() === target.url
          ) || item.querySelector(".submenu-card a");
        if (link) {
          activateSubmenuItem(link, item);
          return true;
        }
      }
    }
  }
  return openDefaultMenuEntry();
}

function createMenuItemElement(menu, isActive = false) {
  const li = document.createElement("li");
  const clickable = isMenuPrimaryClickable(menu);
  const submenuEntries = Array.isArray(menu.submenu) ? menu.submenu : [];
  const effectiveSubs = submenuEntries.filter((entry) => {
    const entryLabel = typeof entry?.label === "string" ? entry.label.trim() : "";
    const rawUrl = typeof entry?.url === "string" ? entry.url.trim() : "";
    return !!(entryLabel || rawUrl);
  });
  const hasSubmenu = effectiveSubs.length > 0;

  li.className = `menu-item${hasSubmenu ? " has-submenu" : ""}${
    clickable ? " primary-clickable" : " primary-hover-only"
  }${isActive ? " active" : ""}`;
  li.dataset.menu = menu.key;
  li.dataset.primaryClickable = clickable ? "1" : "0";
  li.dataset.hasSubmenu = hasSubmenu ? "1" : "0";
  li.dataset.external = menu.external === true ? "1" : "0";

  const button = document.createElement("button");
  button.className = "menu-button";
  button.type = "button";
  button.setAttribute("aria-expanded", "false");
  if (hasSubmenu) {
    button.setAttribute("aria-haspopup", "true");
  }

  const icon = document.createElement("i");
  icon.className =
    MenuApi && typeof MenuApi.normalizeIconClass === "function"
      ? MenuApi.normalizeIconClass(menu.iconClass)
      : menu.iconClass || "fa-solid fa-folder";
  button.appendChild(icon);

  const label = document.createElement("span");
  label.textContent = menu.label;
  button.appendChild(label);

  li.appendChild(button);

  if (hasSubmenu) {
    const submenu = document.createElement("div");
    submenu.className = "submenu-card";
    submenu.setAttribute("role", "menu");

    effectiveSubs.forEach((entry) => {
      const entryLabel = typeof entry?.label === "string" ? entry.label.trim() : "";
      const rawUrl = typeof entry?.url === "string" ? entry.url : "";
      const allowExternal = entry?.external === true;
      const link = document.createElement("a");
      const resolved = normalizeReportUrl(rawUrl, allowExternal);
      link.href = resolved || "#";
      link.setAttribute("role", "menuitem");
      link.textContent = entryLabel || rawUrl;
      link.dataset.url = rawUrl;
      link.dataset.external = allowExternal ? "1" : "0";
      submenu.appendChild(link);
    });

    li.appendChild(submenu);
  }

  return li;
}

function renderMenuByConfig(config, keepActive = false) {
  if (!menuList) return;
  menuList.replaceChildren();

  const activeKey = keepActive ? activePath.primaryKey : "";
  const activeSecondary = keepActive ? activePath.secondary : "";
  let activeMatched = false;

  config.forEach((menu) => {
    const isActive = keepActive ? menu.key === activeKey : false;
    if (isActive) activeMatched = true;
    const itemNode = createMenuItemElement(menu, isActive);
    if (isActive && activeSecondary) {
      const targetLink = Array.from(itemNode.querySelectorAll(".submenu-card a")).find(
        (node) => node.textContent?.trim() === activeSecondary
      );
      if (targetLink) targetLink.classList.add("submenu-link-active");
    }
    menuList.appendChild(itemNode);
  });

  if (!activeMatched) {
    setActivePath("", "", "");
  }
}

function refreshMenuRefs() {
  if (!menuList) {
    menuItems = [];
    submenuLinks = [];
    return;
  }
  menuItems = Array.from(menuList.querySelectorAll(".menu-item"));
  submenuLinks = Array.from(menuList.querySelectorAll(".submenu-card a"));
}

function menuItemHasSubmenu(item) {
  return !!(item && item.dataset.hasSubmenu === "1" && item.querySelector(".submenu-card a"));
}

function openSubmenu(item) {
  if (!menuItemHasSubmenu(item)) return;
  closeAllSubmenu();
  cancelClose(item);
  item.classList.add("submenu-open");
  const button = item.querySelector(".menu-button");
  if (button) button.setAttribute("aria-expanded", "true");
}

function scheduleClose(item, delay = 120) {
  const prev = closeTimers.get(item);
  if (prev) clearTimeout(prev);
  const timer = setTimeout(() => {
    item.classList.remove("submenu-open");
    const button = item.querySelector(".menu-button");
    if (button) button.setAttribute("aria-expanded", "false");
    closeTimers.delete(item);
  }, delay);
  closeTimers.set(item, timer);
}

function cancelClose(item) {
  const timer = closeTimers.get(item);
  if (!timer) return;
  clearTimeout(timer);
  closeTimers.delete(item);
}

function closeAllSubmenu() {
  menuItems.forEach((item) => {
    cancelClose(item);
    item.classList.remove("submenu-open");
    const button = item.querySelector(".menu-button");
    if (button) button.setAttribute("aria-expanded", "false");
  });
}

function clearMenuActiveState() {
  menuItems.forEach((menu) => menu.classList.remove("active"));
}

function getMenuItemFromTarget(target) {
  if (!(target instanceof Element)) return null;
  return target.closest(".menu-item");
}

function getPrimaryMenuConfig(item) {
  if (!item) return null;
  const menuKey = item.dataset.menu;
  if (!menuKey) return null;
  return currentMenuConfig.find((menu) => menu.key === menuKey) || null;
}

function activatePrimaryItem(item, navigate = true) {
  clearMenuActiveState();
  submenuLinks.forEach((node) => node.classList.remove("submenu-link-active"));
  item.classList.add("active");
  const parentLabel = item.querySelector(".menu-button span")?.textContent?.trim() || "";
  const parentMenu = getPrimaryMenuConfig(item);
  const parentUrl = typeof parentMenu?.url === "string" ? parentMenu.url : "";
  const allowExternal = parentMenu?.external === true;
  setActivePath(parentMenu?.key || item.dataset.menu || "", "", parentLabel);
  if (navigate) {
    setReportUrl(parentUrl, allowExternal);
  }
}

function activateSubmenuItem(link, parentItem) {
  submenuLinks.forEach((node) => node.classList.remove("submenu-link-active"));
  link.classList.add("submenu-link-active");
  clearMenuActiveState();
  parentItem.classList.add("active");
  const parentLabel = parentItem.querySelector(".menu-button span")?.textContent?.trim() || "";
  const parentMenu = getPrimaryMenuConfig(parentItem);
  setActivePath(
    parentMenu?.key || parentItem.dataset.menu || "",
    link.textContent?.trim() || "",
    parentLabel
  );
  setReportUrl(link.dataset.url || "", link.dataset.external === "1");
  closeAllSubmenu();
}

function bindDelegatedInteractions() {
  if (!menuList) return;

  menuList.addEventListener("mouseover", (event) => {
    const item = getMenuItemFromTarget(event.target);
    if (!item || !menuItemHasSubmenu(item)) return;
    if (!(event.target instanceof Element)) return;
    const related = event.relatedTarget;
    if (related instanceof Node && item.contains(related)) return;
    cancelClose(item);
    openSubmenu(item);
  });

  menuList.addEventListener("mouseout", (event) => {
    const item = getMenuItemFromTarget(event.target);
    if (!item || !menuItemHasSubmenu(item)) return;
    const related = event.relatedTarget;
    if (related instanceof Node && item.contains(related)) return;
    scheduleClose(item);
  });

  menuList.addEventListener("focusin", (event) => {
    const item = getMenuItemFromTarget(event.target);
    if (!item || !menuItemHasSubmenu(item)) return;
    openSubmenu(item);
  });

  menuList.addEventListener("click", (event) => {
    const target = event.target;
    if (!(target instanceof Element)) return;

    const link = target.closest(".submenu-card a");
    if (link) {
      event.preventDefault();
      const parentItem = link.closest(".menu-item");
      if (!parentItem) return;
      activateSubmenuItem(link, parentItem);
      return;
    }

    const button = target.closest(".menu-button");
    if (!button) return;
    event.preventDefault();
    const item = button.closest(".menu-item");
    if (!item) return;

    const submenuCount = item.querySelectorAll(".submenu-card a").length;
    const isOpen = item.classList.contains("submenu-open");
    const parentMenu = getPrimaryMenuConfig(item);
    const canNavigate = isMenuPrimaryClickable(parentMenu);

    // 仅一级，或勾选「允许点击一级」时才跳转；有二级且未勾选则只展开、不跳转
    activatePrimaryItem(item, canNavigate);

    // 无二级菜单时直接收起并结束
    if (submenuCount === 0) {
      closeAllSubmenu();
      return;
    }

    // 有二级菜单：切换展开状态
    if (isOpen) {
      scheduleClose(item, 0);
      return;
    }
    openSubmenu(item);
  });

  menuList.addEventListener("keydown", (event) => {
    const target = event.target;
    if (!(target instanceof Element)) return;

    if (event.key === "Escape") {
      closeAllSubmenu();
      const focusedItem = target.closest(".menu-item");
      focusedItem?.querySelector(".menu-button")?.focus();
      return;
    }

    if (event.key === "Enter") {
      const link = target.closest(".submenu-card a");
      if (!link) return;
      event.preventDefault();
      const parentItem = link.closest(".menu-item");
      if (!parentItem) return;
      activateSubmenuItem(link, parentItem);
    }
  });
}

// 配置热切换：传入新配置后实时重渲染（不写入 localStorage，持久化请使用菜单配置页）
function hotSwitchNavMenuConfig(nextConfig) {
  if (!MenuApi || !MenuApi.isValidMenuConfig(nextConfig)) {
    console.error("菜单配置无效，请检查 key/iconClass/label/submenu 字段。");
    return false;
  }

  closeAllSubmenu();
  currentMenuConfig = nextConfig;
  renderMenuByConfig(currentMenuConfig, false);
  refreshMenuRefs();
  openDefaultMenuEntry();
  return true;
}

document.addEventListener("click", (event) => {
  const target = event.target;
  if (!(target instanceof Node) || !menuList) return;
  if (menuList.contains(target)) return;
  closeAllSubmenu();
});

let compactHeaderScrollRaf = 0;
window.addEventListener(
  "scroll",
  () => {
    if (!topHeader) return;
    if (compactHeaderScrollRaf) return;
    compactHeaderScrollRaf = requestAnimationFrame(() => {
      compactHeaderScrollRaf = 0;
      if (window.scrollY > 40) topHeader.classList.add("compact");
      else topHeader.classList.remove("compact");
    });
  },
  { passive: true }
);

/** 帆软等场景下 load 后仍可能短暂未就绪，保留一次短延迟二次清除 loading，避免重复 rAF */
reportFrame?.addEventListener("load", () => {
  cancelReportOverlayDismiss();
  onReportFrameLoad();
  reportOverlayDismissTimer = setTimeout(() => {
    reportOverlayDismissTimer = 0;
    onReportFrameLoad();
  }, 120);
});

reportFrame?.addEventListener("error", () => {
  clearReportTimeout();
  showReportError();
});

retryReportBtn?.addEventListener("click", () => {
  if (!currentReportUrl) return;
  setReportUrl(currentReportUrl, currentReportAllowExternal, true);
});

function bootstrapHomepage() {
  renderMenuByConfig(currentMenuConfig);
  refreshMenuRefs();
  bindDelegatedInteractions();
  openDefaultMenuEntry();
}

function menuConfigSignature(cfg) {
  try {
    return JSON.stringify(cfg || []);
  } catch (e) {
    return "";
  }
}

async function reloadMenuFromServer(options = {}) {
  const openDefault = options.openDefault === true;
  const onlyIfChanged = options.onlyIfChanged === true;
  if (!MenuApi || typeof MenuApi.fetchServerMenuConfig !== "function") {
    if (openDefault) openDefaultMenuEntry();
    return;
  }
  try {
    const serverCfg = await MenuApi.fetchServerMenuConfig();
    if (!serverCfg || !MenuApi.isValidMenuConfig(serverCfg)) {
      if (openDefault) openDefaultMenuEntry();
      return;
    }
    if (
      onlyIfChanged &&
      menuConfigSignature(serverCfg) === menuConfigSignature(currentMenuConfig)
    ) {
      return;
    }
    if (typeof MenuApi.saveMenuConfig === "function") {
      MenuApi.saveMenuConfig(serverCfg);
    }
    currentMenuConfig = serverCfg;
    renderMenuByConfig(currentMenuConfig, !openDefault);
    refreshMenuRefs();
    if (openDefault) {
      openDefaultMenuEntry();
    } else {
      reapplyActiveOrOpenDefault();
    }
  } catch (e) {
    console.warn("[homepage] reload menu failed", e);
    if (openDefault) openDefaultMenuEntry();
  }
}

async function bootstrapHomepageAsync() {
  bootstrapHomepage();
  await reloadMenuFromServer({ openDefault: true });
}

void bootstrapHomepageAsync();

window.addEventListener("homepage:menu-config-saved", () => {
  if (!MenuApi) return;
  currentMenuConfig = MenuApi.getEffectiveMenuConfig();
  renderMenuByConfig(currentMenuConfig, false);
  refreshMenuRefs();
  openDefaultMenuEntry();
});

window.addEventListener("storage", (ev) => {
  if (!MenuApi || !ev || ev.key !== MenuApi.STORAGE_KEY) return;
  currentMenuConfig = MenuApi.getEffectiveMenuConfig();
  renderMenuByConfig(currentMenuConfig, false);
  refreshMenuRefs();
  openDefaultMenuEntry();
});

// 切回标签仅在菜单配置有变化时同步，避免 iframe 被重复加载
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "visible") {
    void reloadMenuFromServer({ openDefault: false, onlyIfChanged: true });
  }
});

window.hotSwitchNavMenuConfig = hotSwitchNavMenuConfig;

