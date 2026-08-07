package com.fr.plugin.czcb.homepage.core;

import com.fr.stable.StableUtils;
import com.fr.stable.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CzcbHomepagePluginPaths {

    private static volatile File pluginDataDir;

    private CzcbHomepagePluginPaths() {
    }

    public static File getPluginDataDir() {
        if (pluginDataDir == null) {
            synchronized (CzcbHomepagePluginPaths.class) {
                if (pluginDataDir == null) {
                    pluginDataDir = resolvePluginDataDir();
                }
            }
        }
        return pluginDataDir;
    }

    public static File getWebrootDir() {
        String catalinaBase = System.getProperty("catalina.base");
        if (StringUtils.isNotBlank(catalinaBase)) {
            File webroot = new File(catalinaBase, "webapps" + File.separator + "webroot");
            if (webroot.isDirectory()) {
                return webroot;
            }
        }
        try {
            String home = StableUtils.getInstallHome();
            if (StringUtils.isNotBlank(home)) {
                File homeFile = new File(home);
                String path = homeFile.getAbsolutePath().replace('\\', '/').toLowerCase(Locale.ROOT);
                if (path.endsWith("/webroot") && homeFile.isDirectory()) {
                    return homeFile;
                }
                File nested = new File(homeFile, "webapps" + File.separator + "webroot");
                if (nested.isDirectory()) {
                    return nested;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static File getHomepageDeployDir() {
        File webroot = getWebrootDir();
        if (webroot == null) {
            return null;
        }
        return new File(webroot, CzcbHomepageConstants.HOMEPAGE_DEPLOY_DIR.replace('/', File.separatorChar));
    }

    public static File getDeployMarkerFile() {
        File deployDir = getHomepageDeployDir();
        if (deployDir == null) {
            return null;
        }
        return new File(deployDir, CzcbHomepageConstants.DEPLOY_MARKER_FILE);
    }

    public static File getConfigFile() {
        return new File(getPluginDataDir(), CzcbHomepageConstants.CONFIG_FILE);
    }

    public static File getMenuConfigFile() {
        return new File(getPluginDataDir(), CzcbHomepageConstants.MENU_CONFIG_FILE);
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
        if (fallbackRoot != null) {
            File fallback = new File(fallbackRoot,
                    CzcbHomepageConstants.PLUGIN_DIR_PREFIX + "-" + CzcbHomepageConstants.PLUGIN_VERSION);
            ensureDir(fallback);
            return fallback;
        }
        File cwd = new File(CzcbHomepageConstants.PLUGIN_DIR_PREFIX + "-" + CzcbHomepageConstants.PLUGIN_VERSION);
        ensureDir(cwd);
        return cwd;
    }

    private static boolean isPluginFolder(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        if (!isPluginFolderName(dir.getName())) {
            return false;
        }
        return new File(dir, "plugin.xml").exists()
                || new File(dir, CzcbHomepageConstants.CONFIG_FILE).exists()
                || new File(dir, CzcbHomepageConstants.MENU_CONFIG_FILE).exists();
    }

    private static boolean isPluginFolderName(String name) {
        return StringUtils.isNotBlank(name)
                && (name.equals(CzcbHomepageConstants.PLUGIN_DIR_PREFIX)
                || name.startsWith(CzcbHomepageConstants.PLUGIN_DIR_PREFIX + "-"));
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

    private static List<File> collectPluginsRoots() {
        List<File> roots = new ArrayList<File>();
        addIfPresent(roots, webrootPluginsRoot());
        addIfPresent(roots, installHomePluginsRoot());
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
            URL location = CzcbHomepagePluginPaths.class.getProtectionDomain().getCodeSource().getLocation();
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
        return root.isDirectory() ? root : null;
    }

    private static File installHomePluginsRoot() {
        try {
            String home = StableUtils.getInstallHome();
            if (StringUtils.isBlank(home)) {
                return null;
            }
            File root = new File(home, "WEB-INF" + File.separator + "plugins");
            return root.isDirectory() ? root : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void ensureDir(File dir) {
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }
}
