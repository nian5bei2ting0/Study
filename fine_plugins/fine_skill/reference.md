# FineReport 插件开发 — 参考

## 环境与依赖

| 项 | 值 |
|----|-----|
| 产品 | FineReport / 决策平台 **11.0**（`env-version` 写 `11.0~11.0`） |
| JDK | 1.8（source/target=1.8） |
| 构建 | Maven 3.8+：`mvn -DskipTests package` |
| 共用 JAR | `E:\AI\cursor\fine_plugins\fine_jar\` |
| 工程内 JAR | 各插件下 `finereport11/`（`fr.lib.dir`） |
| jartime | 与本地 `fine-*-11.0.jar` 家族对齐，样例多为 `2023-08-08` |

覆盖依赖目录：

```bash
mvn -DskipTests -Dfr.lib.dir=E:/AI/cursor/fine_plugins/fine_jar package
```

运行时由 FineReport 提供 FR 类；插件 zip **只含** 业务 jar + `plugin.xml`。

## 标准目录

```
your_plugin/
├── finereport11/           # 或引用 fine_jar
├── plugin.xml              # 必须在工程根，与 jar 一并打包
├── pom.xml
├── src/main/java/com/fr/plugin/<domain>/
│   ├── core/               # ConfigStore、PluginPaths、鉴权、业务
│   ├── decision/           # SystemOption、Client、Bridge、Controller
│   ├── locale/
│   ├── web/                # WebService、JS/CSS Handler
│   ├── filter/             # GlobalRequestFilter
│   └── design/             # Hyperlink（designer）
└── src/main/resources/com/fr/plugin/<domain>/
    ├── locale/*.properties
    ├── decision/bundle.js
    └── web/*.js|*.css|homepage/**
```

## plugin.xml 扩展块

| 块 | 元素 | 用途 |
|----|------|------|
| `extra-core` | LocaleFinder, WebService | i18n；ReportServer op |
| `extra-decision` | SystemOption, WebResource, ControllerRegister | 管理系统 UI + REST |
| 同上 | GlobalRequestFilterProvider | HTTP 拦截 |
| 同上 | DecisionInitEventProvider | 启动部署/挂目录 |
| `extra-report` | JS/CSS Handler, DataModel*Processor | 预览注入 / 取数 Hook |
| `extra-designer` | HyperlinkProvider | 设计器超链接 |
| `function-recorder` | 通常指向 SystemOption | 功能点登记 |
| `plugin-xml-i18n` | resource + name/description keys | 插件元数据 i18n |

### 样例插件 ID

| 工程 | plugin id |
|------|-----------|
| ip_white | `com.fr.plugin.ip.white` |
| sql_debug | `com.fr.plugin.report.debug.assistant` |
| czcb_homepage | `com.fr.plugin.czcb.homepage` |
| other_sys_sso | `com.fr.plugin.other.sys.sso` |
| online_save | `com.fr.plugin.online.save` |
| drawer_link | `com.fr.plugin.drawer.link` |

包名通常与 id 一致；sql_debug 的 `main-package` 为较短的 `com.fr.plugin.report.debug`。

## 决策平台 ID 契约

锁定后全链路一致：

| 项 | 必须一致的位置 |
|----|----------------|
| SystemOption.id | Java `id()`、bundle `navigation.id` |
| cardType | `BI.shortcut` 名、navigation `cardType`、OptionClient 若暴露 parser 名 |
| ScriptPath | `/com/fr/plugin/.../decision/bundle.js` ↔ resources 路径 |
| i18n 键 | `displayName()`、`plugin-xml-i18n`、`BI.i18nText` |
| REST 前缀 | `@RequestMapping` ↔ `Dec.reqGet/Post` |

`parentId` / `fullPath` 管理系统下均为 `decision-management-root`；`sortIndex` 建议 ≥ 2035。

## 运行时挂载模式

### A. GlobalRequestFilter（ip_white）

- `urlPatterns = {"/*"}`，内部快速判断是否目标预览 URI，非目标立即放行。
- 敏感路径建议 fail-closed。
- 可配合挂在 `DecisionReportComponent` 的 `report.js` 处理 AJAX 403。

### B. extra-report（sql_debug）

- JS/CSS Handler：配置关闭时返回空 paths。
- Processor 记录取数；无权限用户跳过登记。
- 预览 JS：多 URL 探测 ReportServer；失败可走决策 REST 备用。
- op **只**挂在 ReportServer；勿打 `/decision/view/report`。

### C. 静态站 + InitEvent（czcb_homepage）

1. ResourceDeployer：classpath `web/homepage/**` → `webroot/help/<name>/`
2. InitEvent.before：ensureDeployed + ensureDirectoryLink
3. EntryService.addLink 挂目录
4. WebService 供首页匿名读菜单；写接口走管理端管理员鉴权
5. 升级后需「重新部署静态页」

### D. 设计器 Hyperlink（drawer_link）

- `AbstractHyperlinkProvider` + pane + 运行时超链接类 + 预览 JS
- 依赖 `fine-report-designer`；改扩展后需重启设计器

### E. 仅 API（online_save）

- LocaleFinder + ControllerRegister，无 SystemOption/WebResource

## 鉴权分层

```
管理读  → @VisitRefer(true)
管理写  → VisitRefer + isAdmin(userId)
预览业务 → 独立白名单 / session；与管理 API 隔离
过滤器豁免超管 → 独立开关 + 管理员判断
```

管理员判断：

```text
LoginService 取用户名 → UserService 取 userId → isAdmin(userId)
兜底：超管名 admin；显示名「张三(zhangsan)」取括号内登录名
```

参考：`IpWhiteService.isCurrentUserAdmin`、`CzcbHomepageAdminAuth`。

错误约定：

```json
{ "status": "error", "errorMsg": "login required", "errorCode": "unauthorized" }
{ "status": "error", "errorMsg": "admin required", "errorCode": "forbidden" }
```

## 配置持久化

```
WEB-INF/plugins/plugin-{id}-{version}/
  ├── xxx.jar
  ├── plugin.xml
  ├── *.properties
  └── *.json / *.dat
```

| 工程 | 文件 |
|------|------|
| ip_white | `ip-white.properties` + `sensitive-reports.json` |
| sql_debug | `debug-assistant.properties` + `authorized-users.dat` |
| czcb_homepage | `czcb-homepage.properties` + `nav-menu-config.json` |

保存时递增 `configVersion`；预览有缓存时一并清空。

## 前端要点

| 主题 | 规则 |
|------|------|
| 配置 IO | `Dec.reqGet` / `Dec.reqPost` 相对路径 |
| 授权用户 | 优先 `dec.case.platform.user`；提交 `显示名(用户名)` |
| 磁盘格式 | `用户名\|显示名(用户名)`；比对用括号内登录名 |
| BI.map | 回调 `(index, value)` |
| iframe 配置页 | 保存走 `parent.Dec.reqPost` |
| 预览 API | 探测多 ReportServer base；可选决策 REST 备用 |

## 打包安装升级

1. `pom` / `plugin.xml` 版本与 change-notes 同步
2. `mvn clean package` → 安装 `target/plugin-{id}-{version}.zip`
3. Filter / WebService / designer 变更后重启
4. 菜单/权限缓存异常时：禁用再启用插件
5. 平台会保留旧版本目录；确认新版后删旧
6. 静态页插件：升级后强制 redeploy

设计器插件另检查 `%FineReport_HOME%\plugins\`。

## 本地样例绝对路径索引

| 模式 | 路径 |
|------|------|
| 跨项目手册 | `E:\AI\cursor\fine_plugins\docs\帆软插件开发手册.md` |
| sql_debug 深手册 | `E:\AI\cursor\fine_plugins\sql_debug-1.0.1\docs\帆软插件开发技能手册.md` |
| AI 提示词 | `E:\AI\cursor\fine_plugins\sql_debug-1.0.1\DEV_AGENT_PROMPT.md` |
| 干净骨架 plugin.xml | `...\ip_white\plugin.xml` |
| antrun pom | `...\ip_white\pom.xml` |
| SystemOption | `...\ip_white\...\IpWhiteSystemOption.java` |
| OptionClient | `...\ip_white\...\IpWhiteOptionClient.java` |
| Filter | `...\ip_white\...\SensitiveReportIpFilter.java` |
| 完整预览插件 | `...\sql_debug-1.0.1\plugin.xml` |
| WebService | `...\sql_debug-1.0.1\...\ReportDebugWebService.java` |
| InitEvent | `...\czcb_homepage\...\CzcbHomepageInitEvent.java` |
| Hyperlink | `...\drawer_link\...\DrawerReportHyperlinkProvider.java` |
| 共用 JAR | `E:\AI\cursor\fine_plugins\fine_jar\` |
