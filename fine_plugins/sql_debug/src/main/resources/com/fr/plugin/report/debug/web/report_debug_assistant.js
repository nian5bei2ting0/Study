(function () {
    if (window.__FR_REPORT_DEBUG_ASSISTANT__) {
        return;
    }
    window.__FR_REPORT_DEBUG_ASSISTANT__ = true;

    var state = {
        snapshot: null,
        plainText: "",
        sqlBlocks: [],
        depsBlocks: [],
        fullscreen: false,
        apiMode: null,
        apiRestBase: null,
        apiReportServerBase: null
    };

    var SQL_KW = /\b(SELECT|FROM|WHERE|AND|OR|UNION|ALL|INSERT|UPDATE|DELETE|INTO|VALUES|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP|BY|ORDER|HAVING|LIMIT|AS|DISTINCT|CASE|WHEN|THEN|ELSE|END|DUAL)\b/gi;

    function getSessionId() {
        try {
            if (typeof FR !== "undefined" && FR.SessionMgr && FR.SessionMgr.getSessionID) {
                var sid = FR.SessionMgr.getSessionID();
                if (sid) {
                    return sid;
                }
            }
        } catch (e) {
        }
        var m = location.search.match(/sessionID=([^&]+)/i);
        return m ? decodeURIComponent(m[1]) : "";
    }

    function uniqueList(items) {
        var seen = {};
        var out = [];
        for (var i = 0; i < items.length; i++) {
            var item = items[i];
            if (!item || seen[item]) {
                continue;
            }
            seen[item] = true;
            out.push(item);
        }
        return out;
    }

    function resolveAppPrefix() {
        var path = window.location.pathname || "";
        var idx = path.indexOf("/decision/");
        if (idx >= 0) {
            return path.substring(0, idx);
        }
        idx = path.indexOf("/decision/view/report");
        if (idx >= 0) {
            return path.substring(0, idx);
        }
        try {
            if (typeof FR !== "undefined") {
                if (FR.contextPath) {
                    return String(FR.contextPath).replace(/\/$/, "");
                }
                if (FR.servletURL) {
                    var servlet = FR.servletURL;
                    idx = servlet.indexOf("/decision/");
                    if (idx >= 0) {
                        return servlet.substring(0, idx);
                    }
                }
            }
        } catch (e) {
        }
        return "";
    }

    function collectReportServerBases() {
        var bases = [];
        var prefix = resolveAppPrefix();
        var path = window.location.pathname || "";
        if (path.indexOf("ReportServer") >= 0) {
            bases.push(path.substring(0, path.indexOf("ReportServer") + "ReportServer".length));
        }
        bases.push(prefix + "/ReportServer");
        bases.push("/webroot/ReportServer");
        bases.push("/ReportServer");
        try {
            if (typeof FR !== "undefined" && FR.servletURL) {
                var servlet = FR.servletURL;
                if (servlet.indexOf("ReportServer") >= 0) {
                    bases.push(servlet.substring(0, servlet.indexOf("ReportServer") + "ReportServer".length));
                }
                if (servlet.indexOf("/decision/view/report") >= 0) {
                    var servletPrefix = servlet.substring(0, servlet.indexOf("/decision/view/report"));
                    bases.push(servletPrefix + "/ReportServer");
                    bases.push(servletPrefix + "/decision/ReportServer");
                }
                if (servlet.indexOf("/view/report") >= 0) {
                    bases.push(servlet.replace("/view/report", "/ReportServer"));
                }
            }
        } catch (e) {
        }
        if (path.indexOf("/decision/") >= 0) {
            var pathPrefix = path.substring(0, path.indexOf("/decision/"));
            bases.push(pathPrefix + "/ReportServer");
            bases.push(pathPrefix + "/decision/ReportServer");
        }
        return uniqueList(bases);
    }

    function collectDecisionRestBases() {
        var bases = [];
        var prefix = resolveAppPrefix();
        var path = window.location.pathname || "";
        bases.push(prefix + "/decision");
        bases.push("/webroot/decision");
        bases.push("/decision");
        if (path.indexOf("/decision/") >= 0) {
            bases.push(path.substring(0, path.indexOf("/decision/") + "/decision".length));
        }
        try {
            if (typeof FR !== "undefined" && FR.servletURL && FR.servletURL.indexOf("/decision/") >= 0) {
                bases.push(FR.servletURL.substring(0, FR.servletURL.indexOf("/decision/") + "/decision".length));
            }
        } catch (e) {
        }
        return uniqueList(bases);
    }

    function buildAccessCandidates() {
        var sessionId = getSessionId();
        if (!sessionId) {
            return [];
        }
        var sid = encodeURIComponent(sessionId);
        var urls = [];
        var i;
        var reportServerBases = collectReportServerBases();
        for (i = 0; i < reportServerBases.length; i++) {
            urls.push(reportServerBases[i] + "?op=report_debug&cmd=access&sessionID=" + sid);
        }
        var decisionBases = collectDecisionRestBases();
        for (i = 0; i < decisionBases.length; i++) {
            urls.push(decisionBases[i] + "/report/debug/assistant/preview/access?sessionID=" + sid);
        }
        return uniqueList(urls);
    }

    function rememberAccessEndpoint(url) {
        if (url.indexOf("/preview/access") >= 0) {
            state.apiMode = "decisionRest";
            state.apiRestBase = url.substring(0, url.indexOf("/preview/access"));
            return;
        }
        state.apiMode = "reportServer";
        state.apiReportServerBase = url.substring(0, url.indexOf("?"));
    }

    function buildSnapshotUrl(forceRefresh) {
        var sessionId = getSessionId();
        if (!sessionId) {
            return null;
        }
        var sid = encodeURIComponent(sessionId);
        var title = encodeURIComponent(document.title || "");
        var refresh = forceRefresh ? "1" : "0";
        var diag = shouldIncludeDiagnostics() ? "&diag=1" : "";
        if (state.apiMode === "decisionRest" && state.apiRestBase) {
            return state.apiRestBase
                + "/preview/snapshot?sessionID=" + sid
                + "&refresh=" + refresh
                + "&pageTitle=" + title
                + diag;
        }
        var reportServer = state.apiReportServerBase;
        if (!reportServer) {
            var bases = collectReportServerBases();
            reportServer = bases.length > 0 ? bases[0] : "/webroot/ReportServer";
        }
        return reportServer
            + "?op=report_debug&cmd=snapshot&refresh=" + refresh
            + diag
            + "&sessionID=" + sid
            + "&pageTitle=" + title;
    }

    function shouldIncludeDiagnostics() {
        var search = location.search || "";
        return /(?:^|[?&])report_debug_diag=(?:1|true)(?:&|$)/i.test(search)
            || /(?:^|[?&])diag=(?:1|true)(?:&|$)/i.test(search);
    }

    function buildUrl(forceRefresh) {
        return buildSnapshotUrl(!!forceRefresh);
    }

    function checkAccess(callback) {
        function tryUrls(urls, idx) {
            if (idx >= urls.length) {
                callback(false);
                return;
            }
            var xhr = new XMLHttpRequest();
            xhr.open("GET", urls[idx], true);
            xhr.withCredentials = true;
            xhr.onreadystatechange = function () {
                if (xhr.readyState !== 4) {
                    return;
                }
                try {
                    var resp = JSON.parse(xhr.responseText || "{}");
                    if (resp.success && resp.data && resp.data.allowed !== undefined) {
                        rememberAccessEndpoint(urls[idx]);
                        callback(!!resp.data.allowed);
                        return;
                    }
                } catch (e) {
                }
                tryUrls(urls, idx + 1);
            };
            xhr.send();
        }

        function attempt(remaining) {
            var urls = buildAccessCandidates();
            if (!urls.length) {
                if (remaining > 0) {
                    setTimeout(function () {
                        attempt(remaining - 1);
                    }, 500);
                    return;
                }
                callback(false);
                return;
            }
            tryUrls(urls, 0);
        }

        attempt(8);
    }

    function formatParamLine(p) {
        var pv = p.value;
        if (pv === undefined || pv === null || pv === "") {
            pv = p.original;
        }
        if (pv === undefined || pv === null || pv === "") {
            pv = "(\u672a\u8bbe\u7f6e)";
        }
        return p.name + ":" + pv;
    }

    function escapeHtml(s) {
        return String(s)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    function highlightSql(sql) {
        if (!sql) {
            return "";
        }
        return escapeHtml(sql).replace(SQL_KW, function (m) {
            return '<span class="sql-kw">' + m + '</span>';
        });
    }

    function dashLine(len) {
        var s = "";
        for (var i = 0; i < len; i++) {
            s += "-";
        }
        return s;
    }

    function shouldShowAbsolutePath(data) {
        return !!(data && (data.displayAbsolutePath === true || data.displayAbsolutePath === "true"));
    }

    function formatDependentTables(ds) {
        if (!ds || !ds.dependentTables) {
            return [];
        }
        var list = ds.dependentTables;
        if (typeof list === "string") {
            return list ? [list] : [];
        }
        var out = [];
        for (var i = 0; i < list.length; i++) {
            if (list[i]) {
                out.push(String(list[i]));
            }
        }
        return out;
    }

    function renderDependentTablesBlock(tables, depsIdx) {
        var countLabel = "(" + tables.length + ")";
        var html = [];
        html.push('<div class="rda-deps-wrap rda-deps-collapsed">');
        html.push('<div class="rda-deps-toolbar">');
        html.push('<button type="button" class="rda-deps-toggle" aria-expanded="false">');
        html.push('<span class="rda-deps-toggle-icon" aria-hidden="true">\u25b6</span>');
        html.push('<span class="rda-deps-toolbar-label">\u6570\u636e\u96c6\u4f9d\u8d56</span>');
        html.push('<span class="rda-deps-count">' + escapeHtml(countLabel) + '</span>');
        html.push('</button>');
        html.push('<button type="button" class="rda-deps-copy-btn" data-deps-idx="' + depsIdx + '">\u590d\u5236</button>');
        html.push('</div>');
        html.push('<div class="rda-deps-block">');
        if (!tables.length) {
            html.push('<span class="rda-deps-empty">\u672a\u8bc6\u522b\u5230\u8868\u540d</span>');
        } else {
            for (var i = 0; i < tables.length; i++) {
                html.push('<span class="rda-deps-tag">' + escapeHtml(tables[i]) + '</span>');
            }
        }
        html.push('</div>');
        html.push('</div>');
        return html.join("");
    }

    function formatPlainText(data) {
        if (!data) {
            return "";
        }
        var lines = [];
        lines.push("--" + (data.reportName || ""));
        lines.push("--\u6a21\u677f\u76f8\u5bf9\u8def\u5f84:" + (data.relativePath || ""));
        if (shouldShowAbsolutePath(data)) {
            lines.push("--\u6a21\u677f\u7edd\u5bf9\u8def\u5f84:" + (data.absolutePath || ""));
        }

        var list = data.datasets || [];
        for (var i = 0; i < list.length; i++) {
            var ds = list[i];
            lines.push("");
            lines.push("--" + (ds.index || i + 1) + ".\u6570\u636e\u96c6" + dashLine(24));
            lines.push("----\u540d\u79f0:" + (ds.name || ""));
            lines.push("----\u7c7b\u578b:" + (ds.typeLabel || ""));
            lines.push("----\u8fd0\u884c\u72b6\u6001:" + (ds.runStatusLabel || ""));
            var timeLine = "----sql\u6267\u884c\u8017\u8d39\u65f6\u95f4:";
            if (ds.sqlTimeMs != null && ds.sqlTimeMs !== undefined) {
                timeLine += ds.sqlTimeMs + "ms";
            }
            lines.push(timeLine);
            lines.push("----\u53c2\u6570:");
            var params = ds.parameters || [];
            for (var j = 0; j < params.length; j++) {
                lines.push("------" + formatParamLine(params[j]));
            }
            if (ds.showSql) {
                var sqlLabel = ds.sqlResolved !== false ? "----\u67e5\u8be2SQL:" : "----\u67e5\u8be2SQL(\u6a21\u677f\uff0c\u672a\u6267\u884c):";
                lines.push(sqlLabel);
                lines.push(ds.querySql || "");
                if (ds.showDependencies !== false) {
                    lines.push("----\u6570\u636e\u96c6\u4f9d\u8d56:");
                    var depTables = formatDependentTables(ds);
                    if (!depTables.length) {
                        lines.push("------\u672a\u8bc6\u522b\u5230\u8868\u540d");
                    } else {
                        for (var k = 0; k < depTables.length; k++) {
                            lines.push("------" + depTables[k]);
                        }
                    }
                }
            }
        }
        return lines.join("\n");
    }

    function renderHtml(data) {
        state.sqlBlocks = [];
        state.depsBlocks = [];
        var html = [];
        html.push('<div class="rda-line rda-report-name">--' + escapeHtml(data.reportName || "") + '</div>');
        html.push('<div class="rda-line rda-meta">--\u6a21\u677f\u76f8\u5bf9\u8def\u5f84:' + escapeHtml(data.relativePath || "") + '</div>');
        if (shouldShowAbsolutePath(data)) {
            html.push('<div class="rda-line rda-meta">--\u6a21\u677f\u7edd\u5bf9\u8def\u5f84:' + escapeHtml(data.absolutePath || "") + '</div>');
        }

        var list = data.datasets || [];
        for (var i = 0; i < list.length; i++) {
            var ds = list[i];
            html.push('<div class="rda-line rda-section-title">--' + (ds.index || i + 1) + '.\u6570\u636e\u96c6' + dashLine(24) + '</div>');
            html.push('<div class="rda-line">----\u540d\u79f0:' + escapeHtml(ds.name || "") + '</div>');
            html.push('<div class="rda-line">----\u7c7b\u578b:' + escapeHtml(ds.typeLabel || "") + '</div>');
            html.push('<div class="rda-line">----\u8fd0\u884c\u72b6\u6001:' + escapeHtml(ds.runStatusLabel || "") + '</div>');
            var timeText = "----sql\u6267\u884c\u8017\u8d39\u65f6\u95f4:";
            if (ds.sqlTimeMs != null && ds.sqlTimeMs !== undefined) {
                timeText += ds.sqlTimeMs + "ms";
            }
            html.push('<div class="rda-line">' + escapeHtml(timeText) + '</div>');
            html.push('<div class="rda-line">----\u53c2\u6570:</div>');
            var params = ds.parameters || [];
            for (var j = 0; j < params.length; j++) {
                html.push('<div class="rda-line rda-param">------' + escapeHtml(formatParamLine(params[j])) + '</div>');
            }
            if (ds.showSql) {
                var sqlPlain = ds.querySql || "";
                var sqlIdx = state.sqlBlocks.length;
                state.sqlBlocks.push(sqlPlain);
                var sqlLabel = ds.sqlResolved !== false
                    ? "\u67e5\u8be2SQL"
                    : "\u67e5\u8be2SQL(\u6a21\u677f\uff0c\u672a\u6267\u884c)";
                html.push('<div class="rda-sql-wrap rda-sql-collapsed">');
                html.push('<div class="rda-sql-toolbar">');
                html.push('<button type="button" class="rda-sql-toggle" aria-expanded="false">');
                html.push('<span class="rda-sql-toggle-icon" aria-hidden="true">\u25b6</span>');
                html.push('<span class="rda-sql-toolbar-label">' + escapeHtml(sqlLabel) + '</span>');
                if (ds.sqlMasked) {
                    html.push('<span class="rda-sql-mask-hint">\u8131\u654f\u5df2\u5f00\u542f</span>');
                }
                html.push('</button>');
                html.push('<button type="button" class="rda-sql-copy-btn" data-sql-idx="' + sqlIdx + '">\u590d\u5236</button>');
                html.push('</div>');
                html.push('<div class="rda-sql-block">' + highlightSql(sqlPlain) + '</div>');
                html.push('</div>');
                if (ds.showDependencies !== false) {
                    var depTables = formatDependentTables(ds);
                    var depsIdx = state.depsBlocks.length;
                    state.depsBlocks.push(depTables.length ? depTables.join("\n") : "");
                    html.push(renderDependentTablesBlock(depTables, depsIdx));
                }
            }
        }
        return html.join("");
    }

    function setBodyContent(html, isError) {
        var body = document.getElementById("fr-rda-body");
        if (!body) {
            return;
        }
        if (isError) {
            body.innerHTML = '<div class="rda-error">' + escapeHtml(html) + '</div>';
        } else {
            body.innerHTML = html;
        }
    }

    function openDialog() {
        document.getElementById("fr-report-debug-mask").classList.add("fr-rda-visible");
        document.getElementById("fr-report-debug-dialog").classList.add("fr-rda-visible");
        loadSnapshot();
    }

    function closeDialog() {
        document.getElementById("fr-report-debug-mask").classList.remove("fr-rda-visible");
        document.getElementById("fr-report-debug-dialog").classList.remove("fr-rda-visible");
        document.getElementById("fr-report-debug-dialog").classList.remove("fr-rda-fullscreen");
        state.fullscreen = false;
    }

    function toggleFullscreen() {
        state.fullscreen = !state.fullscreen;
        var dlg = document.getElementById("fr-report-debug-dialog");
        if (state.fullscreen) {
            dlg.classList.add("fr-rda-fullscreen");
        } else {
            dlg.classList.remove("fr-rda-fullscreen");
        }
    }

    function loadSnapshot(forceRefresh) {
        setBodyContent("\u6b63\u5728\u52a0\u8f7d\u5feb\u7167...", false);

        var url = buildUrl(!!forceRefresh);
        if (!url) {
            setBodyContent("\u672a\u83b7\u53d6\u5230 sessionID\uff0c\u8bf7\u5148\u6253\u5f00\u62a5\u8868\u9884\u89c8", true);
            return;
        }

        var xhr = new XMLHttpRequest();
        xhr.open("GET", url, true);
        xhr.withCredentials = true;
        try {
            xhr.overrideMimeType("application/json;charset=UTF-8");
        } catch (e) {
        }
        xhr.onreadystatechange = function () {
            if (xhr.readyState !== 4) {
                return;
            }
            var raw = xhr.responseText || "";
            try {
                var resp = JSON.parse(raw);
                if (resp.success && resp.data) {
                    state.snapshot = resp.data;
                    state.plainText = formatPlainText(resp.data);
                    setBodyContent(renderHtml(resp.data), false);
                    return;
                }
                var errMsg = resp.errorMsg || resp.errorCode || "\u52a0\u8f7d\u5931\u8d25";
                if (resp.errorCode === "rate_limited") {
                    errMsg = resp.errorMsg || "\u5feb\u7167\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5";
                }
                setBodyContent(errMsg, true);
            } catch (e) {
                setBodyContent("\u54cd\u5e94\u89e3\u6790\u5931\u8d25 (HTTP " + xhr.status + ")", true);
            }
        };
        xhr.send();
    }

    function exportText() {
        if (!state.plainText) {
            alert("\u6682\u65e0\u53ef\u5bfc\u51fa\u5185\u5bb9");
            return;
        }
        var name = (state.snapshot && state.snapshot.reportName) ? state.snapshot.reportName : "report";
        name = name.replace(/[\\/:*?"<>|]/g, "_");
        var blob = new Blob([state.plainText], {type: "text/plain;charset=utf-8"});
        var a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = name + "_debug.txt";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(a.href);
    }

    function copyPlainText(text, done) {
        if (!text) {
            alert("\u6682\u65e0\u53ef\u590d\u5236\u5185\u5bb9");
            return;
        }
        function onSuccess() {
            if (typeof done === "function") {
                done();
            } else {
                alert("\u5df2\u590d\u5236\u5230\u526a\u8d34\u677f");
            }
        }
        function fallback() {
            var ta = document.createElement("textarea");
            ta.value = text;
            ta.style.position = "fixed";
            ta.style.left = "-9999px";
            document.body.appendChild(ta);
            ta.select();
            try {
                document.execCommand("copy");
                onSuccess();
            } catch (e) {
                alert("\u590d\u5236\u5931\u8d25\uff0c\u8bf7\u624b\u52a8\u590d\u5236");
            }
            document.body.removeChild(ta);
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(onSuccess).catch(fallback);
        } else {
            fallback();
        }
    }

    function copyText() {
        copyPlainText(state.plainText);
    }

    function copyDatasetSql(btn) {
        var idx = parseInt(btn.getAttribute("data-sql-idx"), 10);
        if (isNaN(idx) || !state.sqlBlocks[idx]) {
            return;
        }
        copyPlainText(state.sqlBlocks[idx], function () {
            btn.classList.add("rda-copied");
            btn.textContent = "\u5df2\u590d\u5236";
            setTimeout(function () {
                btn.classList.remove("rda-copied");
                btn.textContent = "\u590d\u5236";
            }, 1500);
        });
    }

    function copyDependentTables(btn) {
        var idx = parseInt(btn.getAttribute("data-deps-idx"), 10);
        if (isNaN(idx) || state.depsBlocks[idx] === undefined) {
            return;
        }
        var text = state.depsBlocks[idx];
        if (!text) {
            alert("\u6682\u65e0\u53ef\u590d\u5236\u7684\u4f9d\u8d56\u8868");
            return;
        }
        copyPlainText(text, function () {
            btn.classList.add("rda-copied");
            btn.textContent = "\u5df2\u590d\u5236";
            setTimeout(function () {
                btn.classList.remove("rda-copied");
                btn.textContent = "\u590d\u5236";
            }, 1500);
        });
    }

    function toggleSqlSection(toggleBtn) {
        var wrap = toggleBtn.closest(".rda-sql-wrap");
        if (!wrap) {
            return;
        }
        var expanded = !wrap.classList.contains("rda-sql-expanded");
        wrap.classList.toggle("rda-sql-expanded", expanded);
        wrap.classList.toggle("rda-sql-collapsed", !expanded);
        toggleBtn.setAttribute("aria-expanded", expanded ? "true" : "false");
    }

    function toggleDepsSection(toggleBtn) {
        var wrap = toggleBtn.closest(".rda-deps-wrap");
        if (!wrap) {
            return;
        }
        var expanded = !wrap.classList.contains("rda-deps-expanded");
        wrap.classList.toggle("rda-deps-expanded", expanded);
        wrap.classList.toggle("rda-deps-collapsed", !expanded);
        toggleBtn.setAttribute("aria-expanded", expanded ? "true" : "false");
    }

    function bindSqlCopyButtons() {
        var body = document.getElementById("fr-rda-body");
        if (!body || body.__rdaSqlCopyBound) {
            return;
        }
        body.__rdaSqlCopyBound = true;
        body.addEventListener("click", function (e) {
            var sqlBtn = e.target.closest(".rda-sql-copy-btn");
            if (sqlBtn) {
                e.preventDefault();
                e.stopPropagation();
                copyDatasetSql(sqlBtn);
                return;
            }
            var sqlToggle = e.target.closest(".rda-sql-toggle");
            if (sqlToggle) {
                e.preventDefault();
                toggleSqlSection(sqlToggle);
                return;
            }
            var depsCopyBtn = e.target.closest(".rda-deps-copy-btn");
            if (depsCopyBtn) {
                e.preventDefault();
                e.stopPropagation();
                copyDependentTables(depsCopyBtn);
                return;
            }
            var depsToggle = e.target.closest(".rda-deps-toggle");
            if (depsToggle) {
                e.preventDefault();
                toggleDepsSection(depsToggle);
            }
        });
    }

    function createUi() {
        var mask = document.createElement("div");
        mask.id = "fr-report-debug-mask";
        mask.onclick = closeDialog;

        var dialog = document.createElement("div");
        dialog.id = "fr-report-debug-dialog";
        dialog.innerHTML =
            '<div id="fr-rda-header">' +
            '  <span id="fr-rda-title">\u62a5\u8868\u8c03\u8bd5\u52a9\u624b</span>' +
            '  <div id="fr-rda-header-actions">' +
            '    <button type="button" id="fr-rda-btn-refresh" title="\u5237\u65b0\u5feb\u7167">\u21bb</button>' +
            '    <button type="button" id="fr-rda-btn-full" title="\u5168\u5c4f">\u2922</button>' +
            '    <button type="button" id="fr-rda-btn-close" title="\u5173\u95ed">\u2715</button>' +
            '  </div>' +
            '</div>' +
            '<div id="fr-rda-body"></div>' +
            '<div id="fr-rda-footer">' +
            '  <button type="button" id="fr-rda-btn-export">\u5bfc\u51fa</button>' +
            '  <button type="button" id="fr-rda-btn-copy">\u590d\u5236</button>' +
            '</div>';

        var ball = document.createElement("div");
        ball.id = "fr-report-debug-ball";
        ball.title = "\u6570\u636e\u96c6\u8c03\u8bd5\u52a9\u624b";
        ball.textContent = "SQL";
        ball.onclick = openDialog;

        document.body.appendChild(mask);
        document.body.appendChild(dialog);
        document.body.appendChild(ball);

        document.getElementById("fr-rda-btn-close").onclick = function (e) {
            e.stopPropagation();
            closeDialog();
        };
        document.getElementById("fr-rda-btn-refresh").onclick = function (e) {
            e.stopPropagation();
            loadSnapshot(true);
        };
        document.getElementById("fr-rda-btn-full").onclick = function (e) {
            e.stopPropagation();
            toggleFullscreen();
        };
        dialog.onclick = function (e) {
            e.stopPropagation();
        };
        document.getElementById("fr-rda-btn-export").onclick = exportText;
        document.getElementById("fr-rda-btn-copy").onclick = copyText;
        bindSqlCopyButtons();
    }

    function initAssistant() {
        checkAccess(function (allowed) {
            if (allowed) {
                createUi();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAssistant);
    } else {
        initAssistant();
    }
})();
