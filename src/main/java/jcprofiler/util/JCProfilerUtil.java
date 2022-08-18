package jcprofiler.util;

import javacard.framework.ISO7816;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class JCProfilerUtil {
    public static final byte INS_PERF_SETSTOP = (byte) 0xf5;

    // Needed to fix a SNAFU, where ISO7816.SW_NO_ERROR is a short, ResponseAPDU::getSW returns int
    // and (short) 0x9000 != 0x9000 ...
    public static final int SW_NO_ERROR = Short.toUnsignedInt(ISO7816.SW_NO_ERROR);

    public static final String APPLET_OUT_DIRNAME = "applet";
    public static final String INSTR_OUT_DIRNAME  = "sources_instr";
    public static final String PERF_OUT_DIRNAME   = "sources_perf";
    public static final String SRC_IN_DIRNAME     = "sources_original";

    public static final String APPLET_AID  = "123456789001";
    public static final String PACKAGE_AID = APPLET_AID + "01";

    private static final Logger log = LoggerFactory.getLogger(JCProfilerUtil.class);

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

        final Optional<CtClass<?>> maybeEntryPoint = entryPoints.stream()
                .filter(cls -> cls.getQualifiedName().equals(className)).findAny();

        if (!maybeEntryPoint.isPresent()) {
            if (spoon.getModel().getElements((CtClass<?> cls) -> cls.getQualifiedName().equals(className)).isEmpty())
                throw new RuntimeException("Class " + className + " specified an an entry point does not exist!");

            throw new RuntimeException("Class " + className + " is not an entry point!");
        }

        final CtClass<?> entryPoint = maybeEntryPoint.get();
        log.debug("Found Applet entry point: {}", entryPoint.getQualifiedName());

        return entryPoint;
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

    public static Path getSourceInputDirectory(final Path workDirPath) {
        return workDirPath.resolve(SRC_IN_DIRNAME);
    }

    public static void moveToSubDirIfNotExists(final Path from, final Path to) {
        if (Files.isDirectory(to)) {
            log.debug("{} already exists. Contents of {} will not be moved.", to, from);
            return;
        }

        try {
            // FIXME: there has to be a nicer solution than this ...
            Files.createDirectories(to);
            try (Stream<Path> s = Files.list(from)) {
                s.filter(p -> !p.equals(to)).forEach(f -> {
                    try {
                        Files.move(f, to.resolve(f.getFileName()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Moved contents of {} to {}", from, to);
    }

    public static String getTimeUnitSymbol(final TimeUnit unit) {
        switch (unit) {
            case nano:
                return "ns";
            case micro:
                return "μs";
            case milli:
                return "ms";
            case sec:
                return "s";
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
    }
}
