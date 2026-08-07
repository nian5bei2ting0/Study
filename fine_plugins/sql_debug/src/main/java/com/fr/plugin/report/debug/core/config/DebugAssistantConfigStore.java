package com.fr.plugin.report.debug.core.config;

import com.fr.stable.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 插件配置持久化：统一目录下的 debug-assistant.properties + authorized-users.dat。
 * <p>
 * 授权用户单独存 UTF-8 文本（每行 {@code 用户名|显示名(用户名)}），避免 properties 中 {@code ;} 分隔
 * 及多 Tomcat 部署路径不一致导致读写错位。
 */
public final class DebugAssistantConfigStore {

    private static final String KEY_ALLOW_PREVIEW = "allowReportPreview";
    private static final String KEY_AUTHORIZED_USERS = "authorizedUsers";
    private static final String KEY_MASK_SQL = "maskSqlInResponse";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LOGIN_AUTH_OPEN = "loginAuthOpen";
    private static final String KEY_DISPLAY_ABSOLUTE_PATH = "displayAbsolutePath";

    private static volatile DebugAssistantConfigStore instance;

    private final Properties properties = new Properties();
    private final File configFile;
    private final File authUsersFile;
    private String authorizedUsersCache = "";
    private volatile long configVersion = 1L;

    private DebugAssistantConfigStore() {
        File pluginDir = DebugAssistantPluginPaths.getPluginDataDir();
        configFile = new File(pluginDir, DebugAssistantPluginPaths.CONFIG_FILE);
        authUsersFile = new File(pluginDir, DebugAssistantPluginPaths.AUTH_USERS_FILE);
        load();
    }

    public static DebugAssistantConfigStore getInstance() {
        if (instance == null) {
            synchronized (DebugAssistantConfigStore.class) {
                if (instance == null) {
                    instance = new DebugAssistantConfigStore();
                }
            }
        }
        return instance;
    }

    public static void reload() {
        DebugAssistantPluginPaths.reset();
        instance = null;
        getInstance();
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getAuthUsersFile() {
        return authUsersFile;
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(properties.getProperty(KEY_ENABLED, "true"));
    }

    public void setEnabled(boolean enabled) {
        properties.setProperty(KEY_ENABLED, String.valueOf(enabled));
    }

    public boolean isAllowReportPreview() {
        return Boolean.parseBoolean(properties.getProperty(KEY_ALLOW_PREVIEW, "true"));
    }

    public void setAllowReportPreview(boolean allow) {
        properties.setProperty(KEY_ALLOW_PREVIEW, String.valueOf(allow));
    }

    public boolean isMaskSqlInResponse() {
        return Boolean.parseBoolean(properties.getProperty(KEY_MASK_SQL, "true"));
    }

    public void setMaskSqlInResponse(boolean mask) {
        properties.setProperty(KEY_MASK_SQL, String.valueOf(mask));
    }

    public String getAuthorizedUsers() {
        return authorizedUsersCache == null ? "" : authorizedUsersCache;
    }

    public void setAuthorizedUsers(String users) {
        authorizedUsersCache = users == null ? "" : users.trim();
    }

    public boolean isLoginAuthOpen() {
        return Boolean.parseBoolean(properties.getProperty(KEY_LOGIN_AUTH_OPEN, "true"));
    }

    public void setLoginAuthOpen(boolean open) {
        properties.setProperty(KEY_LOGIN_AUTH_OPEN, String.valueOf(open));
    }

    public boolean isDisplayAbsolutePath() {
        return Boolean.parseBoolean(properties.getProperty(KEY_DISPLAY_ABSOLUTE_PATH, "false"));
    }

    public void setDisplayAbsolutePath(boolean display) {
        properties.setProperty(KEY_DISPLAY_ABSOLUTE_PATH, String.valueOf(display));
    }

    public Properties snapshot() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    public void apply(Properties incoming) {
        if (incoming == null) {
            return;
        }
        for (String key : incoming.stringPropertyNames()) {
            if (KEY_AUTHORIZED_USERS.equals(key)) {
                continue;
            }
            properties.setProperty(key, incoming.getProperty(key));
        }
    }

    public long getConfigVersion() {
        return configVersion;
    }

    public synchronized void save() throws IOException {
        configVersion++;
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create plugin config directory: " + parent.getAbsolutePath());
        }
        properties.remove(KEY_AUTHORIZED_USERS);
        OutputStream out = null;
        try {
            out = new FileOutputStream(configFile);
            properties.store(out, "report-debug-assistant");
        } finally {
            closeQuietly(out);
        }
        writeAuthUsersFile(authorizedUsersCache);
    }

