package com.fr.plugin.report.debug.core.snapshot;

import com.fr.base.Formula;
import com.fr.base.Parameter;
import com.fr.base.ParameterHelper;
import com.fr.data.impl.DBTableData;
import com.fr.script.Calculator;
import com.fr.stable.ParameterProvider;
import com.fr.stable.StringUtils;
import com.fr.web.core.TemplateSessionIDInfo;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 解析数据集参数展示值（含报表传入值、公式、默认值）。
 */
final class ReportDebugParameterResolver {

    private ReportDebugParameterResolver() {
    }

    static String resolveDisplayValue(String name, ParameterProvider provider, ParameterProvider[] processedProviders,
                                      String queryTemplate, DBTableData dbTableData, TemplateSessionIDInfo session,
                                      Calculator calculator, boolean datasetExecuted) {
        String fromProvider = resolveFromProvider(provider, calculator);
        if (StringUtils.isNotBlank(fromProvider)) {
            return fromProvider;
        }
        String fromTemplate = resolveViaTemplateEngine(name, queryTemplate, processedProviders);
        if (StringUtils.isNotBlank(fromTemplate)) {
            return fromTemplate;
        }
        if (dbTableData != null) {
            String fromDb = resolveFromDbProcessParameters(dbTableData, name, session);
            if (StringUtils.isNotBlank(fromDb)) {
                return fromDb;
            }
        }
        String fromSession = resolveFromSession(session, name);
        if (StringUtils.isNotBlank(fromSession)) {
            return fromSession;
        }
        String fromCalc = resolveFromCalculator(calculator, name);
        if (StringUtils.isNotBlank(fromCalc)) {
            return fromCalc;
        }
        if (datasetExecuted) {
            return resolveFromSession(session, name);
        }
        return "";
    }

    static String resolveOriginalValue(ParameterProvider provider, Calculator calculator) {
        return resolveFromProvider(provider, calculator);
    }

