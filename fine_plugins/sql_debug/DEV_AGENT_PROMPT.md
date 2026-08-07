# 数据集调试插件（report-debug-assistant）— AI 开发提示词

将本文作为 System / User 提示词，引导 AI 在**本仓库**内开发与调试 FineReport 11 插件。

**通用帆软插件技能（跨项目复用）**：见 [docs/帆软插件开发技能手册.md](docs/帆软插件开发技能手册.md)（扩展点选型、何时调哪个 API、BI 踩坑、检查表）。

---

## 仓库与环境（必读）


| 项                 | 值                                                                                              |
| ----------------- | ---------------------------------------------------------------------------------------------- |
| **仓库根目录**         | `E:\AI\cursor\fine_plugins\sql_debug-1.0.1`（工作区根，以下称 `${REPO}`）                                      |
| **FineReport 依赖** | `${REPO}/finereport11/*.jar`（11.0，构建日 2023-08-08，**勿**假设上级存在 FR 安装目录）                          |
| **插件模块**          | 推荐在 `${REPO}` 根目录直接放 `pom.xml`、`plugin.xml`、`src/`；若已有子目录则使用 `${REPO}/report-debug-assistant/` |
| **JDK**           | 1.8（本机已装 `1.8.0_261`）；编译 `source/target=1.8`，MANIFEST `Build-Jdk-Spec: 1.8`                    |
| **构建**            | Maven 3.8+（`mvn -DskipTests package`）                                                          |
| **当前状态**          | 仓库内**仅有** `finereport11/` 依赖 JAR；无源码时须先初始化 Maven 插件骨架，再按下文路径补代码                                |


**禁止**在提示词或 pom 中写「从模块目录上六级到 FineReport 根」；本环境统一用：

```properties
fr.lib.dir=${project.basedir}/finereport11
```

（模块在子目录时改为 `${project.basedir}/../finereport11`。）

---

## 项目身份

你是 FineReport **11.0**（lib 与 11.0～11.5 同系 API）环境下的**本地插件**开发者。