    private void load() {
        migrateLegacyConfigFilesIfNeeded();
        loadPropertiesFile();
        authorizedUsersCache = resolveAuthorizedUsersFromDisk();
        migrateAuthUsersToDatIfNeeded();
        if (StringUtils.isBlank(authorizedUsersCache) && !configFile.exists()) {
            initDefaultsIfMissing();
        }
    }

    private void loadPropertiesFile() {
        if (!configFile.exists()) {
            initDefaults();
            return;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(configFile);
            properties.load(in);
        } catch (IOException ignored) {
            initDefaults();
        } finally {
            closeQuietly(in);
        }
    }

    private void initDefaultsIfMissing() {
        if (configFile.exists() && authUsersFile.exists()) {
            return;
        }
        initDefaults();
        try {
            save();
        } catch (IOException ignored) {
        }
    }

    private String resolveAuthorizedUsersFromDisk() {
        String fromDat = readAuthUsersDat(authUsersFile);
        if (StringUtils.isNotBlank(fromDat)) {
            return fromDat;
        }
        String fromProps = properties.getProperty(KEY_AUTHORIZED_USERS, "");
        if (StringUtils.isNotBlank(fromProps)) {
            return fromProps.trim();
        }
        for (File dir : DebugAssistantPluginPaths.collectAllPluginDataDirs()) {
            if (dir == null) {
                continue;
            }
            File dat = new File(dir, DebugAssistantPluginPaths.AUTH_USERS_FILE);
            fromDat = readAuthUsersDat(dat);
            if (StringUtils.isNotBlank(fromDat)) {
                return fromDat;
            }
            File legacyProps = new File(dir, DebugAssistantPluginPaths.CONFIG_FILE);
            if (!legacyProps.exists() || legacyProps.equals(configFile)) {
                continue;
            }
            Properties legacy = loadPropertiesQuietly(legacyProps);
            String legacyUsers = legacy.getProperty(KEY_AUTHORIZED_USERS, "");
            if (StringUtils.isNotBlank(legacyUsers)) {
                return legacyUsers.trim();
            }
        }
        return "";
    }

    private static Properties loadPropertiesQuietly(File file) {
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
        } catch (IOException ignored) {
        } finally {
            closeQuietly(in);
        }
        return props;
    }

    private static String readAuthUsersDat(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (StringUtils.isBlank(line) || line.startsWith("#")) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException ignored) {
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void writeAuthUsersFile(String stored) throws IOException {
        File parent = authUsersFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create plugin config directory: " + parent.getAbsolutePath());
        }
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(authUsersFile), StandardCharsets.UTF_8);
            if (StringUtils.isBlank(stored)) {
                writer.write("# report-debug-assistant authorized users\n");
                return;
            }
            String[] entries = stored.split("[\\r\\n;]+");
            for (String entry : entries) {
                entry = entry.trim();
                if (StringUtils.isBlank(entry)) {
                    continue;
                }
                writer.write(entry);
                writer.write('\n');
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void migrateLegacyConfigFilesIfNeeded() {
        if (configFile.exists() && authUsersFile.exists()) {
            return;
        }
        File currentDir = configFile.getParentFile();
        for (File dir : DebugAssistantPluginPaths.collectAllPluginDataDirs()) {
            if (dir == null || dir.equals(currentDir)) {
                continue;
            }
            copyFileIfAbsent(new File(dir, DebugAssistantPluginPaths.CONFIG_FILE), configFile);
            copyFileIfAbsent(new File(dir, DebugAssistantPluginPaths.AUTH_USERS_FILE), authUsersFile);
            if (configFile.exists() && authUsersFile.exists()) {
                break;
            }
        }
    }

    private void migrateAuthUsersToDatIfNeeded() {
        if (authUsersFile.exists() || StringUtils.isBlank(authorizedUsersCache)) {
            return;
        }
        try {
            writeAuthUsersFile(authorizedUsersCache);
            properties.remove(KEY_AUTHORIZED_USERS);
            if (configFile.exists()) {
                OutputStream out = null;
                try {
                    out = new FileOutputStream(configFile);
                    properties.store(out, "report-debug-assistant");
                } finally {
                    closeQuietly(out);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void copyFileIfAbsent(File source, File target) {
        if (source == null || target == null || !source.exists() || target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(target);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private void initDefaults() {
        properties.clear();
        properties.setProperty(KEY_ENABLED, "true");
        properties.setProperty(KEY_ALLOW_PREVIEW, "true");
        properties.setProperty(KEY_MASK_SQL, "true");
        properties.setProperty(KEY_LOGIN_AUTH_OPEN, "true");
        properties.setProperty(KEY_DISPLAY_ABSOLUTE_PATH, "false");
        authorizedUsersCache = "";
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }
}
