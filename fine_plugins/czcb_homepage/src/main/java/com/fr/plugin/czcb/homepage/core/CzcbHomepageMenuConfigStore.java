package com.fr.plugin.czcb.homepage.core;

import com.fr.log.FineLoggerFactory;
import com.fr.stable.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 组织级菜单配置：持久化在插件目录 nav-menu-config.json，供多用户共享。
 */
public final class CzcbHomepageMenuConfigStore {

    private static final CzcbHomepageMenuConfigStore INSTANCE = new CzcbHomepageMenuConfigStore();

    private volatile String menuConfigJson = "[]";
    private volatile int configVersion = 0;

    private CzcbHomepageMenuConfigStore() {
        reload();
    }

    public static CzcbHomepageMenuConfigStore getInstance() {
        return INSTANCE;
    }

    public synchronized void reload() {
        File menuFile = CzcbHomepagePluginPaths.getMenuConfigFile();
        if (menuFile != null && menuFile.isFile()) {
            String raw = readUtf8File(menuFile);
            menuConfigJson = StringUtils.isBlank(raw) ? "[]" : raw.trim();
        } else {
            menuConfigJson = "[]";
        }
        Properties props = loadProperties(CzcbHomepagePluginPaths.getConfigFile());
        configVersion = parseInt(props.getProperty("configVersion"), 0);
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public String getMenuConfigJson() {
        String json = menuConfigJson;
        return StringUtils.isBlank(json) ? "[]" : json;
    }

    public boolean hasMenuConfig() {
        String json = getMenuConfigJson().trim();
        return StringUtils.isNotBlank(json) && !"[]".equals(json);
    }

    public synchronized void saveMenuConfigJson(String json) throws Exception {
        String normalized = StringUtils.isBlank(json) ? "[]" : json.trim();
        if (!normalized.startsWith("[")) {
            throw new IllegalArgumentException("menu config must be a JSON array");
        }
        File menuFile = CzcbHomepagePluginPaths.getMenuConfigFile();
        if (menuFile == null) {
            throw new IllegalStateException("plugin data dir unavailable");
        }
        File parent = menuFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        writeUtf8File(menuFile, normalized);
        menuConfigJson = normalized;
        configVersion++;
        Properties props = loadProperties(CzcbHomepagePluginPaths.getConfigFile());
        props.setProperty("configVersion", String.valueOf(configVersion));
        props.setProperty("menuUpdatedAt", String.valueOf(System.currentTimeMillis()));
        saveProperties(CzcbHomepagePluginPaths.getConfigFile(), props);
        FineLoggerFactory.getLogger().info(
                "[czcb-homepage] menu config saved, version={}", configVersion);
    }

    private static Properties loadProperties(File file) {
        Properties props = new Properties();
        if (file == null || !file.isFile()) {
            return props;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
        } catch (Exception ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
        return props;
    }

    private static void saveProperties(File file, Properties props) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(file);
        try {
            props.store(out, "czcb-homepage plugin");
        } finally {
            out.close();
        }
    }

    private static String readUtf8File(File file) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        } catch (Exception e) {
            FineLoggerFactory.getLogger().warn("[czcb-homepage] read menu config failed: {}", e.getMessage());
            return "[]";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return sb.toString();
    }

    private static void writeUtf8File(File file, String content) throws Exception {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static int parseInt(String value, int defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
