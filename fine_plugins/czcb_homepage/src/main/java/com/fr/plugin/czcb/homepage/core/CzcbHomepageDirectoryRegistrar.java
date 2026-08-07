package com.fr.plugin.czcb.homepage.core;

import com.fr.decision.authority.base.constant.AuthorityStaticItemId;
import com.fr.decision.authority.base.constant.DeviceType;
import com.fr.decision.authority.base.constant.ExpandRecordType;
import com.fr.decision.webservice.bean.entry.EntryBean;
import com.fr.decision.webservice.bean.entry.LinkBean;
import com.fr.decision.webservice.v10.entry.EntryService;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.StringUtils;

import java.util.Collection;

/**
 * 目录管理幂等挂载。成功探测后短时 TTL 跳过重复查询；删除链接后 TTL 到期可重建。
 */
public final class CzcbHomepageDirectoryRegistrar {

    private static final Object LOCK = new Object();
    private static final long OK_TTL_MS = 60_000L;
    private static volatile long lastOkAt;

    private CzcbHomepageDirectoryRegistrar() {
    }

    public static void invalidateCache() {
        lastOkAt = 0L;
    }

    public static void ensureDirectoryLink() {
        long now = System.currentTimeMillis();
        if (now - lastOkAt < OK_TTL_MS) {
            return;
        }
        synchronized (LOCK) {
            now = System.currentTimeMillis();
            if (now - lastOkAt < OK_TTL_MS) {
                return;
            }
            try {
                CzcbHomepageResourceDeployer.ensureDeployed(false);
                EntryService entryService = EntryService.getInstance();
                String parentId = AuthorityStaticItemId.DEC_DIRECTORY_ROOT_ID;
                String name = CzcbHomepageConstants.DIRECTORY_LINK_NAME;
                String path = CzcbHomepageConstants.HOMEPAGE_WEB_PATH;

                if (linkAlreadyPresent(entryService, parentId, name, path)) {
                    lastOkAt = System.currentTimeMillis();
                    return;
                }

                LinkBean link = new LinkBean();
                link.setText(name);
                link.setPath(path);
                link.setDescription(CzcbHomepageConstants.DIRECTORY_LINK_DESC);
                link.setDeviceType(DeviceType.PC_VALUE | DeviceType.TABLE_VALUE | DeviceType.PHONE_VALUE);
                entryService.addLink(parentId, link, "");
                lastOkAt = System.currentTimeMillis();
                FineLoggerFactory.getLogger().info(
                        "[czcb-homepage] directory link created under {}: {} -> {}",
                        parentId, name, path);
            } catch (Throwable ex) {
                FineLoggerFactory.getLogger().warn(
                        "[czcb-homepage] ensure directory link failed: {}", ex.getMessage());
            }
        }
    }

    private static boolean linkAlreadyPresent(EntryService entryService, String parentId,
                                              String name, String path) {
        try {
            Object raw = entryService.getEntries("", parentId, ExpandRecordType.LINK_TYPE);
            if (raw instanceof Collection) {
                for (Object item : (Collection<?>) raw) {
                    if (!(item instanceof EntryBean)) {
                        continue;
                    }
                    EntryBean entry = (EntryBean) item;
                    String entryPath = entry.getPath();
                    String entryText = entry.getText();
                    if (StringUtils.isNotBlank(entryPath) && pathEquals(entryPath, path)) {
                        return true;
                    }
                    if (StringUtils.isNotBlank(entryText) && name.equals(entryText.trim())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ex) {
            FineLoggerFactory.getLogger().warn(
                    "[czcb-homepage] list directory links failed, fallback to isNameExisted: {}",
                    ex.getMessage());
        }
        try {
            return entryService.isNameExisted(parentId, name);
        } catch (Throwable ex) {
            return false;
        }
    }

    private static boolean pathEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String na = a.trim().replace('\\', '/');
        String nb = b.trim().replace('\\', '/');
        return na.equalsIgnoreCase(nb);
    }
}
