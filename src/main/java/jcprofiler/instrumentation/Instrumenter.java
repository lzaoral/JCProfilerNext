package jcprofiler.instrumentation;

import jcprofiler.args.Args;
import jcprofiler.instrumentation.processors.SpoonWorkarounds;
import jcprofiler.instrumentation.processors.InsertTrapProcessor;
import jcprofiler.instrumentation.processors.ModifyEntryPointProcessor;
import jcprofiler.util.JCProfilerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.javacard.JavaCardSDK;

import spoon.Launcher;
import spoon.OutputType;
import spoon.SpoonAPI;
import spoon.compiler.ModelBuildingException;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtFieldReference;
import spoon.support.compiler.VirtualFile;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

// TODO: support already instrumented stuff
public class Instrumenter {
    private final Args args;

    private static final List<String> generatedClasses = Arrays.asList("PM", "PMC");
    private static final Logger log = LoggerFactory.getLogger(Instrumenter.class);

    public Instrumenter(final Args args) {
        this.args = args;
    }

    public void process() {
        // always recreate the output directory
        final Path outputDir = JCProfilerUtil.getInstrOutputDirectory(args.workDir);
        JCProfilerUtil.recreateDirectory(outputDir);

        // prepare and check the model
        final Launcher spoon = new Launcher();
        addMissingClasses(spoon);
        buildModel(spoon);
        checkArguments(spoon);

        // instrument the model
        spoon.addProcessor(new ModifyEntryPointProcessor(args));
        spoon.addProcessor(new InsertTrapProcessor(args));

        // add workarounds for bugs in SPOON
        spoon.addProcessor(new SpoonWorkarounds.FixNestedClassImportProcessor());
        spoon.addProcessor(new SpoonWorkarounds.FixStaticMethodImportProcessor());
        spoon.addProcessor(new SpoonWorkarounds.FixStaticFieldImportProcessor());

        log.info("Instrumenting existing classes.");
        spoon.process();

        // save the result
        spoon.getEnvironment().setOutputType(OutputType.CLASSES);
        spoon.setSourceOutputDirectory(outputDir.toFile());

        log.info("Saving instrumented classes.");
        spoon.prettyprint();

        // check that all PMC members are unique
        checkPMC(spoon);
    }

    private void buildModel(final Launcher spoon) {
        setupSpoon(spoon, args);
        spoon.addInputResource(JCProfilerUtil.getSourceInputDirectory(args.workDir).toString());

        log.debug("Building SPOON model.");
        try {
            spoon.buildModel();
        } catch (ModelBuildingException e) {
            if (!e.getMessage().matches("The import .* cannot be resolved at .*"))
                throw e;

            throw new RuntimeException(
                    "Import resolution failed! Use the --jar option to add the corresponding JAR file with imports.", e);
        }
    }

