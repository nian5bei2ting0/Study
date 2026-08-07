package com.fr.plugin.drawer.link;

import com.fr.general.GeneralUtils;
import com.fr.json.JSONObject;
import com.fr.js.ReportletHyperlink;
import com.fr.js.ReportletHyperlinkDialogAttr;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.FormulaProvider;
import com.fr.stable.StringUtils;
import com.fr.stable.web.Repository;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抽屉报表超级链接：以前端抽屉方式打开网络报表。
 * <p>
 * 方向编码存放在 targetFrame（前缀 {@link #TARGET_PREFIX}），
 * 尺寸复用 width/height（两侧同步写入）。
 * 抽屉标题保存在 {@link ReportletHyperlinkDialogAttr} 中（可随模板持久化），
 * 不能使用 {@link #getTitle()}：运行时会被超链列表名（如“抽屉报表1”）覆盖。
 */
public class DrawerReportHyperlink extends ReportletHyperlink {

    public static final String TARGET_PREFIX = "_drawer_";
    public static final int DEFAULT_SIZE = 400;
    public static final int MAX_SIZE = 10000;

    private static final Pattern TEMPLATE_PATH_PATTERN = Pattern.compile(
            "^(.*\\.(?i)(?:cpt|frm|fvs|xlsx?))(?:[?&](.*))?$");

    public DrawerReportHyperlink() {
        super();
        setDrawerDirection(DrawerDirection.RIGHT);
        setWidth(DEFAULT_SIZE);
        setHeight(DEFAULT_SIZE);
        setShowParameterInterface(false);
        setByPost(false);
        if (getAttr() == null) {
            setAttr(new ReportletHyperlinkDialogAttr());
        }
    }

    public DrawerDirection getDrawerDirection() {
        String target = getTargetFrame();
        if (StringUtils.isNotBlank(target) && target.startsWith(TARGET_PREFIX)) {
            return DrawerDirection.fromCode(target.substring(TARGET_PREFIX.length()));
        }
        return DrawerDirection.RIGHT;
    }

    public void setDrawerDirection(DrawerDirection direction) {
        if (direction == null) {
            direction = DrawerDirection.RIGHT;
        }
        setTargetFrame(TARGET_PREFIX + direction.getCode());
    }

    public int getDrawerSize() {
        DrawerDirection direction = getDrawerDirection();
        int size;
        if (direction.isHorizontal()) {
            size = getWidth() > 0 ? getWidth() : getHeight();
        } else {
            size = getHeight() > 0 ? getHeight() : getWidth();
        }
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public void setDrawerSize(int size) {
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        // 宽高同步，切换方向时尺寸一致
        setWidth(size);
        setHeight(size);
    }

    /**
     * 配置界面中的抽屉标题（持久化到 Attr）。
     */
    public String getDrawerTitle() {
        ReportletHyperlinkDialogAttr attr = getAttr();
        if (attr == null || attr.getTitle() == null) {
            return "";
        }
        Object title = attr.getTitle();
        if (title instanceof FormulaProvider) {
            Object result = ((FormulaProvider) title).getResult();
            return result == null ? "" : GeneralUtils.objectToString(result);
        }
        return GeneralUtils.objectToString(title);
    }

    public void setDrawerTitle(String title) {
        ReportletHyperlinkDialogAttr attr = getAttr();
        if (attr == null) {
            attr = new ReportletHyperlinkDialogAttr();
            setAttr(attr);
        }
        attr.setTitle(title == null ? "" : title);
    }

    @Override
    public String actionJS(Repository repo) {
        try {
            String rawPath = getReportletPath();
            if (StringUtils.isBlank(rawPath)) {
                return "FR.showTemplateByDrawer({title:'',templateUrl:'',position:'right',size:'400px',params:{}});";
            }

            PathParts pathParts = splitReportPath(rawPath);
            String url = createLetUrl(pathParts.path, repo);
            JSONObject para = createPara(repo);
            mergeQueryParams(para, pathParts.query);

            JSONObject args = JSONObject.create();
            args.put("title", getDrawerTitle());
            args.put("templateUrl", url);
            args.put("position", getDrawerDirection().getCode());
            args.put("size", getDrawerSize() + "px");
            args.put("params", para);
            args.put("byPost", isByPost());
            return "FR.showTemplateByDrawer(" + args.toString() + ");";
        } catch (Exception e) {
            FineLoggerFactory.getLogger().error(e.getMessage(), e);
            return "";
        }
    }

    @Override
    protected String getHyperlinkType() {
        return "drawer";
    }

    private String createLetUrl(String reportPath, Repository repo) {
        if (StringUtils.isBlank(reportPath)) {
            return "";
        }
        return repo.checkoutObject(reportPath, resolveLetType(reportPath));
    }

    private String resolveLetType(String reportPath) {
        String lower = reportPath.toLowerCase();
        if (lower.contains(".frm")) {
            return "formlet";
        }
        if (lower.contains(".fvs")) {
            return "duchamp";
        }
        return "reportlet";
    }

    /**
     * 拆分类似 {@code /a.cpt&__bypagesize__=false} 或 {@code /a.cpt?x=1} 的路径。
     */
    public static PathParts splitReportPath(String reportPath) {
        PathParts parts = new PathParts();
        if (StringUtils.isBlank(reportPath)) {
            parts.path = "";
            parts.query = "";
            return parts;
        }
        String normalized = reportPath.trim().replace('\\', '/');
        Matcher matcher = TEMPLATE_PATH_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            parts.path = matcher.group(1);
            parts.query = matcher.group(2) == null ? "" : matcher.group(2);
        } else {
            int q = normalized.indexOf('?');
            if (q >= 0) {
                parts.path = normalized.substring(0, q);
                parts.query = normalized.substring(q + 1);
            } else {
                parts.path = normalized;
                parts.query = "";
            }
        }
        return parts;
    }

    private void mergeQueryParams(JSONObject para, String query) throws Exception {
        if (StringUtils.isBlank(query) || para == null) {
            return;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (StringUtils.isBlank(pair)) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key;
            String value;
            if (eq < 0) {
                key = decode(pair);
                value = "";
            } else {
                key = decode(pair.substring(0, eq));
                value = decode(pair.substring(eq + 1));
            }
            if (StringUtils.isNotBlank(key) && !para.has(key)) {
                para.put(key, value);
            }
        }
    }

    private String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof DrawerReportHyperlink;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static class PathParts {
        public String path;
        public String query;
    }
}
