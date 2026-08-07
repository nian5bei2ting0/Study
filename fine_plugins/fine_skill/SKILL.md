---
name: fine-plugin-dev
description: >-
  Develop FineReport / FineDecision (帆软) 11.x local plugins: plugin.xml,
  Maven packaging, decision management UI triad, LocaleFinder i18n, REST
  Controllers, ReportServer WebService, GlobalRequestFilter, extra-report
  JS/CSS handlers, designer HyperlinkProvider, config persistence beside JAR.
  Use when creating or modifying 帆软插件, FineReport plugins, plugin.xml,
  finereport11 dependencies, decision bundle.js, or deploying plugin-*.zip.
---

# FineReport 11 插件开发

目标环境：**FineReport / 决策平台 11.0**，JDK **1.8**。本地依赖 JAR 在工程 `finereport11/` 或共用 `fine_plugins/fine_jar/`。

本地样例工程根：`E:\AI\cursor\fine_plugins\`（`ip_white`、`czcb_homepage`、`sql_debug-1.0.1`、`other_sys_sso`、`online_save`、`drawer_link` 等）。

## When to use

- 新建或改造帆软本地插件
- 编写/修改 `plugin.xml`、`pom.xml`、决策三件套、预览注入、过滤器、WebService
- 排查侧栏菜单、i18n 乱码、op 404、配置落盘路径等问题

详细模板见 [templates.md](templates.md)；扩展点与 API 见 [reference.md](reference.md)；排错见 [troubleshooting.md](troubleshooting.md)。

## 抄哪个项目

| 需求 | 优先参考 |
|------|----------|
| 管理系统配置页 + REST 存盘（干净骨架） | `ip_white` |
| 管理员写配置 / IP 预览拦截 | `ip_white` |
| 预览浮层 + 数据集/SQL Hook | `sql_debug-1.0.1` |
| 静态大屏 + 目录管理入口 | `czcb_homepage` |
| 仅 REST、无管理 UI | `online_save` |
| SSO / 自定义 ReportServer op | `other_sys_sso` |
| 设计器超链接 | `drawer_link` |
| Maven 一键 zip | `ip_white` / `czcb_homepage` antrun |

## 扩展点选型

```
需要管理系统配置页？
  → extra-decision 三件套 + decision/bundle.js

需要拦截全部 HTTP（预览前拒绝）？
  → GlobalRequestFilterProvider（ip_white）

需要预览页注入 JS/CSS？
  → extra-report JavaScriptFileHandler / CssFileHandler

需要 Hook 数据集取数？
  → SessionCachedDataModelProcessor / DataModelFillProcessor

需要自定义 HTTP（预览可调）？
  → extra-core WebService → ReportServer?op=&cmd=

需要静态站 + 目录入口？
  → DecisionInitEvent + 静态部署 + EntryService.addLink

需要设计器超链接？
  → extra-designer HyperlinkProvider

需要中文名 / 权限树名称？
  → LocaleFinder + plugin-xml-i18n + \u 转义 properties（无 BOM）