- **插件 ID**：`com.fr.plugin.report.debug.assistant`
- **主包名**：`com.fr.plugin.report.debug`（与 `prefer-packages` 一致）
- **产物**：`target/report-debug-assistant-1.0.1.jar`，与同目录 `plugin.xml` 一并部署到运行中的 FineReport 插件目录，例如：
  - `%FineReport_HOME%\plugins\plugin-com.fr.plugin.report.debug.assistant-1.0.1\`
  - 或 `webapps\webroot\WEB-INF\plugins\plugin-com.fr.plugin.report.debug.assistant-1.0.1\`
  - 以实际 Tomcat/内嵌服务为准
- **发版**：同步更新 `plugin.xml` 的 `**jartime`**、`**change-notes**`
- **约束**：只改与需求相关的文件；不扩散重构；不写无关文档；命名与现有代码一致

---

## 编译依赖（pom `systemPath`）

运行时由 FineReport 提供，编译用 `**provided`** + `systemPath` 指向 `${fr.lib.dir}`：


| 用途                  | JAR（均在 `finereport11/`）         |
| ------------------- | ------------------------------- |
| 插件框架、WebService、国际化 | `fine-core-11.0.jar`            |
| 报表预览、数据集、引擎调试       | `fine-report-engine-11.0.jar`   |
| 决策菜单、REST、系统选项      | `fine-decision-11.0.jar`        |
| 决策 + 报表集成扩展         | `fine-decision-report-11.0.jar` |
| 公共基础                | `fine-cbb-11.0.jar`             |
| SQL/数据源/方言（数据集调试）   | `fine-datasource-11.0.jar`      |


**不要**把 `fine-third-11.0.jar` 打进插件包；其余 `fine-accumulator`、`fine-schedule`*、`fine-webui`、`fine-swift-log-adaptor`、`fine-activator` 仅当编译报错缺类时再按需 `provided` 引用。

覆盖依赖目录示例：

```bash
mvn -DskipTests -Dfr.lib.dir=E:/AI/cursor/fine_plugins/sql_debug-1.0.1/finereport11 package
```

---

## 架构要点（`plugin.xml`）


| 扩展点                             | 作用                                                                                                                                       |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `extra-core` → `LocaleFinder`   | 国际化 basename，须与 JAR 内 `*.properties` 路径一致                                                                                                |
| `extra-core` → `WebService`     | 注册 `op=report_debug` → `ReportDebugSnapshotAction`（仅 `ReportServer` servlet） |
| `extra-report` → JS/CSS Handler | 预览页注入悬浮球与样式；JS 可按配置返回空数组 |
| `extra-decision`                | 三件套 + `DebugAssistantPreviewResource`（预览 REST 备用 `/preview/access\|snapshot`） |
| `function-recorder`             | 功能点登记；快照入口继承 `ActionNoSessionCMD` 并由 recorder 指向                                                                                         |
| `plugin-xml-i18n`               | `resource` 为 `fr-plugin-report-debug-assistant`（**末尾不要单独 `-`**），与 `locale/fr-plugin-report-debug-assistant_zh_CN.properties` basename 一致 |
| `prefer-packages`               | `com.fr.plugin.report.debug`；类加载异常时可暂改 `*` 排查                                                                                            |


---

## 核心代码路径（相对插件模块根）

```
src/main/java/com/fr/plugin/report/debug/
  core/fetch/          # 取数 Hook、FR 扩展点处理器
    FetchExecutionHook, RecordingFetchDataTimeLogger, FetchExecutionRecorder, ...
    DebugSessionCachedDataModelProcessor, DebugDataModelFillProcessor  # plugin.xml 注册
  core/registry/       # 执行登记（主路径读写）
    DatasetSqlExecutionRegistry, PendingFetchRegistry
  core/snapshot/       # 快照构建、SQL/参数解析、运行时缓存
    ReportDebugSnapshotBuilder, DebugAssistantSnapshotRuntime, DatasetSqlMetricsResolver, ...
  core/probe/          # 兜底探测（Logger 历史、ESD 索引、缓存检测）
    FetchLoggerHistoryProbe, ExecutedSqlTimeIndex, DatasetExecutionDetector
  core/util/           # SqlFingerprintUtil
  core/config/         # 配置、权限、审计、限流
    DebugAssistantConfigStore, DebugPermissionHelper, ...
  web/ReportDebugWebService.java
  web/ReportDebugSnapshotAction.java
  web/ReportDebugSnapshotSupport.java
  web/JsonResponses.java
  decision/DebugAssistantSystemOption.java
  decision/DebugAssistantControllerRegister.java
  decision/DebugAssistantConfigResource.java
  decision/DebugAssistantPreviewResource.java
  locale/DebugAssistantLocaleFinder.java
src/main/resources/com/fr/plugin/report/debug/locale/
  fr-plugin-report-debug-assistant*.properties
src/main/resources/com/fr/plugin/report/debug/decision/
  bundle.js
src/main/resources/com/fr/plugin/report/debug/web/
  report_debug_assistant.js, report_debug_assistant.css
