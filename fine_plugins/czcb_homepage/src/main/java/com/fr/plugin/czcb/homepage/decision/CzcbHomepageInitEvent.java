package com.fr.plugin.czcb.homepage.decision;

import com.fr.decision.fun.impl.AbstractDecisionInitEventProvider;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageDirectoryRegistrar;
import com.fr.plugin.czcb.homepage.core.CzcbHomepageResourceDeployer;
import com.fr.plugin.transform.FunctionRecorder;

import javax.servlet.http.HttpServletRequest;

/**
 * 决策初始化：ensureDeployed 在 marker 命中时为轻量文件检查；
 * 目录挂载带 TTL，避免每次请求打 EntryService。
 */
@FunctionRecorder
public class CzcbHomepageInitEvent extends AbstractDecisionInitEventProvider {

    @Override
    public void before(HttpServletRequest req) {
        try {
            CzcbHomepageResourceDeployer.ensureDeployed(false);
            CzcbHomepageDirectoryRegistrar.ensureDirectoryLink();
        } catch (Throwable ignored) {
        }
    }
}