```

## 架构分层

```
plugin.xml
├── locale/     LocaleFinder + *.properties
├── decision/   SystemOption + OptionClient + Bridge + Controller + bundle.js
├── web/        WebService / JS·CSS Handler（按需）
├── filter/     GlobalRequestFilter（按需）
└── core/       业务、ConfigStore、PluginPaths、鉴权（不依赖 Spring/BI）
```

- **core** 可被 web 与 decision 共用；不依赖 Spring。
- **decision** REST：校验参数 + 调 core；管理鉴权用 `@VisitRefer`，写接口再加管理员校验。
- **web** 预览接口：自己做 session/用户校验，勿用管理白名单拦管理 API。

## 新建插件流程

复制此清单并勾选：

```
Task Progress:
- [ ] 1. 定身份：plugin.id / 包名 / OPTION_ID / cardType / i18n 键
- [ ] 2. 准备 finereport11/*.jar（或指向 fine_jar）
- [ ] 3. plugin.xml + pom.xml（版本一致）+ antrun 打 zip
- [ ] 4. LocaleFinder + properties（\u，无 BOM）
- [ ] 5. 决策三件套 + 空 bundle.js（侧栏能打开）
- [ ] 6. ConfigResource get/save + PluginPaths + ConfigStore
- [ ] 7. 按需加 Filter / WebService / extra-report / InitEvent
- [ ] 8. mvn -DskipTests package → 安装 zip → 重启验证
```

### 身份表（先定死）

| 项 | 示例 |
|----|------|
| `plugin.id` | `com.fr.plugin.ip.white` |
| `main-package` | `com.fr.plugin.ip.white` |
| SystemOption.id | `decision-management-ip-white` |
| BI.shortcut / cardType | `dec.management.plugin.ip.white` |
| i18n 名称键 | `Fine-Plugin_Ip_White` |
| REST 前缀 | `/ip/white/config` |

### 决策三件套（缺一不可）

```xml
<extra-decision>
  <SystemOptionProvider class="...XxxSystemOption"/>
  <WebResourceProvider class="...XxxWebResourceBridge"/>
  <ControllerRegisterProvider class="...XxxControllerRegister"/>
</extra-decision>
```

缺 WebResource 或 bundle 未 `BI.config` navigation → **权限树有、侧栏无**。

`displayName()` 必须返回 **i18n 键**，禁止 `getLocText()` / 直接中文。

### Maven 要点

```xml
<fr.lib.dir>${project.basedir}/finereport11</fr.lib.dir>
<!-- 依赖 scope=system + systemPath；fine-third 仅编译，勿打进包 -->
```

构建：`mvn -DskipTests package` → `target/plugin-{id}-{version}.zip`。

### 配置落盘

写在 **插件 JAR 同级目录**（`WEB-INF/plugins/plugin-{id}-{version}/`）。用 `ProtectionDomain` / CodeSource 解析；**不要**只用 `StableUtils.getInstallHome()`（常落到 Tomcat `bin`）。

### 鉴权速查

| 场景 | 做法 |
|------|------|
| 管理读配置 | `@VisitRefer(required=true)` |
| 管理写配置 | VisitRefer **+** `UserService.isAdmin(userId)`（参数是 userId，不是用户名） |
| 预览业务 | 独立白名单/会话；**禁止**用预览白名单拦管理 REST |

### WebService 易错点

- `process` 第三参是 **op**，`cmd` 从 request 参数读。
- 调用 `{prefix}/ReportServer?op=xxx&cmd=yyy`；**禁止** `/decision/view/report?op=`（404）。
- prefix 因部署而异：探测 `/webroot/ReportServer`、`/ReportServer` 等。

### 前端 bundle 必注册

```js
BI.shortcut("dec.management.plugin.xxx", XxxManagement);
BI.config("dec.constant.management.navigation", function (items) {
  items.push({
    id: "decision-management-xxx",       // = SystemOption.id()
    cardType: "dec.management.plugin.xxx",
    text: BI.i18nText("Fine-Plugin_Xxx"),
    cls: "management-log-font"
  });
  return items;
});
```

`BI.map(arr, fn)` 回调是 `(index, value)`，不是 `(value)`。

授权用户优先 `dec.case.platform.user`；磁盘建议 `用户名|显示名(用户名)`。

## 修改守则

- 只改与需求相关的文件；版本号同步 `pom.xml` ↔ `plugin.xml` ↔ change-notes / jartime。
- 新增配置项：同步 Store、REST GET/POST、bundle.js（或静态页）、真正消费逻辑。
- Spring 注解用 `com.fr.third.springframework.*`（fine-third）。
- 只依赖公开 API；勿引用产品 JAR 中混淆的 `com.fr.plugin.A.*`。
- 异常与敏感路径不要回传前端。

## 发版检查

- [ ] `mvn package` 通过；JAR 不含 fine-third / FR 运行时 JAR
- [ ] 插件管理显示中文名（非 i18n 键）
- [ ] 侧栏菜单可开；配置可读写；写接口非管理员拒绝
- [ ] 业务路径（拦截 / 浮层 / 静态页）按预期
- [ ] 升级后旧 `plugin-*-旧版本` 目录可清理；静态页插件需重新部署
- [ ] `basic.log` 无 Can't find bundle

## Additional resources

- [templates.md](templates.md) — plugin.xml / pom / Java / bundle 骨架
- [reference.md](reference.md) — 扩展点、依赖、样例路径索引
- [troubleshooting.md](troubleshooting.md) — 现象 → 原因 → 对策