    private void checkArguments(final Launcher spoon) {
        log.info("Validating '--entry-point' and '--method' arguments.");

        // validate args.entryPoint
        JCProfilerUtil.getEntryPoint(spoon, args.entryPoint);

        // validate and select args.method
        final CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, args.method);
        args.method = method.getDeclaringType().getQualifiedName() + "." + method.getSignature();
    }

    public static void setupSpoon(final SpoonAPI spoon, final Args args) {
        log.debug("Setting SPOON's environment.");

        // TODO: uncommenting this might lead to SPOON crashes!
        // spoon.getEnvironment().setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(spoon.getEnvironment()));

        spoon.getEnvironment().setNoClasspath(false);
        spoon.getEnvironment().setAutoImports(true);
        spoon.getEnvironment().setCopyResources(false);

        // construct the list of JavaCard API JAR files
        final List<String> apiJars = args.jcSDK.getApiJars().stream()
                .map(File::getAbsolutePath).collect(Collectors.toList());
        apiJars.addAll(args.jars.stream().map(j -> {
            log.debug("Adding {} to SPOON's path.", j);
            return j.toString();
        }).collect(Collectors.toSet()));

        spoon.getEnvironment().setSourceClasspath(apiJars.toArray(new String[0]));
    }

    private void addMissingClasses(final Launcher spoon) {
        log.info("Generating additional classes.");

        // this atrocity seems to be required as SPOON does not allow a module rebuild :(
        final Launcher tmpSpoon = new Launcher();
        buildModel(tmpSpoon);

        final List<CtClass<?>> classes = tmpSpoon.getModel().getElements(CtClass.class::isInstance);
        final Set<CtPackage> pkgs = classes.stream().map(CtClass::getPackage).collect(Collectors.toSet());
        log.debug("Found following packages in sources: {}", pkgs);

        if (pkgs.stream().anyMatch(CtPackage::isUnnamedPackage))
            throw new RuntimeException("Usage of the default package detected! " +
                    "This is unsupported by the CAP converter.");

        // Only JavaCard 3.0.1 and newer support multi package CAP files.
        // https://docs.oracle.com/en/java/javacard/3.1/guide/programming-multi-package-large-cap-files.html
        if (pkgs.size() != 1) {
            JavaCardSDK.Version jcVersion = args.jcSDK.getVersion();
            if (!jcVersion.isOneOf(JavaCardSDK.Version.V310)) {
                throw new RuntimeException(String.format(
                        "Only one package is allowed with JavaCard %s! Found: %s", jcVersion, pkgs));
            }

            // TODO: add support for such projects
            throw new RuntimeException("JavaCard 3.1+ multi package projects are unsupported at the moment!");
        }

        final String packageName = pkgs.iterator().next().getQualifiedName();
        for (final String className : generatedClasses) {
            log.debug("Looking for existing {} class.", className);
            final long count = classes.stream().filter(c -> c.isTopLevel() && c.getSimpleName().equals(className)).count();
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
                final String filename = className + ".java";
                // getClass().getResource() does not work when executed from JAR
                final InputStream is = Objects.requireNonNull(getClass().getResourceAsStream(filename));
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    final String src = br.lines()
                            // fix newlines
                            .collect(Collectors.joining(System.lineSeparator()))
                            // set package name
                            .replace("@PACKAGE@", packageName);
                    spoon.addInputResource(new VirtualFile(src, filename));
                }
                log.debug("Successfully generated new {} class.", className);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void checkPMC(final SpoonAPI spoon) {
        // check that all trap constants have unique values
        final CtClass<?> pmc = spoon.getModel().filterChildren(
                (CtClass<?> c) -> c.isTopLevel() && c.getSimpleName().equals("PMC")).first();
        final Map<CtField<?>, Short> fieldValuesMap = pmc.getFields().stream().map(f -> {
            final CtLiteral<? extends Number> lit = f.getAssignment().partiallyEvaluate();
            return new AbstractMap.SimpleEntry<>(f, lit.getValue().shortValue());
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (fieldValuesMap.size() != fieldValuesMap.values().stream().distinct().count()) {
            final Map<Short, List<CtField<?>>> valueFieldsMap = new HashMap<>();
            fieldValuesMap.forEach((k, v) -> valueFieldsMap.computeIfAbsent(v, e -> new ArrayList<>()).add(k));

            throw new RuntimeException(String.format(
                    "PMC class sanity check failed!%nThe following trap fields have the same values: %s",
                    valueFieldsMap));
        }

        // check that every trap constant is used exactly once
        final List<CtInvocation<?>> traps = spoon.getModel().getElements(
                (CtInvocation<?> i) -> {
                    if (!(i.getTarget() instanceof CtTypeAccess))
                        return false;
                    final CtType<?> cls = ((CtTypeAccess<?>) i.getTarget()).getAccessedType().getDeclaration();
                    return cls != null && cls.isTopLevel() && cls.getSimpleName().equals("PM") &&
                           i.getType().equals(i.getFactory().createCtTypeReference(Void.TYPE)) &&
                           i.getExecutable().getSignature().equals("check(short)");
        });
        final Set<CtField<?>> trapFields = traps.stream().flatMap(x -> x.getArguments().stream())
                .map(x -> (CtFieldRead<?>) x).map(CtFieldRead::getVariable).map(CtFieldReference::getDeclaration)
                .collect(Collectors.toSet());

        if (traps.size() != trapFields.size())
            throw new RuntimeException("More traps called with the same PMC field!");

        if (trapFields.size() != fieldValuesMap.size() - /* PERF_START */ 1)
            throw new RuntimeException("Some PMC trap fields are unused!");
    }
}
