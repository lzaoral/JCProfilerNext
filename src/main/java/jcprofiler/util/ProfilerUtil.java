package jcprofiler.util;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

public class ProfilerUtil {
    public static final byte INS_PERF_SETSTOP = (byte) 0xf5;

    // static class!
    private ProfilerUtil() {}

    public static List<CtClass<?>> getEntryPoints(final CtModel model) {
        return model.getElements(ProfilerUtil::isClsEntryPoint);
    }

    public static boolean isClsEntryPoint(final CtClass<?> cls) {
        final CtTypeReference<?> parent = cls.getSuperclass();
        return parent != null && parent.getQualifiedName().equals("javacard.framework.Applet");
    }
}
