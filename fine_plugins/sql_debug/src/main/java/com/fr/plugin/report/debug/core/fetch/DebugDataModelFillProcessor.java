package com.fr.plugin.report.debug.core.fetch;
import com.fr.plugin.report.debug.core.config.DebugAssistantTraceGate;
import com.fr.plugin.report.debug.core.registry.DatasetSqlExecutionRegistry;

import com.fr.general.data.DataModel;
import com.fr.measure.metric.DBMetric;
import com.fr.script.Calculator;
import com.fr.stable.StringUtils;
import com.fr.stable.bridge.ObjectHolder;
import com.fr.stable.fun.impl.AbstractDataModelFillProcessor;
import com.fr.stable.script.NameSpace;

/**
 * 挂接 FR DataModel 填充链（部分版本 preview 走此扩展点而非 SessionCachedDataModelProcessor）。
 * <p>
 * 不得再 delegate 其他 Processor，原因同 {@link DebugSessionCachedDataModelProcessor}。
 */
public class DebugDataModelFillProcessor extends AbstractDataModelFillProcessor {

    @Override
    public int layerIndex() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ObjectHolder getOrCreate(ObjectHolder holder, Object context) {
        if (holder == null) {
            return null;
        }
        recordFromHolder(holder, context);
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

    private static void recordFromHolder(ObjectHolder holder, Object context) {
        if (holder == null) {
            return;
        }
        String sessionId = resolveSessionId(context);
        String datasetKey = resolveDatasetKey(holder);
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(datasetKey)) {
            return;
        }
        if (!DebugAssistantTraceGate.shouldTraceSession(sessionId)) {
            return;
        }
        DebugAssistantTraceGate.ensureHookInstalled();
        DatasetSqlExecutionRegistry.record(sessionId, datasetKey, holder);
    }

    private static String resolveSessionId(Object context) {
        if (!(context instanceof Calculator)) {
            return null;
        }
        Calculator calculator = (Calculator) context;
        try {
            NameSpace space = Calculator.getSavedSessionNameSpace();
            if (space != null) {
                Object sid = space.getVariable("sessionID", calculator);
                if (sid != null) {
                    return String.valueOf(sid);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String resolveDatasetKey(ObjectHolder holder) {
        try {
            DataModel model = holder.get(DataModel.class);
            if (model == null) {
                return null;
            }
            DBMetric metric = model.getMetric();
            if (metric != null && StringUtils.isNotBlank(metric.getDsName())) {
                return metric.getDsName();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
