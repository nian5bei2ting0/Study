!(function () {
    window.__REPORT_DEBUG_ASSISTANT_BUILD__ = "20260713-01-p1";
    var DEFAULT_AUTH_USERNAME = "admin";
    var CONFIG_GET = "/report/debug/assistant/config/get";
    var CONFIG_SAVE = "/report/debug/assistant/config/save";
    var authUserSelection = [];
    var savedDisplayByUsername = {};

    function normalizeGet(e) {
        if (!e) {
            return null;
        }
        if (e.status === "success") {
            return e;
        }
        if (e.data) {
            if (e.data.status === "success") {
                return e.data;
            }
            return BI.extend({status: "success"}, e.data);
        }
        if (e.enabled !== undefined || e.authorizedUsers !== undefined || e.authorizedUsernames || e.authorizedUsersText) {
            return BI.extend({status: "success"}, e);
        }
        return e;
    }

    function toUserList(users) {
        if (!users) {
            return [];
        }
        if (BI.isString(users)) {
            return users.split(/[\r\n;,，]+/);
        }
        if (BI.isArray(users)) {
            return users;
        }
        if (typeof users === "object") {
            var keys = Object.keys(users);
            var numKeys = BI.filter(keys, function (k) {
                return /^\d+$/.test(k);
            });
            if (numKeys.length) {
                numKeys.sort(function (a, b) {
                    return parseInt(a, 10) - parseInt(b, 10);
                });
                return BI.map(numKeys, function (k) {
                    return users[k];
                });
            }
        }
        return [];
    }

    function authorizedUsersFromResponse(e) {
        if (!e) {
            return [];
        }
        var list = toUserList(e.authorizedUsersText);
        if (!list.length) {
            list = toUserList(e.authorizedUsers);
        }
        if (!list.length) {
            list = toUserList(e.authorizedUsernames);
        }
        return uniqueValues(BI.compact(BI.map(list, function (i, v) {
            return normalizeAuthorizedDisplayValue(v);
        })));
    }

    function normalizeAuthorizedDisplayValue(v) {
        if (v == null) {
            return null;
        }
        var s = String(v).trim();
        if (!s || isBareIndexTokens([s])) {
            return null;
        }
        if (s.indexOf("|") >= 0) {
            var parts = s.split("|", 2);
            if (parts.length > 1 && String(parts[1]).trim()) {
                return String(parts[1]).trim();
            }
            s = String(parts[0]).trim();
        }
        if (s.indexOf("(") > 0 && s.lastIndexOf(")") > s.indexOf("(")) {
            return s;
        }
        var token = parseUserToken(s);
        if (token.username) {
            return token.display.indexOf("(") > 0 ? token.display : (token.username + "(" + token.username + ")");
        }
        return s;
    }

    function extractUsernameFromDisplay(v) {
        if (v == null) {
            return null;
        }
        var s = String(v).trim();
        if (!s || isBareIndexTokens([s])) {
            return null;
        }
        var token = parseUserToken(s);
        return token.username || s;
    }

    function displayValuesToUsernames(displayValues) {
        return uniqueValues(BI.compact(BI.map(displayValues, function (i, v) {
            return extractUsernameFromDisplay(v);
        })));
    }

    function toPlatformDisplayValuesFromUsernames(usernames) {
        usernames = BI.compact(BI.map(toUserList(usernames), function (i, un) {
            return extractUsernameFromDisplay(un);
        }));
        if (!usernames.length) {
            return [];
        }
        return uniqueValues(BI.compact(BI.map(usernames, function (i, un) {
            var found = BI.find(authUserStore.resultItems, function (j, it) {
                return it && it.username === un;
            });
            if (found && found.value) {
                return found.value;
            }
            if (savedDisplayByUsername[un]) {
                return savedDisplayByUsername[un];
            }
            return un + "(" + un + ")";
        })));
    }

    function parseUserToken(token) {
        var s = String(token || "").trim();
        if (!s) {
            return {username: "", display: s};
        }
        if (s.indexOf("|") >= 0) {
            var pipeParts = s.split("|", 2);
            var pipeDisplay = pipeParts.length > 1 ? String(pipeParts[1]).trim() : s;
            var pipeUser = extractUsernameFromDisplay(pipeDisplay) || String(pipeParts[0]).trim();
            return {username: pipeUser, display: pipeDisplay};
        }
        var left = Math.max(s.lastIndexOf("("), s.lastIndexOf("\uFF08"));
        var right = Math.max(s.lastIndexOf(")"), s.lastIndexOf("\uFF09"));
        if (left > 0 && right > left) {
            var open = s.charAt(left);
            var close = open === "\uFF08" ? "\uFF09" : ")";
            if (s.charAt(right) === close) {
                return {username: s.substring(left + 1, right).trim(), display: s};
            }
        }
        return {username: s, display: s};
    }

    function displayLabelName(display) {
        var token = parseUserToken(display);
        if (!token.display || token.display.indexOf("(") < 0) {
            return "";
        }
        var left = Math.max(token.display.lastIndexOf("("), token.display.lastIndexOf("\uFF08"));
        if (left <= 0) {
            return "";
        }
        return token.display.substring(0, left).trim();
    }

    function isFallbackSelfDisplay(display) {
        var token = parseUserToken(display);
        if (!token.username || !token.display || token.display.indexOf("(") < 0) {
            return true;
        }
        var label = displayLabelName(token.display);
        return !label || label === token.username;
    }

    function preferAuthUserDisplay(username, candidates) {
        var best = null;
        BI.each(candidates, function (i, c) {
            if (!c) {
                return;
            }
            var token = parseUserToken(c);
            if (token.username !== username) {
                return;
            }
            var display = token.display.indexOf("(") > 0 ? token.display : (token.username + "(" + token.username + ")");
            if (!best) {
                best = display;
                return;
            }
            if (isFallbackSelfDisplay(best) && !isFallbackSelfDisplay(display)) {
                best = display;
            }
        });
        return best || (username + "(" + username + ")");
    }

    function rememberSavedDisplays(values) {
        BI.each(platformUserValues(values), function (i, v) {
            var token = parseUserToken(v);
            if (!token.username) {
                return;
            }
            var display = token.display.indexOf("(") > 0 ? token.display : (token.username + "(" + token.username + ")");
            var existing = savedDisplayByUsername[token.username];
            if (!existing || (isFallbackSelfDisplay(existing) && !isFallbackSelfDisplay(display))) {
                savedDisplayByUsername[token.username] = display;
            }
        });
    }

    function formatPlatformItem(user, keyword) {
        var username = String(user.username || "").trim();
        var label = String(user.realName || user.username || username).trim();
        var display = label + "(" + username + ")";
        return BI.extend({}, user, {
            value: display,
            text: display,
            title: display,
            keyword: keyword || "",
            selected: false
        });
    }

    var authUserStore = {
        resultItems: [],
        allItems: [],
        lastPopupItems: [],
        lastSearchValue: "",
        mergeValues: function (values) {
            var items = this.resultItems ? this.resultItems.slice(0) : [];
            BI.each(values, function (i, val) {
                var normalized = normalizeAuthorizedDisplayValue(val);
                if (!normalized && val != null) {
                    normalized = String(val).trim();
                }
                if (!normalized) {
                    return;
                }
                var un = extractUsernameFromDisplay(normalized);
                if (!un) {
                    return;
                }
                var hit = BI.find(items, function (j, it) {
                    return it && it.username === un;
                });
                var display = preferAuthUserDisplay(un, [
                    normalized.indexOf("(") > 0 ? normalized : null,
                    savedDisplayByUsername[un],
                    hit && hit.value
                ]);
                savedDisplayByUsername[un] = display;
                items = BI.filter(items, function (j, it) {
                    return !(it && it.username === un);
                });
                items.push({
                    username: un,
                    value: display,
                    text: display,
                    title: display,
                    selected: true,
                    keyword: ""
                });
            });
            this.resultItems = items;
            this.allItems = items;
        },
        seed: function (values, callback) {
            var self = this;
            var displayValues = uniqueValues(BI.compact(BI.map(toUserList(values), function (i, v) {
                return normalizeAuthorizedDisplayValue(v);
            })));
            rememberSavedDisplays(displayValues);
            if (!displayValues.length) {
                self.resultItems = [];
                self.allItems = [];
                if (callback) {
                    callback();
                }
                return;
            }
            var finished = false;
            var done = function () {
                if (finished) {
                    return;
                }
                finished = true;
                self.mergeValues(displayValues);
                if (callback) {
                    callback();
                }
            };
            if (!Dec || !Dec.Utils || !Dec.Utils.reqPlatformUsers) {
                done();
                return;
            }
            try {
                Dec.Utils.reqPlatformUsers({page: 1, count: 500, keyword: ""}, false, function (res) {
                    var items = [];
                    BI.each((res && res.items) || [], function (i, user) {
                        items.push(formatPlatformItem(user, ""));
                    });
                    self.resultItems = items;
                    self.allItems = items;
                    done();
                });
            } catch (errSeed) {
                done();
            }
            setTimeout(done, 2000);
        },
        itemsCreator: function (e, callback) {
            var keyword = BI.isNotEmptyArray(e.keywords) ? e.keywords.join("") : "";
            var isNewSearch = this.lastSearchValue !== keyword;
            this.lastSearchValue = keyword;
            if (e.type === BI.MultiSelectCombo.REQ_GET_ALL_DATA) {
                authUserStore.lastPopupItems = this.resultItems.slice(0);
                callback({items: this.resultItems});
                return;
            }
            if (e.type === BI.MultiSelectCombo.REQ_GET_DATA_LENGTH) {
                callback({count: this.resultItems.length});
                return;
            }
            if (!e.times || !Dec || !Dec.Utils || !Dec.Utils.reqPlatformUsers) {
                callback({items: [], hasNext: false});
                return;
            }
            var self = this;
            Dec.Utils.reqPlatformUsers({
                page: e.times || 1,
                count: BI.isKey(keyword) && DecCst && DecCst.Web && DecCst.Web.User && DecCst.Web.User.Search
                    ? DecCst.Web.User.Search.PER_PAGE : 500,
                keyword: keyword
            }, false, function (res) {
                var pageItems = [];
                BI.each((res && res.items) || [], function (i, user) {
                    pageItems.push(formatPlatformItem(user, keyword));
                });
                self.allItems = self._concatDistinct(self.allItems, pageItems);
                self.resultItems = isNewSearch ? pageItems : self._concatDistinct(self.resultItems, pageItems);
                if (e.selectedValues) {
                    var selectedMap = BI.makeObject(e.selectedValues, true);
                    pageItems = BI.filter(pageItems, function (idx, item) {
                        return !selectedMap[item.value];
                    });
                }
                authUserStore.lastPopupItems = pageItems.slice(0);
                callback({items: pageItems, hasNext: !!BI.isKey(keyword) && res.hasNext});
            });
        },
        _concatDistinct: function (base, extra) {
            var merged = base.slice(0);
            BI.each(extra, function (i, item) {
                if (!item) {
                    return;
                }
                var un = item.username || extractUsernameFromDisplay(item.value);
                if (un) {
                    var idx = -1;
                    BI.each(merged, function (j, it) {
                        if (it && it.username === un) {
                            idx = j;
                        }
                    });
                    var display = preferAuthUserDisplay(un, [
                        item.value,
                        idx >= 0 ? merged[idx].value : null,
                        savedDisplayByUsername[un]
                    ]);
                    var mergedItem = BI.extend({}, idx >= 0 ? merged[idx] : {}, item, {
                        username: un,
                        value: display,
                        text: display,
                        title: display
                    });
                    if (idx >= 0) {
                        merged[idx] = mergedItem;
                    } else {
                        merged.push(mergedItem);
                    }
                    savedDisplayByUsername[un] = display;
                    return;
                }
                var known = BI.map(merged, "value");
                if (!BI.contains(known, item.value)) {
                    merged.push(item);
                }
            });
            return merged;
        }
    };

    /**
     * 平台组件 item.value / setValue 必须使用「显示名(用户名)」，不能仅用 admin、Anna。
     * @see dec.model.case.platform.user _formatItems in case.min.js
     */
    function toPlatformDisplayValues(values) {
        values = platformUserValues(values);
        if (!values.length) {
            return [];
        }
        return uniqueValues(BI.compact(BI.map(values, function (i, v) {
            if (!v) {
                return null;
            }
            var s = String(v).trim();
            if (!s) {
                return null;
            }
            if (s.indexOf("(") > 0 && s.lastIndexOf(")") > s.indexOf("(")) {
                return s;
            }
            var found = BI.find(authUserStore.resultItems, function (j, it) {
                return it && (it.value === s || it.username === s || it.text === s);
            });
            if (found && found.value) {
                return found.value;
            }
            if (isBareIndexTokens([s])) {
                return null;
            }
            var token = parseUserToken(s);
            if (token.username) {
                return token.display.indexOf("(") > 0 ? token.display : (token.username + "(" + token.username + ")");
            }
            return s + "(" + s + ")";
        })));
    }

    function refreshOrderComboTrigger(orderCombo, values) {
        if (!orderCombo || !values || !values.length) {
            return;
        }
        var text = values.join(", ");
        var inner = orderCombo.combo;
        if (inner) {
            try {
                if (inner.setText) {
                    inner.setText(text);
                }
                if (inner.trigger && inner.trigger.setText) {
                    inner.trigger.setText(text);
                }
            } catch (errInner) {
            }
        }
        try {
            var $ = getJQuery();
            if ($ && orderCombo.element) {
                $(orderCombo.element).find(".bi-text-trigger, .bi-select-text-trigger").first().text(text);
            }
        } catch (errDom) {
        }
    }

    function preloadAuthUserStore(callback) {
        if (!BI.MultiSelectCombo || !authUserStore.itemsCreator) {
            if (callback) {
                callback();
            }
            return;
        }
        try {
            authUserStore.itemsCreator({
                type: BI.MultiSelectCombo.REQ_GET_ALL_DATA,
                keywords: []
            }, function () {
                if (callback) {
                    callback();
                }
            });
        } catch (errPre) {
            if (callback) {
                callback();
            }
        }
    }

    function invalidatePendingAuthApply(orderCombo) {
        if (orderCombo) {
            orderCombo._authApplySeq = (orderCombo._authApplySeq || 0) + 1;
        }
    }

    function applyOrderComboValues(orderCombo, values) {
        if (!orderCombo || !values || !values.length) {
            return;
        }
        var applyGen = (orderCombo._authApplySeq = (orderCombo._authApplySeq || 0) + 1);
        var apply = function (displayValues) {
            if (!displayValues.length || orderCombo._authApplySeq !== applyGen) {
                return;
            }
            authUserStore.mergeValues(displayValues);
            setAuthUserSelection(displayValues);
            try {
                orderCombo.storeValue = displayValues.slice(0);
                orderCombo.setValue(displayValues);
                if (orderCombo.combo && orderCombo.combo.setValue) {
                    orderCombo.combo.setValue(displayValues);
                }
                if (typeof orderCombo._orderStoreValue === "function") {
                    orderCombo._orderStoreValue(displayValues);
                }
                refreshOrderComboTrigger(orderCombo, displayValues);
            } catch (errApply) {
            }
        };
        preloadAuthUserStore(function () {
            if (orderCombo._authApplySeq !== applyGen) {
                return;
            }
            var displayValues = toPlatformDisplayValuesFromUsernames(values);
            if (!displayValues.length) {
                displayValues = toPlatformDisplayValues(values);
            }
            apply(displayValues);
        });
    }

    function bindOrderComboEvents(orderCombo) {
        if (!orderCombo || !orderCombo.on) {
            return;
        }
        var onUserInteract = function () {
            invalidatePendingAuthApply(orderCombo);
        };
        orderCombo.on("EVENT_BEFORE_POPUPVIEW", onUserInteract);
        orderCombo.on("EVENT_TRIGGER_CLICK", onUserInteract);
        orderCombo.on("EVENT_CONFIRM", function () {
            invalidatePendingAuthApply(orderCombo);
            setTimeout(function () {
                syncAuthUserSelectionFromCombo(orderCombo);
            }, 0);
        });
        if (orderCombo.combo && orderCombo.combo.on) {
            orderCombo.combo.on("EVENT_CHANGE", onUserInteract);
            orderCombo.combo.on("EVENT_CLICK_ITEM", onUserInteract);
        }
    }

    function resolveAuthUserStore(widget) {
        if (!widget) {
            return null;
        }
        if (widget.store) {
            return widget.store;
        }
        if (typeof widget._store === "function") {
            try {
                widget.store = widget._store();
                return widget.store;
            } catch (errStore) {
            }
        }
        try {
            widget.store = BI.Models.getModel("dec.model.case.platform.user", widget.options || {});
            return widget.store;
        } catch (errModel) {
        }
        return null;
    }

    function bindAuthUserStoreToWidget(widget) {
        var store = resolveAuthUserStore(widget);
        if (!store) {
            return;
        }
        store.itemsCreator = BI.bind(authUserStore.itemsCreator, authUserStore);
        store.resultItems = authUserStore.resultItems;
        store.allItems = authUserStore.allItems;
        store.lastSearchValue = authUserStore.lastSearchValue;
    }

    function toPlainArray(src) {
        if (!src) {
            return [];
        }
        if (BI.isArray(src)) {
            return src.slice(0);
        }
        if (typeof src.slice === "function") {
            try {
                var sliced = src.slice(0);
                if (sliced && sliced.length) {
                    return BI.isArray(sliced) ? sliced.slice(0) : toPlainArray(sliced);
                }
            } catch (errSlice) {
            }
        }
        if (typeof src === "object") {
            var keys = [];
            var k;
            for (k in src) {
                if (Object.prototype.hasOwnProperty.call(src, k) && /^\d+$/.test(k)) {
                    keys.push(k);
                }
            }
            if (keys.length) {
                keys.sort(function (a, b) {
                    return parseInt(a, 10) - parseInt(b, 10);
                });
                return BI.map(keys, function (i, key) {
                    return src[key];
                });
            }
        }
        return toUserList(src);
    }

    function isBareIndexTokens(values) {
        if (!values || !values.length) {
            return false;
        }
        var only = true;
        BI.each(values, function (i, v) {
            var s = String(v).trim();
            if (!/^\d{1,2}$/.test(s)) {
                only = false;
            }
            if (s.indexOf("(") >= 0 || /[\u4e00-\u9fa5A-Za-z]/.test(s)) {
                only = false;
            }
        });
        return only;
    }

    function getPlatformUserItems(orderCombo, platformWidget) {
        var items = [];
        var store = platformWidget && platformWidget.store;
        if (store && store.resultItems) {
            items = toPlainArray(store.resultItems);
        }
        if (!items.length && orderCombo) {
            if (orderCombo.combo && orderCombo.combo.items) {
                items = toPlainArray(orderCombo.combo.items);
            }
            if (!items.length && orderCombo.items) {
                items = toPlainArray(orderCombo.items);
            }
            if (!items.length && orderCombo.combo && orderCombo.combo.store && orderCombo.combo.store.items) {
                items = toPlainArray(orderCombo.combo.store.items);
            }
        }
        if (!items.length && authUserStore.resultItems.length) {
            items = authUserStore.resultItems.slice(0);
        }
        return items;
    }

    function capturePopupItemsFromDom() {
        var $ = getJQuery();
        if (!$) {
            return [];
        }
        var pageItems = [];
        $(".bi-popup-view:visible, .bi-popover:visible").find(".bi-multi-select-item").each(function () {
            var $item = $(this);
            var raw = ($item.attr("data-value") || $item.text() || "").trim();
            var display = normalizeStoreValueItem(raw);
            if (!display) {
                return;
            }
            var token = parseUserToken(display);
            pageItems.push({
                username: token.username || display,
                value: display,
                text: display,
                title: display
            });
        });
        if (pageItems.length) {
            authUserStore.lastPopupItems = pageItems;
        }
        return pageItems;
    }

    function resolveIndexTokensToUsers(indices, itemSource) {
        var items = toPlainArray(itemSource);
        if (!items.length || !isBareIndexTokens(indices)) {
            return [];
        }
        return uniqueValues(BI.compact(BI.map(indices, function (i, token) {
            var n = parseInt(String(token), 10);
            if (isNaN(n) || n < 0 || n >= items.length) {
                return null;
            }
            var it = items[n];
            return normalizeStoreValueItem(it && (it.value || it.text || it));
        })));
    }

    function readCheckedUserValuesFromPopup() {
        var $ = getJQuery();
        if (!$) {
            return [];
        }
        var values = [];
        var seen = {};
        var addValue = function (v) {
            v = normalizeStoreValueItem(v);
            if (v && !seen[v]) {
                seen[v] = true;
                values.push(v);
            }
        };
        $(".bi-popup-view:visible, .bi-popover:visible").find(".bi-multi-select-item").each(function () {
            var $item = $(this);
            var selected = $item.hasClass("selected")
                || $item.hasClass("active")
                || $item.hasClass("bi-multi-select-item-selected")
                || $item.find("input[type=checkbox]:checked, input[type=radio]:checked").length > 0;
            if (!selected) {
                return;
            }
            var dataVal = $item.attr("data-value");
            if (dataVal) {
                addValue(dataVal);
                return;
            }
            var txt = ($item.text() || "").trim();
            BI.each(parseDisplayTokens(txt), function (i, token) {
                addValue(token);
            });
        });
        return values;
    }

    function coerceInnerMultiSelectRaw(raw, orderCombo) {
        raw = toPlainArray(raw);
        if (!raw.length) {
            return [];
        }
        if (!isBareIndexTokens(raw)) {
            return platformUserValues(raw);
        }
        var fromPopup = resolveIndexTokensToUsers(raw, authUserStore.lastPopupItems);
        if (fromPopup.length) {
            return fromPopup;
        }
        return [];
    }

    function finalizeAuthUserValues(values, orderCombo, platformWidget) {
        values = platformUserValues(values);
        if (values.length && !isBareIndexTokens(values)) {
            return values;
        }
        if (isBareIndexTokens(values)) {
            return [];
        }
        var fromStore = platformUserValues(toPlainArray(orderCombo && orderCombo.storeValue));
        if (fromStore.length && !isBareIndexTokens(fromStore)) {
            return fromStore;
        }
        return [];
    }

    function readAuthUserDisplayValues(orderCombo) {
        if (!orderCombo) {
            return [];
        }
        var values = readCheckedUserValuesFromPopup();
        if (!values.length) {
            values = finalizeAuthUserValues(toPlainArray(orderCombo.storeValue), orderCombo, null);
        }
        if (!values.length && orderCombo.getValue) {
            values = finalizeAuthUserValues(toPlainArray(orderCombo.getValue()), orderCombo, null);
        }
        if (!values.length) {
            capturePopupItemsFromDom();
            if (orderCombo.combo && orderCombo.combo.getValue) {
                values = coerceInnerMultiSelectRaw(orderCombo.combo.getValue(), orderCombo);
            }
        }
        if (!values.length) {
            values = readCheckedUserValuesFromPopup();
        }
        return values;
    }

    function syncAuthUserSelectionFromCombo(orderCombo) {
        var values = readAuthUserDisplayValues(orderCombo);
        if (values.length && !isBareIndexTokens(values)) {
            setAuthUserSelection(values);
        }
    }

    function normalizeStoreValueItem(u) {
        if (BI.isNull(u) || u === undefined) {
            return null;
        }
        if (BI.isString(u) || typeof u === "number") {
            var s = String(u).trim();
            if (!s || s.indexOf("[object") >= 0) {
                return null;
            }
            if (s.indexOf("|") >= 0) {
                var parts = s.split("|", 2);
                return parts.length > 1 ? parts[1].trim() : parts[0].trim();
            }
            return s;
        }
        var display = u.value || u.text || u.title || u.name || u.label || u.displayName;
        if (display != null && typeof display !== "object") {
            var ds = String(display).trim();
            if (ds && ds.indexOf("[object") < 0) {
                return ds;
            }
        }
        var un = u.username || u.userName || u.id;
        if (un != null && typeof un !== "object") {
            var uname = String(un).trim();
            if (uname) {
                var rn = u.realName || u.realname || u.name;
                if (rn != null && typeof rn !== "object") {
                    var rns = String(rn).trim();
                    if (rns && rns !== uname) {
                        return rns + "(" + uname + ")";
                    }
                }
                if (uname.indexOf("(") > 0) {
                    return uname;
                }
            }
        }
        return null;
    }

    function platformUserValues(users) {
        var original = users;
        users = toPlainArray(users);
        if (!users.length) {
            users = toUserList(original);
        }
        if (!users.length) {
            return [];
        }
        return uniqueValues(BI.compact(BI.map(users, function (i, v) {
            return normalizeStoreValueItem(v);
        })));
    }

    function setAuthUserSelection(values) {
        authUserSelection = platformUserValues(values);
    }

    function getJQuery() {
        return BI.$ || window.jQuery || window.$;
    }

    function parseDisplayTokens(text) {
        if (!text) {
            return [];
        }
        var cleaned = String(text)
            .replace(/授权用户/g, "")
            .replace(/数据集调试助手/g, "")
            .replace(/保存/g, "");
        var matches = cleaned.match(/[\u4e00-\u9fa5A-Za-z0-9_.\-]+\s*[\(\uFF08][^)\uFF09]+[\)\uFF09]/g);
        return platformUserValues(matches || []);
    }

    function uniqueValues(list) {
        var seen = {};
        var out = [];
        BI.each(list, function (i, v) {
            if (v && !seen[v]) {
                seen[v] = true;
                out.push(v);
            }
        });
        return out;
    }

    /**
     * 从页面可见文案读取（不依赖 BI.Cache / getValue）。
     * 决策平台 DOM 通常不是 .bi-horizontal，而在「授权用户」标签附近 500 字内。
     */
    function readAuthUsersFromDocument() {
        var $ = getJQuery();
        var results = [];
        if ($) {
            $(".dec-common-order-multi-select-combo").each(function () {
                results = results.concat(parseDisplayTokens($(this).text()));
            });
            if (!results.length) {
                $("[class*='order-multi-select'], [class*='multi-select']").each(function () {
                    var t = ($(this).text() || "").trim();
                    if (t.indexOf("(") > 0) {
                        results = results.concat(parseDisplayTokens(t));
                    }
                });
            }
        }
        function collectBodyText() {
            var chunks = [];
            var pushDoc = function (doc) {
                if (!doc || !doc.body) {
                    return;
                }
                try {
                    var t = doc.body.innerText || doc.body.textContent || "";
                    if (t) {
                        chunks.push(t);
                    }
                } catch (err) {
                }
            };
            pushDoc(document);
            try {
                var frames = document.getElementsByTagName("iframe");
                var fi;
                for (fi = 0; fi < frames.length; fi++) {
                    try {
                        pushDoc(frames[fi].contentDocument);
                    } catch (err2) {
                    }
                }
            } catch (err3) {
            }
            return chunks.join("\n");
        }
        var bodyText = collectBodyText();
        if (!bodyText && $) {
            bodyText = $("body").text() || "";
        }
        var labelKeys = ["授权用户"];
        try {
            var i18nLabel = BI.i18nText("Report_Debug_Config_Authorized_Users");
            if (i18nLabel && labelKeys.indexOf(i18nLabel) < 0) {
                labelKeys.push(i18nLabel);
            }
        } catch (errLabel) {
        }
        var best = [];
        var searchFrom = 0;
        var keyIdx;
        for (keyIdx = 0; keyIdx < labelKeys.length; keyIdx++) {
            var key = labelKeys[keyIdx];
            if (!key) {
                continue;
            }
            searchFrom = 0;
            while (searchFrom < bodyText.length) {
                var idx = bodyText.indexOf(key, searchFrom);
                if (idx < 0) {
                    break;
                }
                searchFrom = idx + key.length;
                var slice = bodyText.substring(idx, idx + 500);
                var parsed = parseDisplayTokens(slice);
                var lines = slice.split("\n");
                var li;
                for (li = 1; li < lines.length && li < 6; li++) {
                    var line = lines[li].trim();
                    if (line && /\(/.test(line)) {
                        parsed = parsed.concat(parseDisplayTokens(line));
                    }
                }
                parsed = uniqueValues(parsed);
                if (parsed.length > best.length) {
                    best = parsed;
                }
            }
        }
        if (!best.length) {
            var pluginIdx = bodyText.indexOf("数据集调试助手");
            if (pluginIdx < 0) {
                try {
                    pluginIdx = bodyText.indexOf(BI.i18nText("Fine-Plugin_Report_Debug_Assistant"));
                } catch (errPlugin) {
                }
            }
            if (pluginIdx >= 0) {
                best = parseDisplayTokens(bodyText.substring(pluginIdx, pluginIdx + 800));
            }
        }
        if (isBareIndexTokens(best)) {
            return [];
        }
        return uniqueValues(best);
    }

    function resolveOrderCombo(self) {
        if (!self) {
            return window.__REPORT_DEBUG_ORDER_COMBO__ || null;
        }
        return self._authUserCombo
            || window.__REPORT_DEBUG_ORDER_COMBO__
            || self.authUsers
            || null;
    }

    function readStoreValueDirect(orderCombo, platformWidget) {
        if (!orderCombo) {
            return [];
        }
        var sources = [
            orderCombo.storeValue,
            orderCombo.value
        ];
        var si;
        for (si = 0; si < sources.length; si++) {
            var raw = toPlainArray(sources[si]);
            if (raw.length) {
                var parsed = finalizeAuthUserValues(raw, orderCombo, platformWidget);
                if (parsed.length) {
                    return parsed;
                }
            }
        }
        try {
            if (orderCombo.getValue) {
                var v = finalizeAuthUserValues(toPlainArray(orderCombo.getValue()), orderCombo, platformWidget);
                if (v.length) {
                    return v;
                }
            }
        } catch (err) {
        }
        if (orderCombo.combo) {
            var innerCombo = orderCombo.combo;
            var innerSources = [innerCombo.storeValue, innerCombo.value];
            for (si = 0; si < innerSources.length; si++) {
                raw = toPlainArray(innerSources[si]);
                if (raw.length) {
                    parsed = finalizeAuthUserValues(raw, innerCombo, platformWidget);
                    if (parsed.length) {
                        return parsed;
                    }
                }
            }
            try {
                if (innerCombo.getValue) {
                    var inner = finalizeAuthUserValues(toPlainArray(innerCombo.getValue()), innerCombo, platformWidget);
                    if (inner.length) {
                        return inner;
                    }
                }
            } catch (err2) {
            }
        }
        return [];
    }

    function collectFromOrderCombo(orderCombo) {
        if (!orderCombo || !orderCombo.getValue) {
            return [];
        }
        try {
            return finalizeAuthUserValues(toPlainArray(orderCombo.getValue()), orderCombo, null);
        } catch (err) {
            return [];
        }
    }

    function clickMultiSelectConfirmIfOpen() {
        try {
            var $ = getJQuery();
            if (!$) {
                return;
            }
            var labels = [BI.i18nText("BI-Basic_Sure"), BI.i18nText("BI-Basic_OK"), "确定", "OK"];
            $(".bi-popup-view:visible .bi-button, .bi-popover:visible .bi-button").each(function () {
                var txt = ($(this).text() || "").replace(/\s+/g, "");
                var hit = false;
                BI.each(labels, function (i, label) {
                    if (label && (txt === String(label).replace(/\s+/g, "") || txt.indexOf("确定") >= 0)) {
                        $(this).trigger("click");
                        hit = true;
                        return false;
                    }
                });
                if (hit) {
                    return false;
                }
            });
        } catch (err) {
        }
    }

    function readInnerComboDisplayValues(orderCombo) {
        if (!orderCombo || !orderCombo.combo || !orderCombo.combo.getValue) {
            return [];
        }
        try {
            return coerceInnerMultiSelectRaw(orderCombo.combo.getValue(), orderCombo);
        } catch (errInner) {
            return [];
        }
    }

    function collectAuthUsersForSave(self) {
        var orderCombo = resolveOrderCombo(self);
        clickMultiSelectConfirmIfOpen();
        var displayValues = readAuthUserDisplayValues(orderCombo);
        if (!displayValues.length && authUserSelection.length && !isBareIndexTokens(authUserSelection)) {
            displayValues = authUserSelection.slice(0);
        }
        displayValues = toPlatformDisplayValues(displayValues);
        var usernames = displayValuesToUsernames(displayValues);
        if (!usernames.length) {
            usernames = [DEFAULT_AUTH_USERNAME];
            displayValues = toPlatformDisplayValuesFromUsernames(usernames);
        }
        return {
            displayValues: displayValues,
            usernames: usernames
        };
    }

    function normalizeSave(e) {
        if (!e) {
            return {status: "error"};
        }
        if (e.status) {
            return e;
        }
        if (e.data) {
            return BI.extend({status: "success"}, e.data);
        }
        return {status: "success"};
    }

    window.__REPORT_DEBUG_AUTH_STATE__ = function () {
        var combo = window.__REPORT_DEBUG_ORDER_COMBO__;
        var rawStore = combo ? toPlainArray(combo.storeValue) : [];
        var innerText = "";
        try {
            if (combo && combo.combo && combo.combo.trigger && combo.combo.trigger.getText) {
                innerText = combo.combo.trigger.getText();
            }
        } catch (errT) {
        }
        var fromStore = readStoreValueDirect(combo, null);
        var fromInner = readInnerComboDisplayValues(combo);
        return {
            build: window.__REPORT_DEBUG_ASSISTANT_BUILD__,
            selection: authUserSelection.slice(0),
            fromOrderGetValue: collectFromOrderCombo(combo),
            fromStoreValue: fromStore,
            fromInnerCombo: fromInner,
            usernames: displayValuesToUsernames(fromStore.length ? fromStore : fromInner),
            getValueRaw: combo && combo.getValue ? toPlainArray(combo.getValue()) : [],
            innerGetValueRaw: combo && combo.combo && combo.combo.getValue
                ? toPlainArray(combo.combo.getValue()) : [],
            triggerText: innerText,
            resultItemsLen: authUserStore.resultItems.length,
            storeValue: rawStore,
            storeValueLength: rawStore.length
        };
    };

    var ReportDebugManagement = BI.inherit(BI.Widget, {
        props: {
            baseCls: "",
            authorizedUsers: []
        },
        mounted: function () {
        },
        beforeInit: function (callback) {
            var self = this;
            Dec.reqGet(CONFIG_GET, "", function (res) {
                var cfg = {
                    authorizedUsers: [],
                    loginAuthOpen: true,
                    displayAbsolutePath: false,
                    enabled: true,
                    allowReportPreview: true,
                    maskSqlInResponse: true
                };
                var e = normalizeGet(res);
                if (e && e.status === "success") {
                    cfg.authorizedUsers = authorizedUsersFromResponse(e);
                    rememberSavedDisplays(cfg.authorizedUsers);
                    cfg.loginAuthOpen = !!e.loginAuthOpen;
                    cfg.displayAbsolutePath = !!e.displayAbsolutePath;
                    cfg.enabled = e.enabled !== false && e.enabled !== "false";
                    cfg.allowReportPreview = e.allowReportPreview !== false;
                    cfg.maskSqlInResponse = !!e.maskSqlInResponse;
                    setAuthUserSelection(cfg.authorizedUsers);
                    authUserStore.seed(cfg.authorizedUsers, function () {
                        self.options.reportDebugConfig = cfg;
                        callback();
                    });
                    return;
                }
                BI.Msg.toast((e && (e.errorMsg || e.msg)) || "error", {level: "error"});
                setAuthUserSelection([]);
                self.options.reportDebugConfig = cfg;
                callback();
            });
        },
        _applyAuthUsers: function (values) {
            applyOrderComboValues(this._authUserCombo, values);
        },
        _captureAuthUserCombo: function (orderCombo) {
            var self = this;
            if (!orderCombo) {
                return;
            }
            self._authUserCombo = orderCombo;
            self.authUsers = orderCombo;
            window.__REPORT_DEBUG_ORDER_COMBO__ = orderCombo;
            bindOrderComboEvents(orderCombo);
            applyOrderComboValues(orderCombo, (self.options.reportDebugConfig || {}).authorizedUsers);
        },
        render: function () {
            var self = this;
            var cfg = this.options.reportDebugConfig || {};
            var authDisplayValues = toPlatformDisplayValues(cfg.authorizedUsers || []);
            return {
                type: "bi.vtape",
                items: [{
                    type: "bi.vertical_adapt",
                    cls: "bi-card bi-border-bottom bi-font-bold",
                    items: [{
                        type: "bi.label",
                        text: BI.i18nText("Fine-Plugin_Report_Debug_Assistant"),
                        textAlign: "left",
                        hgap: 10
                    }, {
                        type: "bi.button",
                        text: BI.i18nText("BI-Basic_Save"),
                        textAlign: "center",
                        hgap: 10,
                        handler: function () {
                            self._saveAllData();
                        }
                    }],
                    height: 40,
                    bgap: 10
                }, {
                    type: "bi.vertical",
                    cls: "bi-card",
                    items: [{
                        el: {
                            type: "bi.horizontal",
                            cls: "bi-card",
                            items: [{
                                el: {
                                    type: "bi.label",
                                    cls: "dec-font-weight-bold",
                                    text: BI.i18nText("Report_Debug_Config_Enabled"),
                                    textAlign: "left",
                                    hgap: 10
                                },
                                width: 240,
                                height: 40,
                                tgap: 4
                            }, {
                                el: {
                                    type: "bi.switch",
                                    selected: cfg.enabled !== false,
                                    ref: function (w) {
                                        self.pluginEnabled = w;
                                    }
                                }
                            }, {
                                type: "bi.icon_button",
                                title: BI.i18nText("Report_Debug_Config_Enabled_Tips"),
                                cls: "detail-font",
                                width: 30,
                                height: 24
                            }]
                        },
                        lgap: 10,
                        tgap: 10
                    }, {
                        el: {
                            type: "bi.horizontal",
                            cls: "bi-card",
                            items: [{
                                el: {
                                    type: "bi.label",
                                    cls: "dec-font-weight-bold",
                                    text: BI.i18nText("Report_Debug_Config_Authorized_Users"),
                                    textAlign: "left",
                                    hgap: 10
                                },
                                width: 240,
                                height: 40,
                                tgap: 4
                            }, {
                                el: {
                                    type: "dec.common.order_multi_select_combo",
                                    itemsCreator: BI.bind(authUserStore.itemsCreator, authUserStore),
                                    width: 300,
                                    height: 24,
                                    ref: function (w) {
                                        self._captureAuthUserCombo(w);
                                    }
                                }
                            }, {
                                type: "bi.icon_button",
                                title: BI.i18nText("Report_Debug_Config_Authorized_Users_Tips"),
                                cls: "detail-font",
                                width: 30,
                                height: 24
                            }]
                        },
                        lgap: 10,
                        tgap: 10
                    }, {
                        el: {
                            type: "bi.horizontal",
                            cls: "bi-card",
                            items: [{
                                el: {
                                    type: "bi.label",
                                    cls: "dec-font-weight-bold",
                                    text: BI.i18nText("Dec-Login_Authentication_Open"),
                                    textAlign: "left",
                                    hgap: 10
                                },
                                width: 240,
                                height: 40,
                                tgap: 4
                            }, {
                                el: {
                                    type: "bi.switch",
                                    selected: cfg.loginAuthOpen,
                                    ref: function (w) {
                                        self.loginAuth = w;
                                    }
                                }
                            }, {
                                type: "bi.icon_button",
                                title: BI.i18nText("Report_Debug_Config_Login_Auth_Tips"),
                                cls: "detail-font",
                                width: 30,
                                height: 24
                            }]
                        },
                        lgap: 10,
                        tgap: 10
                    }, {
                        el: {
                            type: "bi.horizontal",
                            cls: "bi-card",
                            items: [{
                                el: {
                                    type: "bi.label",
                                    cls: "dec-font-weight-bold",
                                    text: BI.i18nText("Report_Debug_Config_Display_Absolute_Path"),
                                    textAlign: "left",
                                    hgap: 10
                                },
                                width: 240,
                                height: 40,
                                tgap: 4
                            }, {
                                el: {
                                    type: "bi.switch",
                                    selected: cfg.displayAbsolutePath,
                                    ref: function (w) {
                                        self.displayAbsolutePath = w;
                                    }
                                }
                            }, {
                                type: "bi.icon_button",
                                title: BI.i18nText("Report_Debug_Config_Display_Absolute_Path_Tips"),
                                cls: "detail-font",
                                width: 30,
                                height: 24
                            }]
                        },
                        lgap: 10,
                        tgap: 10
                    }, {
                        el: {
                            type: "bi.horizontal",
                            cls: "bi-card",
                            items: [{
                                el: {
                                    type: "bi.label",
                                    cls: "dec-font-weight-bold",
                                    text: BI.i18nText("Report_Debug_Config_Allow_Preview"),
                                    textAlign: "left",
                                    hgap: 10
                                },
                                width: 240,
                                height: 40,
                                tgap: 4
                            }, {
                                el: {
                                    type: "bi.switch",
                                    selected: cfg.allowReportPreview,
                                    ref: function (w) {
                                        self.allowPreview = w;
                                    }
                                }
                            }]
                        },
                        lgap: 10,
                        tgap: 10
                    }, {
                        el: {
                            type: "bi.horizontal",
                            cls: "bi-card",
                            items: [{
                                el: {
                                    type: "bi.label",
                                    cls: "dec-font-weight-bold",
                                    text: BI.i18nText("Report_Debug_Config_Mask_Sql"),
                                    textAlign: "left",
                                    hgap: 10
                                },
                                width: 240,
                                height: 40,
                                tgap: 4
                            }, {
                                el: {
                                    type: "bi.switch",
                                    selected: cfg.maskSqlInResponse,
                                    ref: function (w) {
                                        self.maskSql = w;
                                    }
                                }
                            }, {
                                type: "bi.icon_button",
                                title: BI.i18nText("Report_Debug_Config_Mask_Sql_Tips"),
                                cls: "detail-font",
                                width: 30,
                                height: 24
                            }]
                        },
                        lgap: 10,
                        tgap: 10
                    }]
                }]
            };
        },
        _saveAllData: function () {
            this._postSave();
        },
        _postSave: function () {
            var self = this;
            var cfg = this.options.reportDebugConfig || {};
            var collected = collectAuthUsersForSave(self);
            var users = collected.displayValues;
            var usernames = collected.usernames;
            if (!users.length) {
                BI.Msg.toast("未读取到授权用户，请勾选用户并点击弹层「确定」后再保存", {level: "warning"});
                return;
            }
            var payload = {
                authorizedUsers: users,
                authorizedUsernames: usernames,
                authorizedUsersText: users.join("\n"),
                loginAuthOpen: this.loginAuth ? this.loginAuth.isSelected() : false,
                displayAbsolutePath: this.displayAbsolutePath ? this.displayAbsolutePath.isSelected() : false,
                enabled: this.pluginEnabled ? this.pluginEnabled.isSelected() : true,
                allowReportPreview: this.allowPreview ? this.allowPreview.isSelected() : true,
                maskSqlInResponse: this.maskSql ? this.maskSql.isSelected() : false
            };
            Dec.reqPost(CONFIG_SAVE, payload, function (res) {
                var saveRes = normalizeSave(res);
                if (saveRes && saveRes.status === "success") {
                    var saved = authorizedUsersFromResponse(saveRes);
                    if (!saved.length) {
                        saved = users;
                    }
                    rememberSavedDisplays(saved);
                    cfg.authorizedUsers = saved;
                    self.options.reportDebugConfig = cfg;
                    setAuthUserSelection(saved);
                    self._applyAuthUsers(saved);
                }
                self._saveTips(saveRes);
            });
        },
        _saveTips: function (e) {
            if (e && e.status === "success") {
                BI.Msg.toast(BI.i18nText("Dec-Basic_Save_Success"));
                return;
            }
            var msg = (e && (e.errorMsg || e.msg)) || "";
            BI.Msg.toast(BI.i18nText("Dec-Basic_Save_Fail") + (msg ? "," + msg : ""), {level: "error"});
        }
    });

    BI.shortcut("dec.management.plugin.report.debug.assistant", ReportDebugManagement);
}());

!(function () {
    BI.config("dec.constant.management.navigation", function (items) {
        items.push({
            value: "report-debug-assistant",
            id: "decision-management-report-debug-assistant",
            text: BI.i18nText("Fine-Plugin_Report_Debug_Assistant"),
            cardType: "dec.management.plugin.report.debug.assistant",
            cls: "management-log-font"
        });
        return items;
    });
}());
