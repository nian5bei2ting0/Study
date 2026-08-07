!(function (global) {

    'use strict';



    var TIP_CLASS = 'fr-indicator-tip';

    var BOUND_FLAG = '__frIndicatorTipHoverBound';

    var ARGS_FLAG = '__frIndicatorTipArgs';

    var SCAN_TIMER = null;

    var HIDE_TIMER = null;

    var GAP = 10;

    var VIEW_PAD = 8;

    var activeTip = null;

    var activeAnchor = null;



    var STYLE_MAP = {

        darkCyan: 'fr-style-darkCyan',

        darkSimple: 'fr-style-darkSimple',

        lightWarm: 'fr-style-lightWarm',

        lightInfo: 'fr-style-lightInfo',

        success: 'fr-style-success',

        warning: 'fr-style-warning',

        custom: 'fr-style-custom'

    };



    function removeTip() {

        var tips = document.querySelectorAll('.' + TIP_CLASS);

        for (var i = 0; i < tips.length; i++) {

            if (tips[i].parentNode) {

                tips[i].parentNode.removeChild(tips[i]);

            }

        }

        activeTip = null;

        activeAnchor = null;

    }



    function resolveAnchorEl(el) {

        if (!el || !el.tagName) {

            return el;

        }

        var node = el;

        while (node && node !== document.body) {

            var tag = node.tagName;

            if (tag === 'TD' || tag === 'TH') {

                return node;

            }

            node = node.parentNode;

        }

        return el;

    }



    function normalizeStyle(styleId) {

        if (!styleId) {

            return 'darkCyan';

        }

        return STYLE_MAP[styleId] ? styleId : 'darkCyan';

    }



    function copyText(text) {

        if (!text) {

            return Promise.resolve(false);

        }

        if (global.navigator && navigator.clipboard && navigator.clipboard.writeText) {

            return navigator.clipboard.writeText(text).then(function () {

                return true;

            }).catch(function () {

                return fallbackCopy(text);

            });

        }

        return Promise.resolve(fallbackCopy(text));

    }



    function fallbackCopy(text) {

        try {

            var area = document.createElement('textarea');

            area.value = text;

            area.setAttribute('readonly', 'readonly');

            area.style.position = 'fixed';

            area.style.left = '-9999px';

            document.body.appendChild(area);

            area.select();

            var ok = document.execCommand('copy');

            document.body.removeChild(area);

            return !!ok;

        } catch (e) {

            return false;

        }

    }



    /**

     * 气泡优先放在单元格右侧（带左箭头），空间不足则放左侧。

     */

    function placeTipByAnchor(tip, anchorEl) {

        tip.style.visibility = 'hidden';

        tip.style.left = '0px';

        tip.style.top = '0px';

        document.body.appendChild(tip);



        var ar = anchorEl.getBoundingClientRect();

        var tr = tip.getBoundingClientRect();

        var tipW = tr.width;

        var tipH = tr.height;

        var vw = window.innerWidth || document.documentElement.clientWidth || 0;

        var vh = window.innerHeight || document.documentElement.clientHeight || 0;



        var placeRight = (ar.right + GAP + tipW <= vw - VIEW_PAD);

        var placeLeft = (ar.left - GAP - tipW >= VIEW_PAD);

        var side = 'right';

        var left;

        if (placeRight) {

            side = 'right';

            left = ar.right + GAP;

        } else if (placeLeft) {

            side = 'left';

            left = ar.left - GAP - tipW;

        } else {

            // 两端都不够：尽量贴右侧并限制视口

            side = 'right';

            left = Math.max(VIEW_PAD, Math.min(ar.right + GAP, vw - tipW - VIEW_PAD));

        }



        // 垂直：气泡中心对齐单元格中心

        var top = ar.top + ar.height / 2 - tipH / 2;

        top = Math.max(VIEW_PAD, Math.min(top, vh - tipH - VIEW_PAD));



        tip.classList.remove('fr-tip-left', 'fr-tip-right');

        tip.classList.add(side === 'right' ? 'fr-tip-right' : 'fr-tip-left');



        // 箭头对齐到单元格垂直中心

        var arrowTop = (ar.top + ar.height / 2) - top;

        arrowTop = Math.max(14, Math.min(arrowTop, tipH - 14));

        tip.style.setProperty('--fr-tip-arrow-top', Math.round(arrowTop) + 'px');



        tip.style.left = Math.round(left) + 'px';

        tip.style.top = Math.round(top) + 'px';

        tip.style.visibility = 'visible';

        activeTip = tip;

        activeAnchor = anchorEl;

    }



    function showIndicatorTip(args, evt, triggerEl) {

        args = args || {};

        if (HIDE_TIMER) {

            clearTimeout(HIDE_TIMER);

            HIDE_TIMER = null;

        }

        removeTip();



        var content = args.content == null ? '' : String(args.content);

        if (!content) {

            return;

        }



        var anchorEl = resolveAnchorEl(triggerEl);

        if (!anchorEl || !anchorEl.getBoundingClientRect) {

            return;

        }



        var styleId = normalizeStyle(args.tipStyle);

        var showCopy = !!args.showCopy;

        var tip = document.createElement('div');

        tip.className = TIP_CLASS + ' ' + STYLE_MAP[styleId];

        if (showCopy) {

            tip.className += ' fr-tip-interactive';

        }

        tip.style.fontFamily = args.fontFamily || 'Microsoft YaHei';

        tip.style.fontSize = (args.fontSize || 14) + 'px';



        if (styleId === 'custom') {

            tip.style.color = args.fontColor || '#FFFFFF';

            tip.style.backgroundColor = args.backgroundColor || '#1A1F2C';

            tip.style.borderColor = 'rgba(0,0,0,0.12)';

            tip.style.setProperty('--fr-tip-arrow-border', tip.style.borderColor);

            tip.style.setProperty('--fr-tip-arrow-fill', tip.style.backgroundColor);

        }



        if (showCopy) {

            var btn = document.createElement('button');

            btn.type = 'button';

            btn.className = 'fr-indicator-tip-copy';

            btn.textContent = '复制';

            btn.addEventListener('click', function (e) {

                e.preventDefault();

                e.stopPropagation();

                copyText(content).then(function (ok) {

                    if (!ok) {

                        return;

                    }

                    btn.textContent = '已复制';

                    btn.classList.add('is-copied');

                    setTimeout(function () {

                        btn.textContent = '复制';

                        btn.classList.remove('is-copied');

                    }, 1200);

                });

            });

            tip.appendChild(btn);

        }



        var body = document.createElement('div');

        body.className = 'fr-indicator-tip-body';

        body.textContent = content;

        tip.appendChild(body);



        if (showCopy) {

            tip.addEventListener('mouseenter', function () {

                if (HIDE_TIMER) {

                    clearTimeout(HIDE_TIMER);

                    HIDE_TIMER = null;

                }

            });

            tip.addEventListener('mouseleave', function () {

                scheduleHide();

            });

        }



        placeTipByAnchor(tip, anchorEl);

    }



    function scheduleHide() {

        if (HIDE_TIMER) {

            clearTimeout(HIDE_TIMER);

        }

        HIDE_TIMER = setTimeout(function () {

            removeTip();

            HIDE_TIMER = null;

        }, 80);

    }



    function extractJsonObject(text, fromIndex) {

        var start = text.indexOf('{', fromIndex);

        if (start < 0) {

            return null;

        }

        var depth = 0;

        var inString = false;

        var escape = false;

        for (var i = start; i < text.length; i++) {

            var ch = text.charAt(i);

            if (inString) {

                if (escape) {

                    escape = false;

                } else if (ch === '\\') {

                    escape = true;

                } else if (ch === '"') {

                    inString = false;

                }

                continue;

            }

            if (ch === '"') {

                inString = true;

            } else if (ch === '{') {

                depth++;

            } else if (ch === '}') {

                depth--;

                if (depth === 0) {

                    try {

                        return JSON.parse(text.substring(start, i + 1));

                    } catch (e) {

                        return null;

                    }

                }

            }

        }

        return null;

    }



    function b64ToUtf8(b64) {

        try {

            var binary = global.atob(b64);

            var chars = [];

            for (var i = 0; i < binary.length; i++) {

                chars.push('%' + ('00' + binary.charCodeAt(i).toString(16)).slice(-2));

            }

            return decodeURIComponent(chars.join(''));

        } catch (e) {

            try {

                return global.atob(b64);

            } catch (e2) {

                return '';

            }

        }

    }



    function parseTipArgsFromCode(code) {

        if (!code) {

            return null;

        }

        var marker = code.match(/\/\*FR_INDICATOR_TIP:([A-Za-z0-9+/=]+)\*\//);

        if (marker && marker[1]) {

            try {

                var jsonText = b64ToUtf8(marker[1]);

                var obj = JSON.parse(jsonText);

                if (obj && obj.content != null) {

                    return obj;

                }

            } catch (e) {

                // continue fallback

            }

        }

        var markers = ['showIndicatorTip(', 'FR.showIndicatorTip(', '"indicatorTip"'];

        for (var i = 0; i < markers.length; i++) {

            var idx = code.indexOf(markers[i]);

            if (idx >= 0) {

                var parsed = extractJsonObject(code, idx);

                if (parsed && parsed.content != null) {

                    return parsed;

                }

                if (parsed && parsed.indicatorTip && parsed.indicatorTip.content != null) {

                    return parsed.indicatorTip;

                }

            }

        }

        return null;

    }



    function readCodeFromEl(el) {

        if (!el || !el.getAttribute) {

            return '';

        }

        var code = (el.getAttribute('onclick') || '') + (el.getAttribute('data-fr-hyperlink') || '');

        if (typeof el.onclick === 'function') {

            try {

                code += Function.prototype.toString.call(el.onclick);

            } catch (e) {

            }

        }

        return code;

    }



    function findTipArgs(el) {

        if (!el) {

            return null;

        }

        if (el[ARGS_FLAG]) {

            return el[ARGS_FLAG];

        }

        var args = parseTipArgsFromCode(readCodeFromEl(el));

        if (args && args.content != null) {

            el[ARGS_FLAG] = args;

            return args;

        }

        var parent = el.parentNode;

        var hops = 0;

        while (parent && hops < 5) {

            if (parent[ARGS_FLAG]) {

                return parent[ARGS_FLAG];

            }

            var pArgs = parseTipArgsFromCode(readCodeFromEl(parent));

            if (pArgs && pArgs.content != null) {

                parent[ARGS_FLAG] = pArgs;

                return pArgs;

            }

            if (parent.querySelector) {

                var link = parent.querySelector('.linkspan, [onclick*="FR_INDICATOR_TIP"]');

                if (link) {

                    var lArgs = parseTipArgsFromCode(readCodeFromEl(link));

                    if (lArgs && lArgs.content != null) {

                        parent[ARGS_FLAG] = lArgs;

                        return lArgs;

                    }

                }

            }

            parent = parent.parentNode;

            hops++;

        }

        return null;

    }



    function bindHoverElement(hostEl, args) {

        if (!hostEl || hostEl[BOUND_FLAG]) {

            return;

        }

        if (!args || args.content == null || String(args.content) === '') {

            return;

        }

        hostEl[BOUND_FLAG] = true;

        hostEl[ARGS_FLAG] = args;

        hostEl.style.cursor = 'help';



        hostEl.addEventListener('mouseenter', function () {

            showIndicatorTip(args, null, hostEl);

        });

        hostEl.addEventListener('mouseleave', function (e) {

            var related = e.relatedTarget;

            if (related && hostEl.contains && hostEl.contains(related)) {

                return;

            }

            // 移入提示框（复制场景）时不立刻隐藏

            if (related && activeTip && (related === activeTip || (activeTip.contains && activeTip.contains(related)))) {

                return;

            }

            scheduleHide();

        });

        hostEl.addEventListener('click', function (e) {

            e.preventDefault();

            e.stopPropagation();

            if (typeof e.stopImmediatePropagation === 'function') {

                e.stopImmediatePropagation();

            }

            return false;

        }, true);

    }



    function scanAndBind() {

        var candidates = document.querySelectorAll(

            '.linkspan, [onclick*="FR_INDICATOR_TIP"], [onclick*="showIndicatorTip"]'

        );

        for (var i = 0; i < candidates.length; i++) {

            var el = candidates[i];

            var args = findTipArgs(el);

            if (!args) {

                continue;

            }

            var host = resolveAnchorEl(el);

            if (!host) {

                continue;

            }

            host[ARGS_FLAG] = args;

            bindHoverElement(host, args);

        }

    }



    function scheduleScan() {

        if (SCAN_TIMER) {

            clearTimeout(SCAN_TIMER);

        }

        SCAN_TIMER = setTimeout(function () {

            SCAN_TIMER = null;

            scanAndBind();

        }, 120);

    }



    function boot() {

        scheduleScan();

        setTimeout(scanAndBind, 400);

        setTimeout(scanAndBind, 1200);

        setTimeout(scanAndBind, 3000);



        if (global.MutationObserver && document.body) {

            var observer = new MutationObserver(function () {

                scheduleScan();

            });

            observer.observe(document.body, { childList: true, subtree: true });

        }



        global.addEventListener('scroll', function () {

            removeTip();

        }, true);

        global.addEventListener('resize', function () {

            removeTip();

        });

    }



    global.showIndicatorTip = function (args, evt) {

        var el = (evt && (evt.currentTarget || evt.target)) || null;

        showIndicatorTip(args, evt, el);

    };

    global.FR = global.FR || {};

    global.FR.showIndicatorTip = global.showIndicatorTip;



    if (document.readyState === 'loading') {

        document.addEventListener('DOMContentLoaded', boot);

    } else {

        boot();

    }

})(window);

