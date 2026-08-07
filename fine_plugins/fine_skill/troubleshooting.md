# FineReport 插件开发 — 排错

## 决策 / i18n

| 现象 | 原因 | 对策 |
|------|------|------|
| 侧栏/插件名显示 `Fine-Plugin_Xxx` | properties BOM；LocaleFinder 路径错；资源未进 jar | 无 BOM + `\u`；basename 用 `/`；`jar tf` 确认 |
| 决策页英文 | 未命中 `_zh_CN`，落到默认英文 properties | 查 locale 文件与 `basic.log` Init bundle |
| 权限树乱码 | `displayName` 用了中文或 `getLocText()` | 只返回 i18n **键** |
| 权限树有、侧栏无 | 缺 WebResource 或未 `BI.config` navigation | 补齐三件套 + navigation |
| 侧栏有、内容空白 | ScriptPath / cardType / shortcut 不一致 | 三处对齐；ScriptPath 带前导 `/` |
| 配置页 403 / forbidden | 用预览白名单拦了管理 REST | 管理 API 仅 `@VisitRefer`（+ 管理员） |
| 非管理员能保存 | 只有 VisitRefer | 加 `isAdmin(userId)`（注意是 userId） |
| Can't find bundle | LocaleFinder basename 与 jar 内路径不一致 | 路径式 `com/fr/.../fr-plugin-xxx` |

## 预览 / WebService

| 现象 | 原因 | 对策 |
|------|------|------|
| op 404 | 打到 `/decision/view/report` | 改用 `{prefix}/ReportServer?op=` |
| access 返回 HTML 登录页 | ReportServer 被决策登录拦截 | 先登录；或决策 REST 备用 |
| access 404 | prefix 错误 | 试 `/webroot/ReportServer`、`/ReportServer` |
| cmd 不生效 | 把 `process` 第三参当 cmd | 从 request 读 `cmd` |
| 悬浮球不出现 | access 非 JSON / allowed=false / 未注入 JS | Network 查 access；Handler 是否返回空 paths |
| session 校验失败 | 只用 `hasSessionID` | 用 `SessionPoolManager.getSessionIDInfor` |

预览 iframe Console 快速探测：

```javascript
var sid = FR.SessionMgr.getSessionID();
["/webroot/ReportServer","/ReportServer","/decision/ReportServer"].forEach(function(b){
  fetch(b+"?op=YOUR_OP&cmd=access&sessionID="+encodeURIComponent(sid),{credentials:"include"})
    .then(r=>r.text()).then(t=>console.log(b, t.trim().charAt(0)==="{"?"JSON":"HTML", t.substring(0,80)));
});
```

## 配置 / 前端

| 现象 | 原因 | 对策 |
|------|------|------|
| 配置写到 bin | 用了 installHome | PluginPaths：JAR 旁目录 |
| 开关保存无效 | 只写 properties 未在消费端读取 | 渲染+保存+GET+Store+业务五处接线 |
| 授权用户 `[object Object]` | value 传了对象 | 传 `显示名(用户名)` 字符串 |
| 中文用户存不住 | 只存登录名导致回显退化 | 磁盘 `Anna\|安娜(Anna)`；GET 返显示名 |
| 保存成 0,1,2 | `BI.map` 回调用错 | `BI.map(arr, function(i,v){...})` |
| iframe 保存丢鉴权 | 子页自建 XHR | `parent.Dec.reqPost` |

## 过滤器 / 静态站 / 升级

| 现象 | 原因 | 对策 |
|------|------|------|
| Filter 拦不到入口 | URL 模式过窄 | `/*` + 识别 `/v10/entry/access` 等 |
| 升级后仍旧前端 | help/ 未 redeploy | 配置页「重新部署」或触发 InitEvent |
| 升级后行为怪异 | `plugins` 多版本目录并存 | 删旧版目录后重启 |
| 菜单偶发不出现 | 权限/缓存 | 角色勾选管理系统权限；禁用启用插件 |

## 发版后验证

1. 插件管理显示**中文名**
2. 管理系统侧栏可开配置页
3. 保存后刷新仍在；非管理员写失败
4. 业务路径（拦截/浮层/首页）OK
5. `basic.log` 无 bundle 报错
6. `jar tf xxx.jar` 含 locale properties 与 bundle.js
