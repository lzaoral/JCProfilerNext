package jcprofiler.util;

import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;
import java.util.Optional;

public class JCProfilerUtil {
    public static final byte INS_PERF_SETSTOP = (byte) 0xf5;

    // static class!
    private JCProfilerUtil() {}

    // entry points
    public static CtClass<?> getEntryPoint(final SpoonAPI spoon, final String className) {
        final List<CtClass<?>> entryPoints = spoon.getModel().getElements(JCProfilerUtil::isClsEntryPoint);
        if (entryPoints.isEmpty())
            throw new RuntimeException("None of the provided classes is an entry point!");

        if (className.isEmpty()) {
            if (entryPoints.size() > 1)
                throw new RuntimeException("More entry points detected but none was specified to be used! " +
                        "Use the -e/--entry-point argument." +
                        String.format("%nDetected entry points: %s", entryPoints));

            return entryPoints.get(0);
        }

        final Optional<CtClass<?>> entryPoint = entryPoints.stream()
                .filter(cls -> cls.getQualifiedName().equals(className)).findAny();

        if (!entryPoint.isPresent()) {
            if (spoon.getModel().getElements((CtClass<?> cls) -> cls.getQualifiedName().equals(className)).isEmpty())
                throw new RuntimeException("Class " + className + " specified an an entry point does not exist!");

            throw new RuntimeException("Class " + className + " is not an entry point!");
        }

        return entryPoint.get();
    }

    public static boolean isClsEntryPoint(final CtClass<?> cls) {
        final CtTypeReference<?> parent = cls.getSuperclass();
        return parent != null && parent.getQualifiedName().equals("javacard.framework.Applet");
    }
}
