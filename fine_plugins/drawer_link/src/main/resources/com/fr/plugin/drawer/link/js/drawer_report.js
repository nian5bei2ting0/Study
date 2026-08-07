!(function (global) {
    'use strict';

    var MASK_CLASS = 'fr-drawer-mask';
    var PANEL_CLASS = 'fr-drawer-panel';
    var OPEN_CLASS = 'fr-drawer-open';
    var ESC_HANDLER_KEY = '__frDrawerEscHandler__';

    function normalizeSize(size) {
        if (typeof size === 'number' && isFinite(size) && size > 0) {
            return Math.min(Math.floor(size), 10000) + 'px';
        }
        var text = String(size == null ? '' : size).trim();
        var match = text.match(/^(\d+(?:\.\d+)?)(px|%|vh|vw)?$/i);
        if (match) {
            var num = parseFloat(match[1]);
            if (isFinite(num) && num > 0 && num <= 10000) {
                return num + (match[2] || 'px');
            }
        }
        return '400px';
    }

    function buildQuery(params) {
        if (!params) {
            return '';
        }
        var parts = [];
        for (var key in params) {
            if (!Object.prototype.hasOwnProperty.call(params, key)) {
                continue;
            }
            var value = params[key];
            if (value === undefined || value === null) {
                continue;
            }
            if (typeof value === 'object') {
                try {
                    value = JSON.stringify(value);
                } catch (e) {
                    continue;
                }
            }
            parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(String(value)));
        }
        return parts.length ? ('&' + parts.join('&')) : '';
    }

    function resolveUrl(templateUrl, params) {
        var url = templateUrl || '';
        var query = buildQuery(params);
        if (!query) {
            return url;
        }
        if (url.indexOf('?') === -1) {
            return url + '?' + query.substring(1);
        }
        return url + query;
    }

    function removeDrawerImmediate(mask) {
        if (!mask) {
            return;
        }
        if (mask.__frDrawerTimer) {
            clearTimeout(mask.__frDrawerTimer);
            mask.__frDrawerTimer = null;
        }
        if (mask.parentNode) {
            mask.parentNode.removeChild(mask);
        }
        unbindEscIfNeeded();
    }

    function closeDrawer(mask) {
        if (!mask) {
            return;
        }
        var panel = mask.querySelector('.' + PANEL_CLASS);
        if (panel) {
            panel.classList.remove(OPEN_CLASS);
        }
        mask.classList.remove(OPEN_CLASS);
        if (mask.__frDrawerTimer) {
            clearTimeout(mask.__frDrawerTimer);
        }
        mask.__frDrawerTimer = setTimeout(function () {
            removeDrawerImmediate(mask);
        }, 280);
    }

    function unbindEscIfNeeded() {
        if (!document.querySelector('.' + MASK_CLASS) && global[ESC_HANDLER_KEY]) {
            document.removeEventListener('keydown', global[ESC_HANDLER_KEY], true);
            global[ESC_HANDLER_KEY] = null;
        }
    }

    function bindEsc(mask) {
        if (global[ESC_HANDLER_KEY]) {
            document.removeEventListener('keydown', global[ESC_HANDLER_KEY], true);
        }
        global[ESC_HANDLER_KEY] = function (evt) {
            var key = evt.key || evt.keyCode;
            if (key === 'Escape' || key === 'Esc' || key === 27) {
                closeDrawer(mask);
            }
        };
        document.addEventListener('keydown', global[ESC_HANDLER_KEY], true);
    }

    function loadReportIntoIframe(iframe, args) {
        var url = args.templateUrl || '';
        var params = args.params || {};
        var iframeName = 'fr-drawer-iframe-' + Date.now();
        iframe.setAttribute('name', iframeName);
        iframe.setAttribute('id', iframeName);
        iframe.name = iframeName;

        // 不能调用 FR.doHyperlinkByGet*：自定义 target 会被当成新窗口打开
        if (args.byPost) {
            loadByPostForm(iframe, url, params, iframeName);
        } else {
            iframe.src = resolveUrl(url, params);
        }
    }

    function escapeHtmlAttr(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function loadByPostForm(iframe, url, params, iframeName) {
        // 在 iframe 文档内生成并提交表单，导航一定留在抽屉 iframe 内
        // （外层 form[target=iframeName] 在部分环境下会找不到 frame 从而新开页）
        var submitted = false;

        function writeInnerFormAndSubmit() {
            if (submitted) {
                return;
            }
            submitted = true;
            try {
                var win = iframe.contentWindow;
                var doc = win && (iframe.contentDocument || win.document);
                if (!doc) {
                    throw new Error('iframe document unavailable');
                }
                try {
                    win.name = iframeName;
                } catch (ignore) {
                }

                var fields = [];
                var paramJson;
                try {
                    paramJson = typeof params === 'string' ? params : JSON.stringify(params || {});
                } catch (e) {
                    paramJson = '{}';
                }
                fields.push('<input type="hidden" name="__parameters__" value="' + escapeHtmlAttr(paramJson) + '"/>');

                if (params && typeof params === 'object') {
                    for (var key in params) {
                        if (!Object.prototype.hasOwnProperty.call(params, key) || key === '__parameters__') {
                            continue;
                        }
                        var value = params[key];
                        if (value === undefined || value === null || typeof value === 'object') {
                            continue;
                        }
                        fields.push(
                            '<input type="hidden" name="' + escapeHtmlAttr(key) +
                            '" value="' + escapeHtmlAttr(String(value)) + '"/>'
                        );
                    }
                }

                doc.open('text/html', 'replace');
                doc.write(
                    '<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>' +
                    '<form id="fr-drawer-post-form" method="POST" accept-charset="UTF-8" action="' +
                    escapeHtmlAttr(url) + '">' + fields.join('') + '</form>' +
                    '<script>document.getElementById("fr-drawer-post-form").submit();<\/script>' +
                    '</body></html>'
                );
                doc.close();
            } catch (err) {
                // 极端情况下退回外层 form target（仍可能新开页，但优于完全失败）
                fallbackOuterPost(iframe, url, params, iframeName);
            }
        }

        iframe.addEventListener('load', function onBlank() {
            iframe.removeEventListener('load', onBlank);
            writeInnerFormAndSubmit();
        });
        iframe.src = 'about:blank';
        setTimeout(function () {
            if (!submitted) {
                writeInnerFormAndSubmit();
            }
        }, 300);
    }

    function fallbackOuterPost(iframe, url, params, iframeName) {
        var form = document.createElement('form');
        form.method = 'POST';
        form.action = url;
        form.target = iframeName;
        form.acceptCharset = 'UTF-8';
        form.style.cssText = 'position:absolute;left:-9999px;top:-9999px;';

        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = '__parameters__';
        try {
            input.value = typeof params === 'string' ? params : JSON.stringify(params || {});
        } catch (e) {
            input.value = '{}';
        }
        form.appendChild(input);

        (iframe.parentNode || document.body).appendChild(form);
        try {
            if (iframe.contentWindow) {
                iframe.contentWindow.name = iframeName;
            }
        } catch (e) {
        }
        form.submit();
        setTimeout(function () {
            if (form.parentNode) {
                form.parentNode.removeChild(form);
            }
        }, 1500);
    }

    /**
     * @param {Object} args
     * @param {string} args.title
     * @param {string} args.templateUrl
     * @param {string} args.position left|right|top|bottom
     * @param {string|number} args.size 如 400px
     * @param {Object} [args.params]
     * @param {boolean} [args.byPost]
     */
    function showTemplateByDrawer(args) {
        args = args || {};

        var position = (args.position || args.positon || 'right').toLowerCase();
        if (['left', 'right', 'top', 'bottom'].indexOf(position) < 0) {
            position = 'right';
        }

        var size = normalizeSize(args.size);
        var templateUrl = args.templateUrl || '';
        if (!templateUrl) {
            if (global.FR && typeof global.FR.Msg === 'object' && typeof global.FR.Msg.toast === 'function') {
                global.FR.Msg.toast('抽屉报表路径为空');
            }
            return;
        }

        // 连点：立即移除旧抽屉，避免动画重叠
        var existingList = document.querySelectorAll('.' + MASK_CLASS);
        for (var i = 0; i < existingList.length; i++) {
            removeDrawerImmediate(existingList[i]);
        }

        var mask = document.createElement('div');
        mask.className = MASK_CLASS + ' fr-drawer-' + position;

        var panel = document.createElement('div');
        panel.className = PANEL_CLASS + ' fr-drawer-' + position;

        if (position === 'left' || position === 'right') {
            panel.style.width = size;
            panel.style.height = '100%';
        } else {
            panel.style.height = size;
            panel.style.width = '100%';
        }

        var header = document.createElement('div');
        header.className = 'fr-drawer-header';

        var titleEl = document.createElement('div');
        titleEl.className = 'fr-drawer-title';
        // 仅纯文本，避免 XSS
        titleEl.textContent = args.title == null ? '' : String(args.title);

        var closeBtn = document.createElement('span');
        closeBtn.className = 'fr-drawer-close';
        closeBtn.appendChild(document.createTextNode('\u00d7'));
        closeBtn.title = '关闭';
        closeBtn.onclick = function () {
            closeDrawer(mask);
        };

        header.appendChild(titleEl);
        header.appendChild(closeBtn);

        var body = document.createElement('div');
        body.className = 'fr-drawer-body';

        var iframe = document.createElement('iframe');
        iframe.className = 'fr-drawer-iframe';
        iframe.setAttribute('frameborder', '0');
        iframe.setAttribute('title', args.title == null ? 'drawer-report' : String(args.title));

        body.appendChild(iframe);
        panel.appendChild(header);
        panel.appendChild(body);
        mask.appendChild(panel);

        mask.addEventListener('click', function (evt) {
            if (evt.target === mask) {
                closeDrawer(mask);
            }
        });

        document.body.appendChild(mask);
        bindEsc(mask);
        loadReportIntoIframe(iframe, args);

        setTimeout(function () {
            mask.classList.add(OPEN_CLASS);
            panel.classList.add(OPEN_CLASS);
        }, 20);
    }

    // 挂到 FR 上，绝不整体替换 FR
    global.FR = global.FR || {};
    global.FR.showTemplateByDrawer = showTemplateByDrawer;
    global.showTemplateByDrawer = showTemplateByDrawer;
})(window);
