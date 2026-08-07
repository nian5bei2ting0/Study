package com.fr.plugin.czcb.homepage.core;

import com.fr.log.FineLoggerFactory;
import com.fr.stable.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 静态资源部署：版本 marker 跳过重复解压；force 时 staging 原子替换，避免半成品。
 */
public final class CzcbHomepageResourceDeployer {

    private static volatile boolean deployAttempted;

    private CzcbHomepageResourceDeployer() {
    }

    public static synchronized DeployResult ensureDeployed(boolean force) {
        deployAttempted = true;
        File targetDir = CzcbHomepagePluginPaths.getHomepageDeployDir();
        if (targetDir == null) {
            return DeployResult.failed("webroot not found");
        }
        File marker = CzcbHomepagePluginPaths.getDeployMarkerFile();
        if (!force && marker != null && marker.isFile()) {
            String markerVersion = readMarker(marker);
            if (CzcbHomepageConstants.PLUGIN_VERSION.equals(markerVersion)
                    && new File(targetDir, "index.html").isFile()) {
                return DeployResult.ok(publicDeployPath(), false);
            }
        }
        File staging = new File(targetDir.getParentFile(),
                targetDir.getName() + ".staging-" + System.currentTimeMillis());
        try {
            if (staging.exists()) {
                deleteRecursively(staging);
            }
            if (!staging.mkdirs()) {
                return DeployResult.failed("cannot create staging dir");
            }
            int count = extractClasspathHomepage(staging);
            writeMarker(new File(staging, CzcbHomepageConstants.DEPLOY_MARKER_FILE),
                    CzcbHomepageConstants.PLUGIN_VERSION);
            if (!swapStagingToTarget(staging, targetDir)) {
                deleteRecursively(staging);
                return DeployResult.failed("cannot swap staging to target");
            }
            FineLoggerFactory.getLogger().info("[czcb-homepage] deployed {} files to {}",
                    count, targetDir.getAbsolutePath());
            return DeployResult.ok(publicDeployPath(), true);
        } catch (Exception e) {
            try {
                deleteRecursively(staging);
            } catch (Exception ignored) {
            }
            FineLoggerFactory.getLogger().error("[czcb-homepage] deploy failed: {}", e.getMessage());
            return DeployResult.failed("deploy failed: " + e.getMessage());
        }
    }

    public static DeployResult getStatus() {
        File targetDir = CzcbHomepagePluginPaths.getHomepageDeployDir();
        if (targetDir == null) {
            return DeployResult.failed("webroot not found");
        }
        boolean ready = new File(targetDir, "index.html").isFile();
        File marker = CzcbHomepagePluginPaths.getDeployMarkerFile();
        String version = marker != null && marker.isFile() ? readMarker(marker) : "";
        DeployResult result = ready
                ? DeployResult.ok(publicDeployPath(), false)
                : DeployResult.failed("homepage not deployed");
        result.setDeployedVersion(version);
        result.setDeployAttempted(deployAttempted);
        return result;
    }

    /** 对外仅返回相对 web 路径，避免泄露绝对文件系统路径 */
    public static String publicDeployPath() {
        return "/" + CzcbHomepageConstants.HOMEPAGE_DEPLOY_DIR.replace('\\', '/');
    }

