package com.fr.plugin.online.save.service;

import com.fr.decision.authority.AuthorityContext;
import com.fr.decision.authority.controller.DefaultAuthorityController;
import com.fr.decision.authority.data.Authority;
import com.fr.log.FineLoggerFactory;
import com.fr.stable.query.QueryFactory;
import com.fr.stable.query.condition.QueryCondition;
import com.fr.stable.query.restriction.RestrictionFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 等价于决策平台「目录管理」点击保存时的目录树缓存刷新逻辑。
 * <p>
 * UI 保存走 {@code PUT /v10/directory/{id}}（editDirectory），其上标注了
 * {@code @DecisionCacheRefresh(EntryTreeCache, EntryTreeNodeCache)}。
 * 自动化挂载若未触发该注解，需主动刷新这两类缓存，新报表才会出现在目录树中。
 */
public final class DirectorySaveService {

    private static final DirectorySaveService INSTANCE = new DirectorySaveService();

    private DirectorySaveService() {
    }

    public static DirectorySaveService getInstance() {
        return INSTANCE;
    }

    /**
     * 刷新目录树相关缓存（本地 + 集群广播），与目录管理保存按钮副作用一致。
     *
     * @return 已刷新的 cache key 列表
     */
    public Map<String, Object> applyDirectorySave() {
        List<String> cacheKeys = new ArrayList<String>();
        cacheKeys.add(com.fr.decision.webservice.v10.entry.cache.EntryTreeCache.class.getName());
        cacheKeys.add(com.fr.decision.webservice.v10.entry.cache.EntryTreeNodeCache.class.getName());
        com.fr.decision.cache.DecisionCacheManager manager = com.fr.decision.cache.DecisionCacheManager.getInstance();
        for (String key : cacheKeys) {
            manager.refresh(key);
            FineLoggerFactory.getLogger().info("[online-save] refresh cache: {}", key);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("refreshed", cacheKeys);
        result.put("message", "directory entry tree cache refreshed");
        return result;
    }

    /**
     * 按 displayName 查询 fine_authority_object 表，返回匹配的 entry 列表。
     *
     * @param displayName 目录/模板显示名
     * @return 匹配列表，每项含 id、displayName、parentId
     */
    public List<Map<String, Object>> lookupByDisplayName(String displayName) throws Exception {
        DefaultAuthorityController controller = AuthorityContext.getInstance().getAuthorityController();
        QueryCondition condition = QueryFactory.create()
                .addRestriction(RestrictionFactory.eq(Authority.COLUMN_DISPLAY_NAME, displayName));
        List<Authority> list = controller.find(condition);

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (list != null) {
            for (Authority auth : list) {
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("id", auth.getId());
                item.put("displayName", auth.getDisplayName());
                item.put("parentId", auth.getParentId());
                item.put("path", auth.getPath());
                result.add(item);
            }
        }
        return result;
    }
}
