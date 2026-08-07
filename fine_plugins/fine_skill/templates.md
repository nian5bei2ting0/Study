# FineReport 插件开发 — 模板

占位符：`xxx` / `Xxx` / `com.fr.plugin.xxx`。复制后全局替换身份表中的四元组。

## plugin.xml（管理系统配置页）

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<plugin>
    <id>com.fr.plugin.xxx</id>
    <name><![CDATA[中文名]]></name>
    <active>yes</active>
    <version>1.0.0</version>
    <env-version>11.0~11.0</env-version>
    <vendor email="dev@local">xxx</vendor>
    <jartime>2023-08-08</jartime>
    <description><![CDATA[描述]]></description>
    <change-notes><![CDATA[1.0.0 初始版本]]></change-notes>
    <main-package>com.fr.plugin.xxx</main-package>
    <prefer-packages>com.fr.plugin.xxx</prefer-packages>
    <function-recorder class="com.fr.plugin.xxx.decision.XxxSystemOption"/>
    <plugin-xml-i18n resource="fr-plugin-xxx" location="com.fr.plugin.xxx.locale">
        <name key="Fine-Plugin_Xxx"/>
        <description key="Plugin-Xxx_Description"/>
        <change-notes key="Plugin-Xxx_Change_Notes"/>
    </plugin-xml-i18n>
    <extra-core>
        <LocaleFinder class="com.fr.plugin.xxx.locale.XxxLocaleFinder"/>
        <!-- 按需：<WebService class="com.fr.plugin.xxx.web.XxxWebService"/> -->
    </extra-core>
    <extra-decision>
        <SystemOptionProvider class="com.fr.plugin.xxx.decision.XxxSystemOption"/>
        <WebResourceProvider class="com.fr.plugin.xxx.decision.XxxWebResourceBridge"/>
        <ControllerRegisterProvider class="com.fr.plugin.xxx.decision.XxxControllerRegister"/>
        <!-- 按需：InitEvent / GlobalRequestFilter / 额外 WebResource -->
    </extra-decision>
    <!-- 按需 extra-report / extra-designer -->
</plugin>
```

`plugin-xml-i18n` 的 `resource` 末尾**不要**多一个 `-`。

## pom.xml 要点

```xml
<groupId>com.fr.plugin</groupId>
<artifactId>xxx</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>

<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <fr.lib.dir>${project.basedir}/finereport11</fr.lib.dir>
    <fr.plugin.id>com.fr.plugin.xxx</fr.plugin.id>
</properties>
```

常用 system 依赖（路径均 `${fr.lib.dir}/fine-*-11.0.jar`）：

| artifactId | 何时 |
|------------|------|
| fine-core | 总是 |
| fine-cbb | 几乎总是 |
| fine-decision | 决策 UI / LoginService / VisitRefer |
| fine-decision-report | 决策+报表 |
| fine-report-engine | Session、预览、TableData |
| fine-datasource | SQL / ESD |
| fine-third | Spring 注解编译；**勿打进包** |
| fine-report-designer | 设计器超链接 |

`javax.servlet-api` 3.1.0 → `provided`。

### antrun 打 zip（推荐）

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-antrun-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <id>package-plugin-zip</id>
      <phase>package</phase>
      <goals><goal>run</goal></goals>
      <configuration>
        <target>
          <mkdir dir="${project.build.directory}/plugin-${fr.plugin.id}-${project.version}"/>
          <copy file="${project.build.directory}/${project.build.finalName}.jar"
                todir="${project.build.directory}/plugin-${fr.plugin.id}-${project.version}"/>
          <copy file="${project.basedir}/plugin.xml"
                todir="${project.build.directory}/plugin-${fr.plugin.id}-${project.version}"/>
          <zip destfile="${project.build.directory}/plugin-${fr.plugin.id}-${project.version}.zip"
               basedir="${project.build.directory}/plugin-${fr.plugin.id}-${project.version}"/>
        </target>
      </configuration>
    </execution>
  </executions>
</plugin>
```

完整可抄：`E:\AI\cursor\fine_plugins\ip_white\pom.xml`。

## LocaleFinder

```java
package com.fr.plugin.xxx.locale;

import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.stable.fun.impl.AbstractLocaleFinder;

@FunctionRecorder
public class XxxLocaleFinder extends AbstractLocaleFinder {
    public static final String BUNDLE_BASENAME =
        "com/fr/plugin/xxx/locale/fr-plugin-xxx"; // 路径式 /，非点号

    @Override
    @ExecuteFunctionRecord
    public String find() {
        return BUNDLE_BASENAME;
    }
}
```