    /**
     * 与 {@link ParameterHelper#analyze4Templatee} 一致：从已 process 的参数数组渲染占位符取值。
     */
    private static String resolveViaTemplateEngine(String name, String queryTemplate,
                                                   ParameterProvider[] processedProviders) {
        if (StringUtils.isBlank(name) || processedProviders == null || processedProviders.length == 0) {
            return "";
        }
        String[] candidates = buildPlaceholderCandidates(name, queryTemplate);
        for (String placeholder : candidates) {
            String v = tryAnalyzeTemplate(placeholder, processedProviders);
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

    private static String[] buildPlaceholderCandidates(String name, String queryTemplate) {
        String n = name.trim();
        if (StringUtils.isNotBlank(queryTemplate) && queryTemplate.contains("${" + n + "}")) {
            return new String[]{"${" + n + "}", "$" + n, "$[" + n + "]"};
        }
        if (StringUtils.isNotBlank(queryTemplate) && queryTemplate.contains("$" + n)) {
            return new String[]{"$" + n, "${" + n + "}", "$[" + n + "]"};
        }
        return new String[]{"${" + n + "}", "$" + n, "$[" + n + "]"};
    }

    private static String tryAnalyzeTemplate(String placeholder, ParameterProvider[] providers) {
        if (StringUtils.isBlank(placeholder)) {
            return "";
        }
        try {
            String v = ParameterHelper.analyze4Templatee(placeholder, providers);
            if (isRenderedValue(placeholder, v)) {
                return normalizeRendered(v);
            }
        } catch (Throwable ignored) {
        }
        try {
            String v = ParameterHelper.analyzeCurrentContextTableData4Templatee(placeholder, providers);
            if (isRenderedValue(placeholder, v)) {
                return normalizeRendered(v);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static boolean isRenderedValue(String placeholder, String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return !placeholder.equals(value);
    }

    private static String normalizeRendered(String value) {
        if (value == null) {
            return "";
        }
        String s = value.trim();
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            if (s.length() >= 2) {
                s = s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private static String resolveFromDbProcessParameters(DBTableData db, String name, TemplateSessionIDInfo session) {
        if (db == null || StringUtils.isBlank(name)) {
            return "";
        }
        try {
            Calculator calculator = createExecuteCalculator(session);
            Parameter[] params = invokeDbProcessParameters(db, calculator);
            if (params == null) {
                return "";
            }
            for (Parameter param : params) {
                if (param == null || !name.equals(param.getName())) {
                    continue;
                }
                String text = param.valueToString();
                if (StringUtils.isNotBlank(text)) {
                    return text.trim();
                }
                return formatObject(param.getValue());
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static Parameter[] invokeDbProcessParameters(DBTableData db, Calculator calculator) {
        try {
            Method method = DBTableData.class.getDeclaredMethod("processParameters", Calculator.class);
            method.setAccessible(true);
            return (Parameter[]) method.invoke(db, calculator);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Calculator createExecuteCalculator(TemplateSessionIDInfo session) {
        Map<String, Object> executeMap = null;
        try {
            executeMap = session.getParameterMap4ExecuteAll();
            if (executeMap == null || executeMap.isEmpty()) {
                executeMap = session.getParameterMap4Execute();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (executeMap != null && !executeMap.isEmpty()) {
                return session.createSessionCalculator(null, null, executeMap);
            }
        } catch (Throwable ignored) {
        }
        Calculator calculator = session.createSessionCalculator(null, null);
        try {
            Map<String, Object> all = session.getAllPara();
            if (all != null) {
                for (Map.Entry<String, Object> entry : all.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        calculator.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return calculator;
    }

    private static String resolveFromProvider(ParameterProvider provider, Calculator calculator) {
        if (provider == null) {
            return "";
        }
        try {
            String text = provider.valueToString();
            if (StringUtils.isNotBlank(text)) {
                return text.trim();
            }
        } catch (Throwable ignored) {
        }
        return evaluateObject(provider.getValue(), calculator);
    }

    private static String resolveFromSession(TemplateSessionIDInfo session, String name) {
        if (session == null || StringUtils.isBlank(name)) {
            return "";
        }
        try {
            Object direct = session.getParameterValue(name);
            String formatted = formatObject(direct);
            if (StringUtils.isNotBlank(formatted)) {
                return formatted;
            }
        } catch (Throwable ignored) {
        }
        String[] found = findInMaps(session, name);
        if (StringUtils.isNotBlank(found[0])) {
            return found[0];
        }
        return "";
    }

    private static String[] findInMaps(TemplateSessionIDInfo session, String name) {
        Map<String, Object>[] maps = new Map[]{
                safeMap(session, "getParameterMap4Execute"),
                safeMap(session, "getParameterMap4ExecuteAll"),
                safeMap(session, "getAllPara"),
                safeMap(session, "getParameterMap"),
                safeMap(session, "getOriginalParameterMap"),
                safeMap(session, "getParameterMapFromTpl", Boolean.FALSE)
        };
        for (Map<String, Object> map : maps) {
            if (map != null && map.containsKey(name)) {
                String v = formatObject(map.get(name));
                if (StringUtils.isNotBlank(v)) {
                    return new String[]{v};
                }
            }
        }
        return new String[]{""};
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeMap(TemplateSessionIDInfo session, String method, Object... args) {
        try {
            if ("getParameterMap4Execute".equals(method)) {
                return session.getParameterMap4Execute();
            }
            if ("getParameterMap4ExecuteAll".equals(method)) {
                return session.getParameterMap4ExecuteAll();
            }
            if ("getAllPara".equals(method)) {
                return session.getAllPara();
            }
            if ("getParameterMap".equals(method)) {
                return session.getParameterMap();
            }
            if ("getOriginalParameterMap".equals(method)) {
                return session.getOriginalParameterMap();
            }
            if ("getParameterMapFromTpl".equals(method)) {
                boolean flag = args.length > 0 && Boolean.FALSE.equals(args[0]);
                return session.getParameterMapFromTpl(flag);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Map<String, Object> safeMap(TemplateSessionIDInfo session, String method) {
        return safeMap(session, method, new Object[0]);
    }

    private static String resolveFromCalculator(Calculator calculator, String name) {
        if (calculator == null || StringUtils.isBlank(name)) {
            return "";
        }
        try {
            Object v = calculator.resolveVariable(name);
            return formatObject(v);
        } catch (Throwable ignored) {
        }
        try {
            return calculator.evalValue(name).toString();
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static String evaluateObject(Object value, Calculator calculator) {
        if (value == null) {
            return "";
        }
        if (value instanceof Formula) {
            return evaluateFormula((Formula) value, calculator);
        }
        if (value instanceof ParameterProvider) {
            return resolveFromProvider((ParameterProvider) value, calculator);
        }
        String text = String.valueOf(value).trim();
        if (text.startsWith("=") && calculator != null) {
            try {
                Object eval = calculator.evalValue(text);
                if (eval != null) {
                    return formatObject(eval);
                }
            } catch (Throwable ignored) {
            }
            return text;
        }
        return text;
    }

    private static String evaluateFormula(Formula formula, Calculator calculator) {
        if (formula == null) {
            return "";
        }
        String content = formula.getContent();
        if (StringUtils.isBlank(content)) {
            return "";
        }
        if (calculator == null) {
            return "=" + content;
        }
        try {
            Object eval = calculator.evalValue(content);
            if (eval != null) {
                return formatObject(eval);
            }
        } catch (Throwable ignored) {
        }
        return "=" + content;
    }

    private static String formatObject(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Formula) {
            Formula f = (Formula) value;
            String c = f.getContent();
            return StringUtils.isBlank(c) ? "" : "=" + c;
        }
        if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(formatObject(arr[i]));
            }
            return sb.toString();
        }
        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object item : (Iterable<?>) value) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(formatObject(item));
            }
            return sb.toString();
        }
        String s = String.valueOf(value).trim();
        return "null".equalsIgnoreCase(s) ? "" : s;
    }
}
