# 开发辅助脚本（不打包进插件）

本目录脚本仅用于本地调试 FineReport 预览页 DOM / 控件，**不会**被 Maven 打入 `report-debug-assistant-*.jar`，部署时可忽略。

| 脚本 | 用途 |
|------|------|
| `find-*.js` | 在浏览器控制台查找特定控件/触发器 |
| `extract-widget.js` | 提取页面 widget 结构 |

正式资源路径见 `src/main/resources/com/fr/plugin/report/debug/`。