resources：

```
src/main/resources/com/fr/plugin/xxx/locale/
  fr-plugin-xxx.properties          # 英文 fallback
  fr-plugin-xxx_zh_CN.properties    # \u 转义，无 BOM
  fr-plugin-xxx_en.properties
```

```properties
Fine-Plugin_Xxx=\u4e2d\u6587\u540d
Plugin-Xxx_Description=...
Plugin-Xxx_Change_Notes=...
Dec-Plugin_Xxx=\u4e2d\u6587\u540d
```

## SystemOption

```java
@FunctionRecorder
public class XxxSystemOption extends AbstractSystemOptionProvider {
    public static final String OPTION_ID = "decision-management-xxx";
    public static final String I18N_NAME_KEY = "Fine-Plugin_Xxx";

    public String id() { return OPTION_ID; }
    public String parentId() { return "decision-management-root"; }
    public String fullPath() { return "decision-management-root"; }

    @ExecuteFunctionRecord
    public String displayName() { return I18N_NAME_KEY; } // 键，非译文

    public int sortIndex() { return 2050; }
    public Atom attach() { return MainComponent.KEY; }
    public Atom client() { return XxxOptionClient.KEY; }
}
```

## OptionClient

```java
@FunctionRecorder
public class XxxOptionClient extends Component {
    public static final XxxOptionClient KEY = new XxxOptionClient();
    private XxxOptionClient() {}

    @ExecuteFunctionRecord
    public ScriptPath script(RequestClient client) {
        return ScriptPath.build("/com/fr/plugin/xxx/decision/bundle.js");
    }

    @ExecuteFunctionRecord
    public StylePath style(RequestClient client) { return null; }

    public Filter filter() {
        return new Filter() {
            @Override public boolean accept() { return true; }
        };
    }
}
```

## WebResourceBridge

```java
public class XxxWebResourceBridge extends AbstractWebResourceProvider {
    public Atom attach() { return MainComponent.KEY; }
    public Atom client() { return XxxOptionClient.KEY; }
}
```

预览脚本另挂 `DecisionReportComponent` 时，再注册第二个 Bridge（见 `ip_white`）。

## ControllerRegister + ConfigResource

```java
public class XxxControllerRegister extends AbstractControllerRegisterProvider {
    public Class<?>[] getControllers() {
        return new Class[]{ XxxConfigResource.class };
    }
}

@Controller
@RequestMapping("/xxx/config")
@VisitRefer(required = true)
@FunctionRecorder
public class XxxConfigResource {

    @ResponseBody
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ExecuteFunctionRecord
    public Map<String, Object> get(...) { /* status=success|error */ }

    @ResponseBody
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ExecuteFunctionRecord
    public Map<String, Object> save(..., @RequestBody Map body) {
        // VisitRefer 不够：再校验 UserService.isAdmin(userId)
    }
}
```

注解包：`com.fr.third.springframework.*`。

## WebService

```java
public class XxxWebService extends NoSessionIDService {
    public String actionOP() { return "xxx_op"; }

    public void process(HttpServletRequest req, HttpServletResponse resp, String frameworkOp) {
        // frameworkOp == op，不是 cmd
        String cmd = WebUtils.getHTTPRequestParameter(req, "cmd");
        // 分发 info / get / save ...
    }
}
```

## JS Handler（可关闭）

```java
public String[] pathsForFiles() {
    if (!enabled) return new String[0];
    return new String[]{ "/com/fr/plugin/xxx/web/preview.js" };
}
```

## bundle.js 最小注册

```js
BI.shortcut("dec.management.plugin.xxx", XxxManagement);

BI.config("dec.constant.management.navigation", function (items) {
    items.push({
        value: "xxx",
        id: "decision-management-xxx",
        text: BI.i18nText("Fine-Plugin_Xxx"),
        cardType: "dec.management.plugin.xxx",
        cls: "management-log-font"
    });
    return items;
});

// 配置 IO
Dec.reqGet("/xxx/config/get", function (res) { ... });
Dec.reqPost("/xxx/config/save", payload, function (res) { ... });
```

## PluginPaths 思路

```text
1. CodeSource.getLocation() → JAR → 父目录 = 插件目录
2. 回退扫描 WEB-INF/plugins/plugin-{id}-*
3. 配置文件与 jar 同级写入
4. 目录名含版本号，勿写死旧版本路径
```

参考：`IpWhitePluginPaths`、`DebugAssistantPluginPaths`、`CzcbHomepagePluginPaths`。
