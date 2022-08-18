package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.visualisation.processors.InsertMeasurementsProcessor;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;
import spoon.reflect.declaration.CtMethod;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Visualiser {
    private final Args args;
    private final SpoonAPI spoon;

    private String atr;
    private List<String> inputs;
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(Visualiser.class);

    public Visualiser(final Args args, final SpoonAPI spoon) {
        this.args = args;
        this.spoon = spoon;

        loadCSV();
    }

    private void loadCSV() {
        final Path csv = args.workDir.resolve("measurements.csv");
        log.info("Loading measurements in {} from {}.", JCProfilerUtil.getTimeUnitSymbol(args.timeUnit), csv);

        try (Scanner scan = new Scanner(csv)) {
            // parse header
            atr = scan.findInLine("[^,]+");
            scan.skip(",");
            inputs = Arrays.asList(scan.nextLine().split(","));

            while (scan.hasNextLine()) {
                final String trap = scan.findInLine("[^,]+");
                scan.skip(",");
                final List<Long> trapMeasurements = Arrays.stream(scan.nextLine().split(","))
                        .map(this::convertTime).collect(Collectors.toList());

                measurements.put(trap, trapMeasurements);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Long convertTime(final String input) {
        if (input.equals("unreach"))
            return null;

        long nanos = Long.parseLong(input);
        switch (args.timeUnit) {
            case nano:
                return nanos; // noop
            case micro:
                return TimeUnit.NANOSECONDS.toMicros(nanos);
            case milli:
                return TimeUnit.NANOSECONDS.toMillis(nanos);
            case sec:
                return TimeUnit.NANOSECONDS.toSeconds(nanos);
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
    }

    // TODO: aggregate results when there's too many of them
    public void insertMeasurementsToSources() {
        log.info("Inserting measurements into sources.");
        spoon.setSourceOutputDirectory(JCProfilerUtil.getPerfOutputDirectory(args.workDir).toFile());
        spoon.addProcessor(new InsertMeasurementsProcessor(atr, measurements));
        spoon.process();
        spoon.prettyprint();
    }

    // TODO: must be executed before insertMeasurementsToSources()
    public void generateHTML() {
        // TODO: this might break if more methods have the same name
        final CtMethod<?> method = spoon.getModel()
                .filterChildren((CtMethod<?> m) -> m.getSimpleName().equals(args.method)).first();

        log.info("Initializing Apache Velocity.");
        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loaders", "class");
        velocityEngine.setProperty(
                "resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        final VelocityContext context = new VelocityContext();
        context.put("cardATR", atr);
        context.put("code", StringEscapeUtils.escapeHtml4(method.prettyprint()).split(System.lineSeparator()));
        context.put("inputs", inputs.stream().map(s -> "'" + s + "'").collect(Collectors.toList()));
        context.put("methodName", method.getDeclaringType().getQualifiedName() + "." + method.getSignature());
        context.put("measurements", measurements);
        context.put("null", null);
        context.put("timeUnit", JCProfilerUtil.getTimeUnitSymbol(args.timeUnit));

        final Path output = args.workDir.resolve("measurements.html");
        log.info("Generating {}.", output.toAbsolutePath());

        try (final Writer writer = new FileWriter(output.toFile())) {
            final Template template = velocityEngine.getTemplate("jcprofiler/visualisation/template.vm");
            template.merge(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
