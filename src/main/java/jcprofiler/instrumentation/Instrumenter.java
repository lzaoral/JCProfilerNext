package jcprofiler.instrumentation;

import jcprofiler.args.Args;
import jcprofiler.instrumentation.processors.*;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Triple;

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
import java.util.function.Function;
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

        // Instrument the model

        // The insertion of traps must be done BEFORE instrumentation of the entry point class, otherwise the custom
        // instruction handler could be unreachable due to the trap inserted before its invocation which will break
        // selection of e.g. next fatal trap.
        switch (args.mode) {
            case memory:
                spoon.addProcessor(new InsertMemoryTrapProcessor(args));
                spoon.addProcessor(new ModifyMemoryEntryPointProcessor(args));
                break;
            case time:
                spoon.addProcessor(new InsertTimeTrapProcessor(args));
                spoon.addProcessor(new ModifyTimeEntryPointProcessor(args));
                break;
            case custom:
                spoon.addProcessor(new InsertCustomTrapProcessor(args));
                spoon.addProcessor(new ModifyCustomEntryPointProcessor(args));
                break;
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }

        // add workarounds for bugs in Spoon
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
        JCProfilerUtil.setupSpoon(spoon, args);
        spoon.addInputResource(JCProfilerUtil.getSourceInputDirectory(args.workDir).toString());

        log.debug("Building Spoon model.");
        try {
            spoon.buildModel();
        } catch (ModelBuildingException e) {
            if (!e.getMessage().matches(".* cannot be resolved (to a type )?at .*"))
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
        CtExecutable<?> executable;
        switch (args.mode) {
            case custom:
            case memory:
                executable = JCProfilerUtil.getProfiledExecutable(spoon, args.entryPoint, args.method);
                break;
            case time:
                executable = JCProfilerUtil.getProfiledMethod(spoon, args.method);
                break;
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
        args.method = JCProfilerUtil.getFullSignature(executable);
    }

    private void addMissingClasses(final Launcher spoon) {
        log.info("Generating additional classes.");

        // this atrocity seems to be required as Spoon does not allow a module rebuild :(
        final Launcher tmpSpoon = new Launcher();
        buildModel(tmpSpoon);

        final List<CtType<?>> types = tmpSpoon.getModel().getElements(CtType.class::isInstance);
        final Set<CtPackage> pkgs = types.stream().map(CtType::getPackage)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        log.debug("Found following packages in sources: {}", pkgs);

        if (pkgs.stream().anyMatch(CtPackage::isUnnamedPackage))
            throw new UnsupportedOperationException("Usage of the default package detected! " +
                    "This is unsupported by the CAP converter.");

        // Only JavaCard 3.0.1 and newer support multi package CAP files.
        // https://docs.oracle.com/en/java/javacard/3.1/guide/programming-multi-package-large-cap-files.html
        if (pkgs.size() != 1) {
            JavaCardSDK.Version jcVersion = args.jcSDK.getVersion();
            if (!jcVersion.isOneOf(JavaCardSDK.Version.V310)) {
                throw new UnsupportedOperationException(String.format(
                        "Only one package is allowed with JavaCard %s! Found: %s", jcVersion, pkgs));
            }

            // TODO: add support for such projects
            throw new UnsupportedOperationException(
                    "JavaCard 3.1+ multi package projects are unsupported at the moment!");
        }

        final String packageName = pkgs.iterator().next().getQualifiedName();
        for (final String className : generatedClasses) {
            log.debug("Looking for existing {} class.", className);
            final long count = types.stream()
                    .filter(t -> t.isClass() && t.isTopLevel() && t.getSimpleName().equals(className)).count();
            if (count > 0)
                throw new UnsupportedOperationException(String.format(
                        "Code contains %d classes named %s! Updating already instrumented sources is unsupported " +
                        "at the moment!", count, className));

            log.debug("Class {} not found.", className);
            try {
                String actualFilename = null;
                if (className.equals("PM")) {
                    switch (args.mode) {
                    case custom:
                        log.info("Using custom PM class from {}.", args.customPM);
                        spoon.addInputResource(args.customPM.toString());
                        continue;
                    case memory:
                        actualFilename = args.mode + "/" + className;

                        // support newer JCSystem.getAvailableMemory overloads
                        final boolean hasNewerAPI = args.jcSDK.getVersion().ordinal() >= JavaCardSDK.Version.V304.ordinal();
                        if (hasNewerAPI && args.useSimulator) {
                            log.warn("jCardSim does not implement JCSystem.getAvailableMemory(short[],short,byte).");
                            log.info("Falling back to JCSystem.getAvailableMemory(short).");
                        }

                        final boolean useNewerAPI = hasNewerAPI && !args.useSimulator;
                        log.info("Using JCSystem.getAvailableMemory with {} B limit.",
                                useNewerAPI ? Integer.MAX_VALUE : Short.MAX_VALUE);
                        actualFilename += (useNewerAPI ? "-new" : "-old") + ".java";
                        break;
                    case time:
                        actualFilename = args.mode + "/" + className + ".java";
                        break;
                    default:
                        throw new RuntimeException("Unreachable statement reached!");
                    }
                }

                final String filename = className + ".java";
                // getClass().getResource() does not work when executed from JAR
                final InputStream is = Objects.requireNonNull(getClass().getResourceAsStream(
                        actualFilename != null ?  actualFilename : filename));
                try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    final String src = br.lines()
                            // fix newlines
                            .collect(Collectors.joining(System.lineSeparator()))
                            // set package name
                            .replace("jcprofiler", packageName);
                    spoon.addInputResource(new VirtualFile(src, filename));
                }
                log.debug("Successfully generated new {} class.", filename);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void checkPMC(final SpoonAPI spoon) {
        // check that all trap constants have unique values
        final CtClass<?> pmc = spoon.getModel().filterChildren(
                (CtClass<?> c) -> c.isTopLevel() && c.getSimpleName().equals("PMC")).first();
        final Map<CtField<?>, Short> fieldValuesMap = pmc.getFields().stream().collect(Collectors.toMap(
                Function.identity(),
                f -> f.getAssignment().<CtLiteral<Integer>>partiallyEvaluate().getValue().shortValue()));
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

    public void generateStatistics() {
        boolean permissive = false;

        Launcher spoon = new Launcher();
        JCProfilerUtil.setupSpoon(spoon, args);
        spoon.addInputResource(args.workDir.toString());

        try {
            spoon.buildModel();
        } catch (ModelBuildingException e) {
            log.warn(e.getMessage());

            log.info("Retrying in permissive mode.");
            log.warn("The results may not be accurate!");
            permissive = true;

            // try it with missing imports
            spoon = new Launcher();
            JCProfilerUtil.setupSpoon(spoon, args);
            spoon.addInputResource(args.workDir.toString());
            spoon.getEnvironment().setNoClasspath(true);
            spoon.buildModel();
        }

        final StatisticsProcessor sp = new StatisticsProcessor();
        spoon.addProcessor(sp);
        spoon.process();

        final Path csv = args.workDir.resolve("APIstatistics.csv");
        try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csv.toFile()), JCProfilerUtil.getCSVFormat())) {
            if (permissive) {
                printer.printComment("CSV generated in permissive mode! Some imports or types could not be resolved.");
                printer.printComment("Please, do a rerun with appropriate JAR files using the --jar option.");
                printer.printComment("");
            }

            printer.printComment("package/outer type,type,member,frequency");
            for (Map.Entry<Triple<String, String, String>, Integer> pair : sp.getUsedReferences().entrySet()) {
                final Triple<String, String, String> key = pair.getKey();
                printer.printRecord(key.getLeft(), key.getMiddle(), key.getRight(), pair.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Statistics saved to {}.", csv);
    }
}
