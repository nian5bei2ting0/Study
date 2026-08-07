package com.fr.plugin.report.debug.core.fetch;

import com.fr.log.FetchDataTimeLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 替换引擎 {@code AbstractDBDataModel.logger}（static final ThreadLocal），在取数结束时主动登记。
 */
public final class FetchExecutionHook {

    private static volatile boolean installed;
    private static volatile String installError = "";

    private FetchExecutionHook() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        synchronized (FetchExecutionHook.class) {
            if (installed) {
                return;
            }
            try {
                Class<?> modelClass = resolveAbstractDbModelClass();
                Field field = modelClass.getDeclaredField("logger");
                field.setAccessible(true);
                ThreadLocal<FetchDataTimeLogger> wrapper = createWrapperThreadLocal();
                if (!forceSetStaticFinal(field, wrapper)) {
                    installError = "cannot_assign_static_final_logger";
                    return;
                }
                installed = true;
                installError = "";
            } catch (Throwable t) {
                installError = t.getClass().getSimpleName() + ":" + safeMessage(t);
            }
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static String getInstallError() {
        return installError == null ? "" : installError;
    }

    private static Class<?> resolveAbstractDbModelClass() throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = FetchExecutionHook.class.getClassLoader();
        }
        return Class.forName("com.fr.data.impl.AbstractDBDataModel", false, cl);
    }

    @SuppressWarnings("unchecked")
    private static boolean forceSetStaticFinal(Field field, ThreadLocal<FetchDataTimeLogger> wrapper) throws Exception {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, wrapper);
            return verifyInstalled(field, wrapper);
        } catch (NoSuchFieldException modifiersMissing) {
            return forceSetWithUnsafe(field, wrapper);
        }
    }

    private static boolean forceSetWithUnsafe(Field field, ThreadLocal<FetchDataTimeLogger> wrapper) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            java.lang.reflect.Method staticFieldBase = unsafeClass.getDeclaredMethod("staticFieldBase", Field.class);
            java.lang.reflect.Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
            java.lang.reflect.Method putObject = unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class);
            Object base = staticFieldBase.invoke(unsafe, field);
            long offset = ((Long) staticFieldOffset.invoke(unsafe, field)).longValue();
            putObject.invoke(unsafe, base, offset, wrapper);
            return verifyInstalled(field, wrapper);
        } catch (Throwable t) {
            installError = "unsafe_failed:" + safeMessage(t);
            return false;
        }
    }

    private static boolean verifyInstalled(Field field, ThreadLocal<FetchDataTimeLogger> expected) {
        try {
            Object current = field.get(null);
            return current == expected;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ThreadLocal<FetchDataTimeLogger> createWrapperThreadLocal() {
        return new ThreadLocal<FetchDataTimeLogger>() {
            @Override
            public FetchDataTimeLogger get() {
                FetchDataTimeLogger logger = super.get();
                if (!(logger instanceof RecordingFetchDataTimeLogger)) {
                    logger = new RecordingFetchDataTimeLogger();
                    set(logger);
                }
                return logger;
            }
        };
    }

    private static String safeMessage(Throwable t) {
        if (t == null || t.getMessage() == null) {
            return "";
        }
        String msg = t.getMessage();
        return msg.length() > 120 ? msg.substring(0, 120) : msg;
    }
}