plugin.xml
pom.xml
tools/          # 本地 DOM 调试脚本，不打包；见 tools/README.md
```

**读取链**：Registry（`core/registry`）→ Probe（`core/probe`）→ ESD Index 兜底；取数写入见 `core/fetch`。

**资源唯一来源**：仅 `src/main/resources/com/fr/plugin/report/debug/`。勿在仓库根目录维护 `com/` 副本（已从 `.gitignore` 忽略，多为 JAR 解压临时文件）。

`LocaleFinder.find()` 使用路径式 basename：`com/fr/plugin/report/debug/locale/fr-plugin-report-debug-assistant`（与 JAR 内 properties 目录一致；**勿**写成点号包名）。

---

## 预览页 HTTP 与悬浮球（运维必读）

### API 注册与路径

| 通道 | URL 示例 | 说明 |
|------|----------|------|
| **主路径** | `{prefix}/ReportServer?op=report_debug&cmd=access\|snapshot` | `op` **只**挂在 `ReportServer` servlet |
| **备用 REST** | `{prefix}/decision/report/debug/assistant/preview/access\|snapshot` | 生产 `ReportServer` 被登录过滤器拦截时用；`@VisitRefer(required=false)` |

`{prefix}` 因部署而异（**勿写死**）：

| 环境 | 常见 prefix | 实测 access 示例 |
|------|-------------|------------------|
| 本地 Tomcat / webroot | `/webroot` | `/webroot/ReportServer?op=report_debug&cmd=access` |
| 决策根路径部署 | ``（空）或 `/decision` | `/decision/ReportServer?...`（可能被登录页拦截） |

**禁止**把 `op=report_debug` 打到 `/decision/view/report`（预览 servlet）→ 必 404。

`report_debug_assistant.js` 的 `collectReportServerBases()` / `collectDecisionRestBases()` 会按 `FR.servletURL`、`location.pathname`、`FR.contextPath` 依次尝试多条候选 URL；成功后记住 `state.apiReportServerBase` 或 `state.apiRestBase` 供 snapshot 复用。

### 悬浮球显示链路

```
JS 注入 → checkAccess（多 URL 探测）→ allowed:true → createUi（#fr-report-debug-ball）
```

| 现象 | 原因 |
|------|------|
| `ball=null`，access 返回 HTML 登录页 | 打错 servlet 或 ReportServer 需决策登录 |
| `ball=null`，access 404 | 路径 prefix 错误（如本地用了 `/decision/ReportServer` 而非 `/webroot/ReportServer`） |
| `__FR_REPORT_DEBUG_ASSISTANT__=true` 但 `ball=null` | access 未返回 JSON 或 `allowed:false` |
| JS 有、球无 | 权限/白名单/`loginAuthOpen`/sessionID 时机 |

悬浮球 UI：`innerHTML=SQL`，`title=数据集调试助手`（`\u6570\u636e\u96c6\u8c03\u8bd5\u52a9\u624b`）。

### 生产 vs 本地快速探测（预览 iframe Console）

```javascript
var sid = FR.SessionMgr.getSessionID();
["/webroot/ReportServer","/ReportServer","/decision/ReportServer"].forEach(function(b){
  fetch(b+"?op=report_debug&cmd=access&sessionID="+encodeURIComponent(sid),{credentials:"include"})
    .then(r=>r.text()).then(t=>console.log(b, t.trim().charAt(0)==="{"?"JSON":"HTML", t.substring(0,80)));
});
fetch("/webroot/decision/report/debug/assistant/preview/access?sessionID="+encodeURIComponent(sid),{credentials:"include"})
  .then(r=>r.text()).then(console.log);
```

期望至少一条返回：`{"success":true,"data":{"allowed":true}}`。

---

## 构建与部署

```bash
cd E:\AI\cursor\fine_plugins\sql_debug-1.0.1
mvn -DskipTests package
```

部署清单：

1. 复制 `target/report-debug-assistant-1.0.1.jar` → 插件目录
2. 复制 `plugin.xml` → 同目录
3. 重启 FineReport / Tomcat
4. 校验：`jar tf report-debug-assistant-1.0.1.jar | findstr fr-plugin-report-debug-assistant`

---

## 远程调试（可选）

1. 报表/决策 JVM 增加：`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
2. IDEA：**Remote JVM Debug**，Host/Port 指向服务器；断点打在插件类上
3. 断点不命中：确认 `plugins\plugin-com.fr.plugin.report.debug.assistant-1.0.1\` 内 JAR 已替换并重启

---

## 已知运维问题（日志对照）

1. **Can't find bundle / 决策页英文 / 权限树乱码**
  - `LocaleFinder` basename（路径式）：`com/fr/plugin/report/debug/locale/fr-plugin-report-debug-assistant`  
  - `*_zh_CN.properties` 中文须 `**\uXXXX` 转义**（ASCII 文件），勿 UTF-8 直写，避免 Java 8 ResourceBundle 误读  
  - 无后缀默认 `fr-plugin-report-debug-assistant.properties` 为**英文 fallback**；locale 未命中 `_zh_CN` 时决策页/插件名显示英文  
  - `basic.log` 应出现 `Init bundle with path = com/fr/plugin/report/debug/locale/fr-plugin-report-debug-assistant`  
  - `displayName()` 必须返回 **i18n 键**（如 `Fine-Plugin_Report_Debug_Assistant`），禁止 `getLocText()`  
  - `db.script` 若仍存旧乱码：停 Tomcat → 插件卸载重装或删 `FINE_AUTHORITY_OBJECT` 对应行后重启
2. **悬浮球不显示 / access 返回 HTML 或 404**
  - 见上文「预览页 HTTP 与悬浮球」；本地常见正确路径 `/webroot/ReportServer`；生产可能 `/decision/ReportServer`（登录拦截时用 REST 备用）  
  - `ReportDebugJsHandler`：`enabled=false` 或 `allowReportPreview=false` 时不注入 JS  
  - `window.__FR_REPORT_DEBUG_ASSISTANT__=true` 仅表示 JS 执行，不等于已创建 `#fr-report-debug-ball`  
  - 排查：Network 中 `cmd=access` 须 JSON；`document.getElementById('fr-report-debug-ball')` 非 null
