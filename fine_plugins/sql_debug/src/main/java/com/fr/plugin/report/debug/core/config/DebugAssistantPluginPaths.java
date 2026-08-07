package com.fr.plugin.report.debug.core.config;

import com.fr.stable.StableUtils;
import com.fr.stable.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 统一解析插件数据目录。目录名随 plugin.xml 版本变化（如 {@code ...-1.0.2}），
 * 不能写死 {@code 1.0.0}。
 */
public final class DebugAssistantPluginPaths {

    static final String PLUGIN_ID = "com.fr.plugin.report.debug.assistant";
    static final String PLUGIN_DIR_PREFIX = "plugin-com.fr.plugin.report.debug.assistant";
    static final String CONFIG_FILE = "debug-assistant.properties";
    static final String AUTH_USERS_FILE = "authorized-users.dat";

    private static volatile File pluginDataDir;

    private DebugAssistantPluginPaths() {
    }

    static File getPluginDataDir() {
        if (pluginDataDir == null) {
            synchronized (DebugAssistantPluginPaths.class) {
                if (pluginDataDir == null) {
                    pluginDataDir = resolvePluginDataDir();
                }
            }
        }
        return pluginDataDir;
    }

    static void reset() {
        pluginDataDir = null;
    }

    static List<File> collectAllPluginDataDirs() {
        Set<String> seen = new LinkedHashSet<String>();
        List<File> dirs = new ArrayList<File>();
        for (File pluginsRoot : collectPluginsRoots()) {
            appendPluginDirsUnder(pluginsRoot, dirs, seen);
        }
        File besideJar = besideJarPluginDir();
        if (isPluginFolder(besideJar)) {
            String path = besideJar.getAbsolutePath();
            if (seen.add(path)) {
                dirs.add(0, besideJar);
            }
        }
        return dirs;
    }

    private static File resolvePluginDataDir() {
        File besideJar = besideJarPluginDir();
        if (isPluginFolder(besideJar)) {
            ensureDir(besideJar);
            return besideJar;
        }
        for (File pluginsRoot : collectPluginsRoots()) {
            File matched = findPluginDirUnder(pluginsRoot);
            if (matched != null) {
                ensureDir(matched);
                return matched;
            }
        }
        if (besideJar != null) {
            ensureDir(besideJar);
            return besideJar;
        }
        File fallbackRoot = webrootPluginsRoot();
        if (fallbackRoot == null) {
            fallbackRoot = installHomePluginsRoot();
        }
        if (fallbackRoot != null) {
            File fallback = new File(fallbackRoot, PLUGIN_DIR_PREFIX + "-1.0.9");
            ensureDir(fallback);
            return fallback;
        }
        File cwd = new File(PLUGIN_DIR_PREFIX + "-1.0.9");
        ensureDir(cwd);
        return cwd;
    }

    static boolean isPluginFolder(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        if (!isPluginFolderName(dir.getName())) {
            return false;
        }
        if (new File(dir, "plugin.xml").exists()) {
            return true;
        }
        return new File(dir, CONFIG_FILE).exists() || new File(dir, AUTH_USERS_FILE).exists();
    }

    private static boolean isPluginFolderName(String name) {
        return StringUtils.isNotBlank(name)
                && (name.equals(PLUGIN_DIR_PREFIX) || name.startsWith(PLUGIN_DIR_PREFIX + "-"));
    }

    private static File findPluginDirUnder(File pluginsRoot) {
        if (pluginsRoot == null || !pluginsRoot.isDirectory()) {
            return null;
        }
        File jarDir = besideJarPluginDir();
        if (jarDir != null && pluginsRoot.equals(jarDir.getParentFile()) && isPluginFolder(jarDir)) {
            return jarDir;
        }
        File best = null;
        File[] children = pluginsRoot.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (!isPluginFolder(child)) {
                continue;
            }
            if (best == null || child.getName().compareTo(best.getName()) > 0) {
                best = child;
            }
        }
        return best;
    }

    private static void appendPluginDirsUnder(File pluginsRoot, List<File> dirs, Set<String> seen) {
        if (pluginsRoot == null || !pluginsRoot.isDirectory()) {
            return;
        }
        File[] children = pluginsRoot.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (!isPluginFolder(child)) {
                continue;
            }
            String path = child.getAbsolutePath();
            if (seen.add(path)) {
                dirs.add(child);
            }
        }
    }

    private static List<File> collectPluginsRoots() {
        List<File> roots = new ArrayList<File>();
        addIfPresent(roots, webrootPluginsRoot());
        addIfPresent(roots, installHomePluginsRoot());
        addIfPresent(roots, installHomeWebrootPluginsRoot());
        addIfPresent(roots, legacyBinPluginsRoot());
        File beside = besideJarPluginDir();
        if (beside != null && beside.getParentFile() != null) {
            addIfPresent(roots, beside.getParentFile());
        }
        return roots;
    }

    private static void addIfPresent(List<File> list, File dir) {
        if (dir == null) {
            return;
        }
        String path = dir.getAbsolutePath();
        for (File existing : list) {
            if (existing.getAbsolutePath().equals(path)) {
                return;
            }
        }
        list.add(dir);
    }

    private static File besideJarPluginDir() {
        try {
            URL location = DebugAssistantPluginPaths.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            File jarOrDir = new File(location.toURI());
            File pluginDir = jarOrDir.isFile() ? jarOrDir.getParentFile() : jarOrDir;
            if (pluginDir != null && pluginDir.exists()) {
                return pluginDir;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static File webrootPluginsRoot() {
        String catalinaBase = System.getProperty("catalina.base");
        if (StringUtils.isBlank(catalinaBase)) {
            return null;
        }
        File root = new File(catalinaBase, "webapps" + File.separator + "webroot"
                + File.separator + "WEB-INF" + File.separator + "plugins");
        return root.isDirectory() || root.getParentFile().exists() ? root : null;
    }

    private static File installHomePluginsRoot() {
        try {
            String home = StableUtils.getInstallHome();
            if (StringUtils.isBlank(home)) {
                return null;
            }
            File root = new File(home, "WEB-INF" + File.separator + "plugins");
            return root.isDirectory() || root.getParentFile().exists() ? root : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static File installHomeWebrootPluginsRoot() {
        try {
            String home = StableUtils.getInstallHome();
            if (StringUtils.isBlank(home)) {
                return null;
            }
            File homeFile = new File(home);
            String path = homeFile.getAbsolutePath().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (path.endsWith("/webroot")) {
                return null;
            }
            File root = new File(homeFile, "webapps" + File.separator + "webroot"
                    + File.separator + "WEB-INF" + File.separator + "plugins");
            return root.isDirectory() || root.getParentFile().exists() ? root : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static File legacyBinPluginsRoot() {
        String catalinaBase = System.getProperty("catalina.base");
        if (StringUtils.isBlank(catalinaBase)) {
            return null;
        }
        return new File(catalinaBase, "bin" + File.separator + "WEB-INF" + File.separator + "plugins");
    }

    private static void ensureDir(File dir) {
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }
}
