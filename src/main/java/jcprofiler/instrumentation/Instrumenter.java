package jcprofiler.instrumentation;

import jcprofiler.args.Args;
import jcprofiler.instrumentation.processors.InsertTrapProcessor;
import jcprofiler.instrumentation.processors.ModifyEntryPointProcessor;
import jcprofiler.util.JCProfilerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.Launcher;
import spoon.OutputType;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Instrumenter {
    private final Args args;

    private static final List<String> generatedClasses = Arrays.asList("PM", "PMC");
    private static final Logger log = LoggerFactory.getLogger(Instrumenter.class);

    public Instrumenter(final Args args) {
        this.args = args;
    }

    // TODO: logging
    public void process() {
        Launcher spoon = new Launcher();

        // prepare and check the model
        addMissingClasses(spoon);
        buildModel(spoon);
        checkArguments(spoon);

        // instrument the model
        spoon.addProcessor(new ModifyEntryPointProcessor(args));
        spoon.addProcessor(new InsertTrapProcessor(args));

        log.info("Instrumenting existing classes.");
        spoon.process();

        // save the result
        spoon.getEnvironment().setOutputType(OutputType.CLASSES);
        spoon.setSourceOutputDirectory(JCProfilerUtil.getInstrOutputDirectory(args.workDir).toFile());

        log.info("Saving instrumented classes.");
        spoon.prettyprint();

        // TODO sanity check that all PMC members are unique
    }

    private void buildModel(final Launcher spoon) {
        setupSpoon(spoon, args);
        spoon.addInputResource(JCProfilerUtil.getSourceInputDirectory(args.workDir).toString());

        log.debug("Building SPOON model.");
        spoon.buildModel();
    }

    // TODO: better exception handling
    private void checkArguments(final Launcher spoon) {
        log.info("Validating '--entry-point' and '--method' arguments.");

        // validate args.entryPoint
        JCProfilerUtil.getEntryPoint(spoon, args.entryPoint);

        // validate args.method
        JCProfilerUtil.getProfiledMethod(spoon, args.method);
    }

    public static void setupSpoon(final SpoonAPI spoon, final Args args) {
        log.debug("Setting SPOON's environment.");

        // TODO: uncommenting this might lead to SPOON crashes!
        // spoon.getEnvironment().setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(spoon.getEnvironment()));

        spoon.getEnvironment().setNoClasspath(false);
        spoon.getEnvironment().setAutoImports(true);
        spoon.getEnvironment().setCopyResources(false);
        spoon.getEnvironment().setSourceClasspath(args.jcSDK.getApiJars()
                .stream().map(File::getAbsolutePath).toArray(String[]::new));
    }

    private void addMissingClasses(final Launcher spoon) {
        log.info("Generating additional classes.");

        // this atrocity seems to be required as SPOON does not allow a module rebuild :(
        final Launcher tmpSpoon = new Launcher();
        buildModel(tmpSpoon);

        final List<CtClass<?>> classes = tmpSpoon.getModel().getElements(CtClass.class::isInstance);
        // TODO: will this always work? what about nested classes?

        final List<CtPackage> pkgs = classes.stream().map(CtClass::getPackage).distinct().collect(Collectors.toList());
        log.debug("Found following packages in sources: {}", pkgs);

        // TODO: does even javacard allow more?
        if (pkgs.size() != 1)
            throw new RuntimeException("Only one package is allowed! Found: " + pkgs);

        final String packageName = pkgs.get(0).getQualifiedName();
        if (packageName.isEmpty())
            throw new RuntimeException("Usage of the default package detected! " +
                    "This is unsupported by the CAP converter.");

        for (final String className : generatedClasses) {
            log.debug("Looking for existing {} class.", className);
            final long count = classes.stream().filter(c -> c.getSimpleName().equals(className)).count();
//            if (count > 1)
            if (count > 0)
                throw new RuntimeException(String.format("Code contains %d classes named %s", count, className));

//            if (count == 1) {
//                if (!args.update)
//                    throw new RuntimeException(String.format(
//                            "Code already contains %s class and -u or --update was not specified!", className));
//
//                System.out.printf("Will update existing %s class.%n", className);
//                continue;
//            }

            log.debug("Class {} not found.", className);
            try {
                final String filename = String.format("%s.java", className);
                // getClass().getResource() does not work when executed from JAR
                final InputStream is = Objects.requireNonNull(getClass().getResourceAsStream(filename));
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    final String src = br.lines()
                            .collect(Collectors.joining(System.lineSeparator()))
                            .replace("@PACKAGE@", packageName);
                    spoon.addInputResource(new VirtualFile(src, filename));
                }
                log.debug("Successfully generated new {} class.", className);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