3. **决策侧栏菜单不出现（权限树有、侧栏无）**
  - `extra-decision` 须同时注册 `**SystemOptionProvider` + `WebResourceProvider` + `ControllerRegisterProvider`**（三件套）；仅 SystemOption 时权限树可见、侧栏不挂载  
  - `WebResourceProvider` 与 `SystemOption.client()` 均指向同一 `ReportDebugOptionClient`（参考数据预警 Sentinel）
  - **管理系统侧栏**：须在 `bundle.js` 中 `BI.config("dec.constant.management.navigation", …)` 注册节点（`id` 与 `SystemOptionProvider.id()` 一致），并 `BI.shortcut` 注册 `cardType` 对应 BI 配置页（参考官方 `plugin-com.fr.plugin.debugging.assistant` 的 `bundle.js`）  
  - `parentId` / `fullPath` 均为 `decision-management-root`，`sortIndex` ≥ 2035  
  - 当前登录角色须在 **权限管理 → 普通权限配置 → 管理系统** 中勾选本插件（授权权限配置里可见 ≠ 侧栏可用）  
  - 部署后重启 Tomcat 并插件禁用/启用
4. **403 forbidden / 配置页 toast forbidden**
  - 管理配置 API **不得**用 `authorizedUsers` 白名单（仅预览快照用）；配置接口依赖 `@VisitRefer`  
  - 预览快照：配置 `authorizedUsers` 后须命中白名单；用户名优先 `LoginService`（含 `*FromRequestCookie`），其次报表 session 参数  
  - `normalizeUsername`：`艾可(eoco)`、`安娜(Anna)` 取括号内登录名与白名单比对
5. **授权用户保存：中文显示名丢失（如 `安娜(Anna)` 存不住，`admin(admin)` 正常）**
  - **根因**：仅存登录名 `Anna` 时，平台组件回显需 `安娜(Anna)`，会退化为 `Anna(Anna)` 导致再次保存失败  
  - **存储格式**（`authorized-users.dat` 每行）：`用户名|显示名(用户名)`，例：`Anna|安娜(Anna)`、`admin|admin(admin)`  
  - **保存**：`bundle.js` 提交 `authorizedUsers` / `authorizedUsersText` 为 **显示名格式**；后端 `DebugAssistantConfigApiHelper.toStoredEntry()` 规范化  
  - **GET**：`authorizedUsers` → 显示名列表（供组件 `setValue`）；`authorizedUsernames` → 纯登录名；`authorizedUsersText` → 磁盘原始内容  
  - 平台 `dec.case.platform.user` 的 `value` 必须是 `显示名(用户名)` 字符串，禁止对象数组（`[object Object]`）  
  - 配置文件与 **插件 JAR 同目录**（`.../plugin-com.fr.plugin.report.debug.assistant-1.0.1/debug-assistant.properties` + `authorized-users.dat`），勿用 `getInstallHome()`  
  - 会话校验用 `SessionPoolManager.getSessionIDInfor`，勿单独依赖 `hasSessionID`

