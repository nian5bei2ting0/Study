# 项目提示词（AI / 协作者上下文）

将以下内容整体复制到 Cursor 或其他 AI 对话中，作为本项目开发与改动的上下文说明。

---

## 角色与目标

FineReport 11 插件 **`com.fr.plugin.czcb.homepage`**：把纯静态「综合大屏首页」嵌入插件。

- 顶部一级菜单 + 二级下拉，主体 iframe 嵌入帆软报表 URL
- **无登录校验**（已去除 SSO / 登录弹窗 / 管理员门禁 / 会话空闲）
- **`config.html`**：插件管理系统配置页（iframe 嵌入）
- **`index.html`**：安装后幂等挂载到决策平台「目录管理」根目录链接

源码目录 `homepage/` 与插件资源 `src/main/resources/.../web/homepage/` 保持一致。

## 技术栈

- HTML5，无构建步骤（静态页）
- 插件：Maven + FineReport 11 JAR（`finereport11/`）
- CSS：`base.css`、`layout.css`、`component.css`；配置页额外 `config-page.css`
- JS：`menu-config-api.js`、`main.js`、`menu-config-page.js`
- 图标：Font Awesome 6（`vendor/font-awesome`）

## 脚本加载顺序

### `index.html`

`menu-config-api.js` → `main.js`

### `config.html`

`menu-config-api.js` → `menu-config-page.js`

## 插件行为

| 能力 | 实现 |
|------|------|
| 静态部署 | `CzcbHomepageResourceDeployer` → `webroot/help/czcb_homepage/` |
| 目录挂载 | `CzcbHomepageDirectoryRegistrar` → `EntryService.addLink`（父节点 `decision-directory-root`） |
| 配置页 | `SystemOptionProvider` + `bundle.js` iframe `config.html` |
| 初始化 | `DecisionInitEventProvider` 部署静态页并确保目录链接 |

## 菜单配置与持久化

- **localStorage** 键：`homepage_nav_menu_config_v1`（见 `menu-config-api.js`）
- 报表 URL 安全校验：`HomepageMenuConfigApi.isSafeReportUrlString`

## 访问示例

- 首页：`/webroot/help/czcb_homepage/index.html`
- 配置：`/webroot/help/czcb_homepage/config.html`
- 管理系统侧栏：综合大屏首页

构建：`mvn -DskipTests package` → `target/plugin-com.fr.plugin.czcb.homepage-1.0.0/`

---

*若代码结构或 API 变更，请同步更新本文件。*
