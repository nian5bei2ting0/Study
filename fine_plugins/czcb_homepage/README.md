# 综合大屏首页插件（czcb-homepage）

FineReport 11 插件：将 `homepage` 静态大屏嵌入插件；`config.html` 作为管理系统配置页；安装后自动在「目录管理」挂载 `index.html` 链接。菜单配置**服务端持久化**（组织级共享）。

## 依赖

编译依赖位于项目内 `finereport11/`（`fine-core`、`fine-decision` 等 11.0 JAR）。

## 构建

```bash
cd E:\AI\cursor\fine_plugins\czcb_homepage
mvn -DskipTests package
```

产物：`target/plugin-com.fr.plugin.czcb.homepage-1.0.11.zip`

## 访问

| 页面 | URL |
|------|-----|
| 首页 | `/webroot/help/czcb_homepage/index.html` |
| 配置 | 管理系统 → 综合大屏首页 |
| 菜单只读 API | `/webroot/ReportServer?op=czcb_homepage&cmd=menu_get` |
| 强制重部署 | 决策 REST `POST /decision/czcb/homepage/config/deploy?force=true`（需登录） |

## 行为说明

1. **静态资源**：staging 原子替换到 `webroot/help/czcb_homepage/`
2. **目录管理**：按 path/name 幂等挂载；成功后 60s TTL 避免热路径重复查询
3. **菜单配置**：写入插件目录 `nav-menu-config.json`；首页启动/可见性变化拉取服务端
4. **报表 URL**：默认仅允许以 `/` 开头的站内路径；勾选「外链」后允许 http(s)
5. **公开 WebService**：`info` / `menu_get` / 管理员 `menu_save`；匿名 deploy 禁用
6. **卸载**：禁用插件后静态页与目录链接不会自动清理，需管理员手工删除（若需要）

## 开发手册

跨项目（含本插件、`ip_white`、`sql_debug`）的详细开发过程见：

`E:\AI\cursor\fine_plugins\docs\帆软插件开发手册.md`