---

## 生产安全基线（P0，已落地）


| 配置项                   | 新装默认值         | 说明                                                   |
| --------------------- | ------------- | ---------------------------------------------------- |
| `loginAuthOpen`       | `true`        | 未配授权用户白名单时，快照须识别登录用户                                 |
| `maskSqlInResponse`   | `true`        | `querySql` / `originalSql` 均返回脱敏内容                   |
| `displayAbsolutePath` | `false`       | 快照 JSON 含 `displayAbsolutePath`；关闭时不返回绝对路径，预览/导出均不展示 |
| 配置 GET/SAVE           | 无 `configDir` | 避免泄露服务器路径                                            |


已有 `debug-assistant.properties` 中**显式写入**的项不受影响；缺省键按上表解析。

## P2 性能优化（已落地）


| 项      | 说明                                                                            |
| ------ | ----------------------------------------------------------------------------- |
| ESD 索引 | `ExecutedSqlTimeIndex` 单次快照只扫一遍 `ExecutedTableDataInfoManager.getList()`      |
| 构建上下文  | `SnapshotBuildContext` 复用 Calculator、SQL 解析结果缓存                               |
| 快照缓存键  | `sessionId@configVersion`，保存配置时 `configVersion++` 且 `clearAll()`              |
| 限流     | `cmd=snapshot` 每 session+IP 每分钟最多 24 次；`access` 不限                            |
| 审计     | 快照日志含 `user`、`report`（相对路径）                                                   |
| 预览 JS  | `enabled=false` 或 `allowReportPreview=false` 时不注入 `report_debug_assistant.js` |
| 预览 API | JS 多路径探测 ReportServer + 决策 REST；共用 `ReportDebugSnapshotSupport` |


## P1 优化（已落地）


| 项    | 说明                                                                        |
| ---- | ------------------------------------------------------------------------- |
| 快照缓存 | 预览默认 `refresh=0`；标题栏「刷新」传 `refresh=1`                                     |
| 配置保存 | 清空 `DebugAssistantSnapshotRuntime` 缓存                                     |
| 参数脱敏 | `maskSqlInResponse` 同时作用于 `parameters` 的 value/original                   |
| 预览请求 | 不再在 URL 附带 `fine_username`（依赖 Cookie / LoginService）                      |
| 决策页  | 增加「启用插件」开关；授权用户/登录验证/SQL 脱敏说明图标                                           |
| 死代码  | 已删除未引用的 `report_debug_management.js`、`decision_report_debug_config.js`    |
| 授权用户 | 磁盘格式 `用户名\|显示名(用户名)`；GET 回显显示名；权限比对仍用括号内登录名 |
| 预览 API | 多 URL 候选 + 决策 REST 备用（`DebugAssistantPreviewResource`） |
| 版本   | `pom.xml` / 产物 `report-debug-assistant-1.0.1.jar` 与 `plugin.xml` 1.0.1 对齐 |


## 修改守则

- 新增配置项：同步 `DebugAssistantConfigStore`、`DebugAssistantConfigResource` GET/POST、`decision/bundle.js`（配置 UI）
- 快照 JSON 注意脱敏；异常勿向前端泄露内部栈/路径
- 大改前对照 `AbstractSystemOptionProvider`、`Service`、`ActionNoSessionCMD`
- 产品 JAR 中部分 `com.fr.plugin.A.`* 已混淆，**只依赖官方公开 API**，勿引用混淆类

---

## 用户任务模板（可复制）

```
在 sql_debug-1.0.1 仓库的 report-debug-assistant 插件中：[具体需求]。
遵守 DEV_AGENT_PROMPT.md；只改必要文件；依赖 finereport11/；编译 mvn -DskipTests package；
说明需替换的 JAR 与 plugin.xml 路径及是否重启。
```