    private static boolean swapStagingToTarget(File staging, File targetDir) {
        File backup = new File(targetDir.getParentFile(),
                targetDir.getName() + ".bak-" + System.currentTimeMillis());
        try {
            if (targetDir.exists()) {
                if (!targetDir.renameTo(backup)) {
                    deleteRecursively(targetDir);
                }
            }
            if (!staging.renameTo(targetDir)) {
                // rename 失败时回退为拷贝
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    return false;
                }
                copyDirectoryGuarded(staging, targetDir, staging);
                deleteRecursively(staging);
            }
            if (backup.exists()) {
                deleteRecursively(backup);
            }
            return new File(targetDir, "index.html").isFile();
        } catch (Exception e) {
            FineLoggerFactory.getLogger().warn("[czcb-homepage] swap staging failed: {}", e.getMessage());
            try {
                if (!targetDir.exists() && backup.exists()) {
                    backup.renameTo(targetDir);
                }
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    private static int extractClasspathHomepage(File targetDir) throws Exception {
        ClassLoader loader = CzcbHomepageResourceDeployer.class.getClassLoader();
        URL rootUrl = loader.getResource(CzcbHomepageConstants.CLASSPATH_HOMEPAGE_PREFIX);
        if (rootUrl == null) {
            throw new IllegalStateException("classpath homepage resources missing");
        }
        if ("jar".equalsIgnoreCase(rootUrl.getProtocol())) {
            return extractFromJar(rootUrl, targetDir);
        }
        return extractFromDirectory(rootUrl, targetDir);
    }

    private static int extractFromJar(URL rootUrl, File targetDir) throws Exception {
        JarURLConnection connection = (JarURLConnection) rootUrl.openConnection();
        JarFile jarFile = connection.getJarFile();
        int count = 0;
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(CzcbHomepageConstants.CLASSPATH_HOMEPAGE_PREFIX) || entry.isDirectory()) {
                    continue;
                }
                String relative = name.substring(CzcbHomepageConstants.CLASSPATH_HOMEPAGE_PREFIX.length());
                if (StringUtils.isBlank(relative) || relative.contains("..")) {
                    continue;
                }
                File outFile = new File(targetDir, relative.replace('/', File.separatorChar));
                if (!isSafeChild(targetDir, outFile)) {
                    continue;
                }
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                InputStream in = jarFile.getInputStream(entry);
                try {
                    copyStream(in, outFile);
                    count++;
                } finally {
                    in.close();
                }
            }
        } finally {
            jarFile.close();
        }
        return count;
    }

    private static int extractFromDirectory(URL rootUrl, File targetDir) throws Exception {
        File sourceDir = new File(rootUrl.toURI());
        return copyDirectoryGuarded(sourceDir, targetDir, targetDir);
    }

    private static int copyDirectoryGuarded(File sourceDir, File targetDir, File rootGuard) throws Exception {
        int count = 0;
        File[] children = sourceDir.listFiles();
        if (children == null) {
            return 0;
        }
        for (File child : children) {
            File out = new File(targetDir, child.getName());
            if (!isSafeChild(rootGuard, out)) {
                FineLoggerFactory.getLogger().warn("[czcb-homepage] skip unsafe path: {}", out);
                continue;
            }
            if (child.isDirectory()) {
                if (!out.exists() && !out.mkdirs()) {
                    continue;
                }
                count += copyDirectoryGuarded(child, out, rootGuard);
            } else {
                copyStream(new FileInputStream(child), out);
                count++;
            }
        }
        return count;
    }

    private static boolean isSafeChild(File root, File candidate) {
        try {
            String rootPath = root.getCanonicalPath();
            String childPath = candidate.getCanonicalPath();
            return childPath.equals(rootPath) || childPath.startsWith(rootPath + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            FineLoggerFactory.getLogger().warn("[czcb-homepage] cannot delete {}", file.getAbsolutePath());
        }
    }

    private static void copyStream(InputStream in, File outFile) throws Exception {
        OutputStream out = new FileOutputStream(outFile);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            out.close();
        }
    }

    private static void writeMarker(File marker, String version) throws Exception {
        if (marker == null) {
            return;
        }
        File parent = marker.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        OutputStream out = new FileOutputStream(marker);
        try {
            out.write(version.getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
    }

    private static String readMarker(File marker) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(marker.toPath());
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static final class DeployResult {
        private final boolean success;
        private final String path;
        private final boolean freshlyDeployed;
        private final String message;
        private String deployedVersion = "";
        private boolean deployAttempted;

        private DeployResult(boolean success, String path, boolean freshlyDeployed, String message) {
            this.success = success;
            this.path = path;
            this.freshlyDeployed = freshlyDeployed;
            this.message = message;
        }

        public static DeployResult ok(String path, boolean freshlyDeployed) {
            return new DeployResult(true, path, freshlyDeployed, "ok");
        }

        public static DeployResult failed(String message) {
            return new DeployResult(false, "", false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPath() {
            return path;
        }

        public boolean isFreshlyDeployed() {
            return freshlyDeployed;
        }

        public String getMessage() {
            return message;
        }

        public String getDeployedVersion() {
            return deployedVersion;
        }

        public void setDeployedVersion(String deployedVersion) {
            this.deployedVersion = deployedVersion == null ? "" : deployedVersion;
        }

        public boolean isDeployAttempted() {
            return deployAttempted;
        }

        public void setDeployAttempted(boolean deployAttempted) {
            this.deployAttempted = deployAttempted;
        }
    }
}
