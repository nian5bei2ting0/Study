package com.fr.plugin.report.debug.core.fetch;

import com.fr.script.Calculator;
import com.fr.stable.StringUtils;
import com.fr.stable.script.NameSpace;
import com.fr.stable.web.SessionProvider;
import com.fr.web.core.SessionPoolManager;
import com.fr.web.core.TemplateSessionIDInfo;
import com.fr.web.session.SessionLocalManager;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 在取数线程解析当前报表 sessionId。
 */
public final class SessionIdResolver {

    private SessionIdResolver() {
    }

    public static String resolveOnFetchThread() {
        String id = readSessionLocalManagerThreadLocal();
        if (StringUtils.isNotBlank(id)) {
            return id;
        }
        id = readFromSessionProvider();
        if (StringUtils.isNotBlank(id)) {
            return id;
        }
        id = readFromCalculatorNameSpace();
        if (StringUtils.isNotBlank(id)) {
            return id;
        }
        return readFromSessionPoolMap();
    }

    static String resolveForSnapshot(String sessionIdHint) {
        if (StringUtils.isNotBlank(sessionIdHint)) {
            return sessionIdHint.trim();
        }
        return resolveOnFetchThread();
    }

    private static String readSessionLocalManagerThreadLocal() {
        String[] fields = new String[]{"SESSION_ID_THREAD_LOCAL", "sessionIDThreadLocal"};
        for (String name : fields) {
            String id = readThreadLocalString(SessionLocalManager.class, name);
            if (StringUtils.isNotBlank(id)) {
                return id;
            }
        }
        return null;
    }

    private static String readFromSessionProvider() {
        try {
            SessionProvider provider = SessionLocalManager.getSession();
            if (provider != null && StringUtils.isNotBlank(provider.getSessionID())) {
                return provider.getSessionID().trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String readFromCalculatorNameSpace() {
        try {
            NameSpace space = Calculator.getSavedSessionNameSpace();
            if (space == null) {
                return null;
            }
            String[] keys = new String[]{"sessionID", "SESSIONID", "sessionId"};
            for (String key : keys) {
                try {
                    Object value = space.getVariable(key, (Calculator) null);
                    if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                        return String.valueOf(value).trim();
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String readFromSessionPoolMap() {
        try {
            Field field = SessionPoolManager.class.getDeclaredField("sessionIDMap");
            field.setAccessible(true);
            Object map = field.get(null);
            if (!(map instanceof Map) || ((Map<?, ?>) map).isEmpty()) {
                return null;
            }
            if (((Map<?, ?>) map).size() == 1) {
                Object key = ((Map<?, ?>) map).keySet().iterator().next();
                return key == null ? null : String.valueOf(key).trim();
            }
            String threadSession = readSessionLocalManagerThreadLocal();
            if (StringUtils.isNotBlank(threadSession) && ((Map<?, ?>) map).containsKey(threadSession)) {
                return threadSession;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String readThreadLocalString(Class<?> holderType, String fieldName) {
        try {
            Field field = holderType.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object tl = field.get(null);
            if (!(tl instanceof ThreadLocal)) {
                return null;
            }
            Object value = ((ThreadLocal<?>) tl).get();
            return value == null ? null : String.valueOf(value).trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    static TemplateSessionIDInfo loadSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        try {
            return SessionPoolManager.getSessionIDInfor(sessionId, TemplateSessionIDInfo.class);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
