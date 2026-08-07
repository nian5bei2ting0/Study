(function () {
  const api = window.HomepageMenuConfigApi;
  const root = document.getElementById("configRoot");
  const msgEl = document.getElementById("configMsg");

  if (!api || !root) {
    console.error("HomepageMenuConfigApi 或配置容器缺失");
    return;
  }

  const ICON_OPTIONS = Array.isArray(api.ICON_OPTIONS) ? api.ICON_OPTIONS : [];
  const DEFAULT_ICON = ICON_OPTIONS[0]?.value || "fa-solid fa-folder";
  const URL_PLACEHOLDER =
    "/webroot/decision/view/report?viewlet=demo/basic/分组统计.cpt";

  /** @type {object[]} */
  let editConfig = [];

  function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
  }

  async function loadEditConfig() {
    if (typeof api.fetchServerMenuConfig === "function") {
      try {
        const serverCfg = await api.fetchServerMenuConfig();
        if (serverCfg && api.isValidMenuConfig(serverCfg)) {
          if (typeof api.saveMenuConfig === "function") {
            api.saveMenuConfig(serverCfg);
          }
          editConfig = deepClone(serverCfg);
          return;
        }
      } catch (e) {
        console.warn("[config] fetch server menu failed", e);
      }
    }
    editConfig = deepClone(api.getEffectiveMenuConfig());
  }

  function showMsg(text, ok = true) {
    if (!msgEl) return;
    msgEl.textContent = text;
    msgEl.classList.remove("hide", "success", "error");
    msgEl.classList.add(ok ? "success" : "error");
    msgEl.removeAttribute("aria-hidden");
    requestAnimationFrame(() => {
      msgEl.scrollIntoView({ behavior: "smooth", block: "nearest" });
    });
  }

  function hideMsg() {
    if (!msgEl) return;
    msgEl.classList.add("hide");
    msgEl.setAttribute("aria-hidden", "true");
  }

  function ensureUniqueKeys(items) {
    const seen = new Set();
    items.forEach((item) => {
      let k = (item.key || "").trim();
      if (!k) {
        k = api.createMenuKey ? api.createMenuKey() : `menu_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
      }
      while (seen.has(k)) {
        k = api.createMenuKey ? api.createMenuKey() : `${k}_${Math.random().toString(36).slice(2, 5)}`;
      }
      seen.add(k);
      item.key = k;
    });
  }

  function normalizeBeforeValidate(items) {
    return items.map((item) => ({
      key: typeof item.key === "string" ? item.key : "",
      iconClass:
        (typeof item.iconClass === "string" && item.iconClass.trim()) || DEFAULT_ICON,
      label: typeof item.label === "string" ? item.label : "",
      url: typeof item.url === "string" ? item.url : "",
      external: item.external === true,
      primaryClickable: item.primaryClickable === true,
      submenu: Array.isArray(item.submenu)
        ? item.submenu.map((s) => ({
            label: typeof s.label === "string" ? s.label : "",
            url: typeof s.url === "string" ? s.url : "",
            external: s.external === true
          }))
        : []
    }));
  }

  function syncEditConfigFromDom() {
    editConfig = collectFromDom();
  }

  function collectFromDom() {
    const sections = root.querySelectorAll("[data-config-primary]");
    const result = [];
    sections.forEach((sec) => {
      result.push({
        key: sec.querySelector(".inp-primary-key")?.value ?? "",
        label: sec.querySelector(".inp-primary-label")?.value ?? "",
        iconClass: sec.querySelector(".inp-primary-icon")?.value ?? "",
        url: sec.querySelector(".inp-primary-url")?.value ?? "",
        external: !!sec.querySelector(".inp-primary-external")?.checked,
        primaryClickable: !!sec.querySelector(".inp-primary-clickable")?.checked,
        submenu: []
      });
      const idx = result.length - 1;
      sec.querySelectorAll("[data-config-sub-row]").forEach((row) => {
        result[idx].submenu.push({
          label: row.querySelector(".inp-sub-label")?.value ?? "",
          url: row.querySelector(".inp-sub-url")?.value ?? "",
          external: !!row.querySelector(".inp-sub-external")?.checked
        });
      });
    });
    return normalizeBeforeValidate(result);
  }

  function render() {
    root.replaceChildren();

    editConfig.forEach((item, pi) => {
      const section = document.createElement("section");
      section.className = "config-primary-card";
      section.dataset.configPrimary = String(pi);

      const head = document.createElement("div");
      head.className = "config-primary-head";
      const h2 = document.createElement("h2");
      h2.textContent = `一级目录 ${pi + 1}`;
      const btnRemove = document.createElement("button");
      btnRemove.type = "button";
      btnRemove.className = "config-btn config-btn-danger";
      btnRemove.textContent = "删除本级";
      btnRemove.dataset.action = "remove-primary";
      btnRemove.dataset.index = String(pi);
      head.appendChild(h2);
      head.appendChild(btnRemove);
      section.appendChild(head);

      const grid = document.createElement("div");
      grid.className = "config-field-grid cols-2";

      section.appendChild(hiddenKeyField(item.key || ""));
      grid.appendChild(fieldBlock("一级目录名称", "inp-primary-label", item.label || "", "必填"));
      grid.appendChild(iconSelectField(item.iconClass || DEFAULT_ICON));
      grid.appendChild(
        urlFieldBlock(
          "一级报表 URL",
          "inp-primary-url",
          "inp-primary-external",
          item.url || "",
          item.external === true,
          "未勾选外链时须以 / 开头的站内路径；勾选后可填 http(s) 链接",
          URL_PLACEHOLDER
        )
      );
      grid.appendChild(primaryClickableField(item.primaryClickable === true));

      section.appendChild(grid);

      const subHead = document.createElement("div");
      subHead.className = "config-sub-head";
      subHead.textContent = "二级目录";
      section.appendChild(subHead);

      const wrap = document.createElement("div");
      wrap.className = "config-sub-table-wrap";
      const table = document.createElement("table");
      table.className = "config-sub-table";
      const thead = document.createElement("thead");
      thead.innerHTML =
        "<tr><th>二级名称</th><th>报表 URL</th><th class=\"col-actions\">操作</th></tr>";
      table.appendChild(thead);
      const tbody = document.createElement("tbody");

      const submenu = Array.isArray(item.submenu) ? item.submenu : [];
      submenu.forEach((sub, si) => {
        tbody.appendChild(
          subRow(pi, si, sub.label || "", sub.url || "", sub.external === true)
        );
      });

      table.appendChild(tbody);
      wrap.appendChild(table);
      section.appendChild(wrap);

      const btnAddSub = document.createElement("button");
      btnAddSub.type = "button";
      btnAddSub.className = "config-btn";
      btnAddSub.style.marginTop = "10px";
      btnAddSub.textContent = "添加二级目录";
      btnAddSub.dataset.action = "add-sub";
      btnAddSub.dataset.primaryIndex = String(pi);
      section.appendChild(btnAddSub);

      root.appendChild(section);
    });

    if (editConfig.length === 0) {
      const empty = document.createElement("p");
      empty.className = "config-page-desc";
      empty.textContent = "暂无一级目录，请点击「添加一级目录」开始配置。";
      root.appendChild(empty);
    }
  }

  function hiddenKeyField(value) {
    const wrap = document.createElement("div");
    wrap.className = "config-field config-field-hidden-key";
    const input = document.createElement("input");
    input.type = "hidden";
    input.className = "inp-primary-key";
    input.value = value;
    wrap.appendChild(input);
    return wrap;
  }

  function primaryClickableField(checked) {
    const wrap = document.createElement("div");
    wrap.className = "config-field config-field-checkbox";
    const label = document.createElement("label");
    label.className = "config-checkbox-label";
    const input = document.createElement("input");
    input.type = "checkbox";
    input.className = "inp-primary-clickable";
    input.checked = !!checked;
    const text = document.createElement("span");
    text.textContent = "允许点击一级目录跳转";
    label.appendChild(input);
    label.appendChild(text);
    wrap.appendChild(label);
    const hint = document.createElement("div");
    hint.className = "config-field-hint";
    hint.textContent =
      "仅一级时默认即可点击 URL；同时配置二级时，勾选后点击一级才跳转，未勾选则只展开二级且无小手光标。";
    wrap.appendChild(hint);
    return wrap;
  }

  function externalCheckbox(checked, className) {
    const label = document.createElement("label");
    label.className = "config-external-check";
    label.title = "勾选后允许填写 http/https 外链";
    const input = document.createElement("input");
    input.type = "checkbox";
    input.className = className;
    input.checked = !!checked;
    const text = document.createElement("span");
    text.textContent = "外链";
    label.appendChild(input);
    label.appendChild(text);
    return label;
  }

  function urlFieldBlock(labelText, urlClass, externalClass, value, externalChecked, hint, placeholder) {
    const wrap = document.createElement("div");
    wrap.className = "config-field";
    const label = document.createElement("label");
    label.textContent = labelText;
    wrap.appendChild(label);

    const row = document.createElement("div");
    row.className = "config-url-row";
    const input = document.createElement("input");
    input.type = "text";
    input.className = urlClass;
    input.value = value;
    input.autocomplete = "off";
    if (placeholder) {
      input.placeholder = placeholder;
    }
    row.appendChild(input);
    row.appendChild(externalCheckbox(externalChecked, externalClass));
    wrap.appendChild(row);

    if (hint) {
      const h = document.createElement("div");
      h.className = "config-field-hint";
      h.textContent = hint;
      wrap.appendChild(h);
    }
    return wrap;
  }

  function fieldBlock(labelText, className, value, hint, placeholder) {
    const wrap = document.createElement("div");
    wrap.className = "config-field";
    const label = document.createElement("label");
    label.textContent = labelText;
    const input = document.createElement("input");
    input.type = "text";
    input.className = className;
    input.value = value;
    input.autocomplete = "off";
    if (placeholder) {
      input.placeholder = placeholder;
    }
    wrap.appendChild(label);
    wrap.appendChild(input);
    if (hint) {
      const h = document.createElement("div");
      h.className = "config-field-hint";
      h.textContent = hint;
      wrap.appendChild(h);
    }
    return wrap;
  }

  function iconSelectField(currentValue) {
    const wrap = document.createElement("div");
    wrap.className = "config-field";
    const label = document.createElement("label");
    label.textContent = "图标";
    wrap.appendChild(label);

    const row = document.createElement("div");
    row.className = "config-icon-select-row";

    const preview = document.createElement("span");
    preview.className = "config-icon-preview";
    preview.setAttribute("aria-hidden", "true");
    const previewIcon = document.createElement("i");
    preview.appendChild(previewIcon);

    const select = document.createElement("select");
    select.className = "inp-primary-icon";
    select.setAttribute("aria-label", "图标类名");

    const values = ICON_OPTIONS.map((o) => o.value);
    let selected = currentValue;
    if (!values.includes(selected)) {
      const opt = document.createElement("option");
      opt.value = selected;
      opt.textContent = `自定义（${selected}）`;
      select.appendChild(opt);
    }

    ICON_OPTIONS.forEach((optDef) => {
      const opt = document.createElement("option");
      opt.value = optDef.value;
      opt.textContent = optDef.label;
      if (optDef.value === selected) {
        opt.selected = true;
      }
      select.appendChild(opt);
    });

    function syncPreview() {
      previewIcon.className = select.value || DEFAULT_ICON;
    }

    select.addEventListener("change", syncPreview);
    syncPreview();

    row.appendChild(preview);
    row.appendChild(select);
    wrap.appendChild(row);
    return wrap;
  }

  function subRow(pi, si, labelVal, urlVal, externalChecked) {
    const tr = document.createElement("tr");
    tr.dataset.configSubRow = "1";
    tr.dataset.primaryIndex = String(pi);
    tr.dataset.subIndex = String(si);

    const td1 = document.createElement("td");
    const in1 = document.createElement("input");
    in1.type = "text";
    in1.className = "inp-sub-label";
    in1.value = labelVal;
    td1.appendChild(in1);

    const td2 = document.createElement("td");
    const urlRow = document.createElement("div");
    urlRow.className = "config-url-row";
    const in2 = document.createElement("input");
    in2.type = "text";
    in2.className = "inp-sub-url";
    in2.value = urlVal;
    in2.placeholder = URL_PLACEHOLDER;
    in2.autocomplete = "off";
    urlRow.appendChild(in2);
    urlRow.appendChild(externalCheckbox(externalChecked, "inp-sub-external"));
    td2.appendChild(urlRow);

    const td3 = document.createElement("td");
    td3.className = "col-actions";
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "config-btn config-btn-danger";
    btn.textContent = "删除";
    btn.dataset.action = "remove-sub";
    btn.dataset.primaryIndex = String(pi);
    btn.dataset.subIndex = String(si);
    td3.appendChild(btn);

    tr.appendChild(td1);
    tr.appendChild(td2);
    tr.appendChild(td3);
    return tr;
  }

  root.addEventListener("click", (e) => {
    const t = e.target;
    if (!(t instanceof HTMLElement)) return;
    const action = t.dataset.action;
    if (!action) return;

    if (action === "remove-primary") {
      const idx = Number(t.dataset.index);
      if (!Number.isFinite(idx)) return;
      if (
        !confirm(
          "确定删除该一级目录吗？\n其下所有二级目录配置将一并删除，此操作在点击「保存配置」后才会写入服务器。"
        )
      ) {
        return;
      }
      syncEditConfigFromDom();
      editConfig.splice(idx, 1);
      render();
      return;
    }

    if (action === "add-sub") {
      const pi = Number(t.dataset.primaryIndex);
      if (!Number.isFinite(pi)) return;
      const section = root.querySelector(`[data-config-primary="${pi}"]`);
      const tbody = section?.querySelector(".config-sub-table tbody");
      if (!tbody) {
        syncEditConfigFromDom();
        if (!editConfig[pi]) return;
        if (!Array.isArray(editConfig[pi].submenu)) editConfig[pi].submenu = [];
        editConfig[pi].submenu.push({ label: "", url: "", external: false });
        render();
        return;
      }
      const si = tbody.querySelectorAll("[data-config-sub-row]").length;
      const row = subRow(pi, si, "", "", false);
      tbody.appendChild(row);
      row.querySelector(".inp-sub-label")?.focus();
      return;
    }

    if (action === "remove-sub") {
      const pi = Number(t.dataset.primaryIndex);
      const si = Number(t.dataset.subIndex);
      if (!Number.isFinite(pi) || !Number.isFinite(si)) return;
      if (!confirm("确定删除该二级目录行吗？")) return;
      const row = t.closest("[data-config-sub-row]");
      const tbody = row?.parentElement;
      if (row && tbody) {
        row.remove();
        const rows = tbody.querySelectorAll("[data-config-sub-row]");
        rows.forEach((r, idx) => {
          r.dataset.subIndex = String(idx);
          const btn = r.querySelector('[data-action="remove-sub"]');
          if (btn) {
            btn.dataset.subIndex = String(idx);
            btn.dataset.primaryIndex = String(pi);
          }
        });
        return;
      }
      syncEditConfigFromDom();
      if (!editConfig[pi]?.submenu) return;
      editConfig[pi].submenu.splice(si, 1);
      render();
    }
  });

  document.getElementById("btnAddPrimary")?.addEventListener("click", () => {
    syncEditConfigFromDom();
    editConfig.push({
      key: api.createMenuKey ? api.createMenuKey() : `menu_${Date.now()}`,
      iconClass: DEFAULT_ICON,
      label: "",
      url: "",
      external: false,
      primaryClickable: false,
      submenu: []
    });
    render();
    hideMsg();
  });

  document.getElementById("btnSave")?.addEventListener("click", async () => {
    hideMsg();
    let cfg = collectFromDom();
    cfg.forEach((item) => {
      if (!Array.isArray(item.submenu)) {
        item.submenu = [];
        return;
      }
      item.submenu = item.submenu.filter(
        (s) => (s.label || "").trim() !== "" || (s.url || "").trim() !== ""
      );
    });
    const missingLabel = cfg.find((item) => !(item.label || "").trim());
    if (missingLabel) {
      showMsg("请填写所有一级目录名称后再保存。", false);
      return;
    }
    ensureUniqueKeys(cfg);
    if (!api.isValidMenuConfig(cfg)) {
      showMsg(
        "校验失败：请检查名称、图标及报表 URL。未勾选「外链」时须为以 / 开头的站内路径；勾选后可填 http(s)。禁止 javascript:、data: 等。",
        false
      );
      return;
    }
    showMsg("正在保存到服务器…", true);
    const result =
      typeof api.persistMenuConfig === "function"
        ? await api.persistMenuConfig(cfg)
        : { ok: api.saveMenuConfig(cfg) };
    if (result && result.ok) {
      editConfig = deepClone(cfg);
      render();
      showMsg(
        cfg.length === 0
          ? "已保存空配置到服务器。所有用户刷新首页后将无菜单。"
          : "已保存到服务器。所有用户刷新首页后即可加载最新菜单。",
        true
      );
    } else {
      const reason = result && result.reason ? String(result.reason) : "error";
      let tip = "保存失败：" + reason;
      if (reason === "network" || reason === "no_dec" || reason === "dec_error") {
        tip = "保存失败：无法连接决策平台接口。请确认已登录管理系统，并重新部署/刷新插件后再试。";
      } else if (reason === "unauthorized" || reason === "login required") {
        tip = "保存失败：未检测到决策平台登录态，请先登录管理系统后再保存。";
      } else if (
        reason === "admin required" ||
        reason === "forbidden" ||
        /admin required/i.test(reason)
      ) {
        tip = "保存失败：仅管理员可修改菜单配置。";
      } else if (reason === "invalid") {
        tip = "保存失败：菜单数据校验未通过，请检查名称、图标与 URL。";
      }
      showMsg(tip, false);
    }
  });

  document.getElementById("btnReloadFromSaved")?.addEventListener("click", async () => {
    hideMsg();
    await loadEditConfig();
    render();
    showMsg("已从服务器重新载入当前表单。", true);
  });

  document.getElementById("btnClearConfig")?.addEventListener("click", async () => {
    if (!confirm("确定清空全部菜单配置吗？将同步清空服务器配置，所有用户生效。")) return;
    hideMsg();
    showMsg("正在清空服务器配置…", true);
    const ok =
      typeof api.clearSavedMenuConfig === "function"
        ? await api.clearSavedMenuConfig()
        : true;
    editConfig = [];
    render();
    showMsg(
      ok ? "已清空服务器与本地配置，首页刷新后将无菜单。" : "清空失败：请确认已登录管理系统。",
      !!ok
    );
  });

  void (async function initConfigPage() {
    await loadEditConfig();
    render();
  })();
})();
