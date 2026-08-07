package com.fr.plugin.report.debug.core.fetch;
import com.fr.plugin.report.debug.core.config.DebugAssistantTraceGate;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;

import com.fr.general.data.DataModel;
import com.fr.stable.bridge.ObjectHolder;
import com.fr.stable.fun.impl.AbstractSessionCachedDataModelProcessor;

/**
 * 挂接 FR 会话数据集缓存创建链，在取数完成时登记 SQL 与耗时。
 * <p>
 * 引擎已按 {@link #layerIndex()} 串行调用各 Processor，此处不得再 delegate，否则取数链重复执行
 * （开启报表引擎属性等模式下会指数级放大，导致预览页卡死/崩溃）。
 */
public class DebugSessionCachedDataModelProcessor extends AbstractSessionCachedDataModelProcessor {

    @Override
    public int layerIndex() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ObjectHolder getOrCreate(String sessionId, String datasetKey, ObjectHolder holder) {
        if (holder == null) {
            return null;
        }
        if (DebugAssistantTraceGate.shouldTraceSession(sessionId)) {
            DebugAssistantTraceGate.ensureHookInstalled();
            DatasetSqlExecutionRegistry.record(sessionId, datasetKey, holder);
        }
        return holder;
    }

    @Override
    public boolean isSupportedType(ObjectHolder holder) {
        if (holder == null) {
            return false;
        }
        try {
            return holder.get(DataModel.class) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
