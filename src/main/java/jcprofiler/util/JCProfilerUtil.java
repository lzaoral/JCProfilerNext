package jcprofiler.util;

import javacard.framework.APDU;
import javacard.framework.ISO7816;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

   /**
    * Checks that a type in an entry point:
    *   1. It inherits from the javacard.framework.Applet abstract class,
    *   2. At least one of the classes in the inheritance chain implements
    *      the void process(javacard.framework.APDU) method.
    *
    * @param type - type to be checked
    * @return true if the type in an applet entry point otherwise false
    */
    public static boolean isTypeEntryPoint(final CtType<?> type) {
        final List<CtTypeReference<?>> inheritanceChain = new ArrayList<>();
        CtTypeReference<?> typeRef = type.getReference();
        while (typeRef != null) {
            inheritanceChain.add(typeRef);
            typeRef = typeRef.getSuperclass();
        }

        return inheritanceChain.stream().allMatch(CtTypeInformation::isClass) &&
               inheritanceChain.stream().anyMatch(c -> c.getQualifiedName().equals("javacard.framework.Applet")) &&
               inheritanceChain.stream().anyMatch(c -> {
                   final CtTypeReference<?> APDURef = c.getFactory().createCtTypeReference(APDU.class);
                   final CtMethod<APDU> processMethod = c.getTypeDeclaration().getMethod("process", APDURef);

                   return processMethod != null && !processMethod.isAbstract() &&
                          processMethod.getType().equals(c.getFactory().createCtTypeReference(Void.TYPE));
               });
    }

    public static CtClass<?> getEntryPoint(final SpoonAPI spoon, final String className) {
        final List<CtClass<?>> entryPoints = spoon.getModel().getElements(JCProfilerUtil::isTypeEntryPoint);
        if (entryPoints.isEmpty())
            throw new RuntimeException("None of the provided classes is an entry point!");

        if (className.isEmpty()) {
            if (entryPoints.size() > 1)
                throw new RuntimeException(String.format(
                        "More entry points detected but none was specified to be used! " +
                        "Use the -e/--entry-point argument.%nDetected entry points: %s",
                        entryPoints.stream().map(CtTypeInformation::getQualifiedName).collect(Collectors.toList())));

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

    // profiled method detection
    public static CtMethod<?> getProfiledMethod(final SpoonAPI spoon, final String methodName) {
        if (methodName == null)
            throw new RuntimeException("--method argument was not provided!");

        final String[] split = methodName.split("#");
        final int lastIdx = split.length - 1;

        final List<CtMethod<?>> methods = spoon.getModel().getElements((CtMethod<?> m) -> {
            boolean sameSignature = split[lastIdx].contains("(") ? m.getSignature().equals(split[lastIdx])
                                                                 : m.getSimpleName().equals(split[lastIdx]);
            return sameSignature && (split.length == 1 || m.getDeclaringType().getQualifiedName().equals(split[0]));
        });

        if (methods.isEmpty())
            throw new RuntimeException(String.format(
                    "None of the provided classes contain %s method!", methodName));

        final List<String> containingClassNames = methods.stream().map(CtMethod::getDeclaringType)
                .map(CtType::getQualifiedName).distinct().sorted().collect(Collectors.toList());

        final List<String> methodSignatures = methods.stream().map(CtMethod::getSignature)
                .distinct().sorted().collect(Collectors.toList());

        // every method found is not overloaded and is in a distinct class
        if (containingClassNames.size() > 1 && methodSignatures.size() == 1)
            throw new RuntimeException(String.format(
                    "More of the provided classes contain the %s method!%n" +
                    "Please, specify the --method parameter in the 'class#%s' format where class is one of:%n%s",
                    methodName, methodName, containingClassNames));

        // overloaded methods are in a single class
        if (containingClassNames.size() == 1 && methodSignatures.size() > 1)
            throw new RuntimeException(String.format(
                    "More %s methods with distinct signatures found in the %s class!%n" +
                    "Please, add the corresponding signature to the --method parameter%nFound: %s",
                    split[split.length - 1], containingClassNames.get(0), methodSignatures));

        // overloaded methods in more classes
        if (containingClassNames.size() > 1 && methodSignatures.size() > 1)
            throw new RuntimeException(String.format(
                    "More %s methods with distinct signatures found in more classes!%n" +
                    "Please, use one of the following values as an argument to the --method parameter:%n%s",
                    methodName, methods.stream().map(JCProfilerUtil::getFullSignature).sorted()
                            .collect(Collectors.toList())));

        // only a single method with such name exists
        final CtMethod<?> method = methods.get(0);
        if (method.getBody() == null)
            throw new RuntimeException(String.format(
                    "Found the %s method but it has no body! Found in class %s.",
                    methodName, containingClassNames.get(0)));

        return method;
    }

    static public String getFullSignature(final CtMethod<?> method) {
        return method.getDeclaringType().getQualifiedName() + "#" + method.getSignature();
    }

    // trap mangling
    static public String getTrapNamePrefix(final CtMethod<?> method) {
        final String prefix = "TRAP_" + getFullSignature(method);

        // list may not be exhaustive
        return prefix.replace('.', '_') // used in qualified types
                .replace("#", "_hash_") // method member
                .replace("$", "_dol_") // nested class
                .replace(",", "__") // args delimiter
                .replace("()", "_argb_arge")
                .replace("(", "_argb_")
                .replace(")", "_arge")
                .replace("<", "_genb_")
                .replace(">", "_gene_")
                .replace("[]", "_arr"); // used in arrays
    }

    // Path utils
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

    public static Path checkFile(final Path path, final Stage stage) {
        if (!Files.exists(path))
            throw new RuntimeException(String.format(
                    "%s does not exist! Please execute the %s stage first.", path, stage));

        return path;
    }

    public static Path checkDirectory(final Path path, final Stage stage) {
        checkFile(path, stage);

        if (!Files.isDirectory(path))
            throw new RuntimeException(String.format(
                    "%s is not a directory! Please execute the %s stage first.", path, stage));

        try (final Stream<Path> files = Files.list(path)) {
            if (!files.findFirst().isPresent())
                throw new RuntimeException(String.format(
                        "The %s directory is empty! Please execute the %s stage first.", path, stage));

            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static void recreateDirectory(Path appletDir) {
        try {
            if (Files.exists(appletDir)) {
                FileUtils.deleteDirectory(appletDir.toFile());
                log.debug("Deleted existing {}.", appletDir);
            }

            Files.createDirectories(appletDir);
            log.debug("Created new {}.", appletDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // CSV
    public static CSVFormat getCsvFormat() {
        return Builder.create(CSVFormat.DEFAULT).setCommentMarker('#').build();
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
