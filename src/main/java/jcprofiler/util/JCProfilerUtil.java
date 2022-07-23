package jcprofiler.util;

import javacard.framework.ISO7816;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class JCProfilerUtil {
    public static final byte INS_PERF_SETSTOP = (byte) 0xf5;

    // Needed to fix a SNAFU, where ISO7816.SW_NO_ERROR is a short, ResponseAPDU::getSW returns int
    // and (short) 0x9000 != 0x9000 ...
    public static final int SW_NO_ERROR = ISO7816.SW_NO_ERROR & 0xFFFF;

    public static final String APPLET_OUT_DIRNAME = "applet";
    public static final String INSTR_OUT_DIRNAME  = "sources_instr";
    public static final String PERF_OUT_DIRNAME   = "sources_perf";

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

    public static Path getInstrOutputDirectory(final Path workDirPath) {
        return workDirPath.resolve(INSTR_OUT_DIRNAME);
    }

    public static Path getPerfOutputDirectory(final Path workDirPath) {
        return workDirPath.resolve(PERF_OUT_DIRNAME);
    }

    public static Path getAppletOutputDirectory(final Path workDirPath) {
        return workDirPath.resolve(APPLET_OUT_DIRNAME);
    }
}
