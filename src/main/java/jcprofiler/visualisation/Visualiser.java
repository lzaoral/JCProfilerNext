package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.util.Stage;
import jcprofiler.visualisation.processors.InsertMeasurementsProcessor;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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
    private String profiledMethodSignature;
    private List<String> inputs;
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();
    private final Map<String, List<Long>> filteredMeasurements = new LinkedHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(Visualiser.class);

    public Visualiser(final Args args, final SpoonAPI spoon) {
        this.args = args;
        this.spoon = spoon;

        loadCSV();
    }

    private void loadCSV() {
        final Path csv = JCProfilerUtil.checkFile(args.workDir.resolve("measurements.csv"), Stage.profiling);
        log.info("Loading measurements in {} from {}.", JCProfilerUtil.getTimeUnitSymbol(args.timeUnit), csv);

        try (Scanner scan = new Scanner(csv)) {
            // parse header
            final String[] header = scan.nextLine().split(",(?=[^,]+$)");
            profiledMethodSignature = header[0];
            atr = header[1];

            // parse inputs
            inputs = Arrays.asList(scan.nextLine().split(","));

            // parse measurements
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

        measurements.forEach((k, v) -> {
            DescriptiveStatistics ds = new DescriptiveStatistics();
            v.stream().filter(Objects::nonNull).forEach(l -> ds.addValue(l.doubleValue()));

            if (ds.getN() == 0) {
                filteredMeasurements.put(k, new ArrayList<>());
                return;
            }

            final List<Long> filtered = v.stream().map(l -> {
                if (l == null || ds.getN() == 1)
                    return l;

                // replace outliers with null
                return 3. >= Math.abs(l - ds.getMean()) / ds.getStandardDeviation() ? l : null;
            }).collect(Collectors.toList());

            filteredMeasurements.put(k, filtered);
        });
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
        // always recreate the output directory
        final Path outputDir = JCProfilerUtil.getPerfOutputDirectory(args.workDir);
        JCProfilerUtil.recreateDirectory(outputDir);

        log.info("Inserting measurements into sources.");
        spoon.setSourceOutputDirectory(outputDir.toFile());
        spoon.addProcessor(new InsertMeasurementsProcessor(args, atr, measurements));
        spoon.process();
        spoon.prettyprint();
    }

    // TODO: must be executed before insertMeasurementsToSources()
    public void generateHTML() {
        final CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, profiledMethodSignature);

        log.info("Initializing Apache Velocity.");
        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("runtime.strict_mode.enable", true);
        velocityEngine.setProperty("resource.loaders", "class");
        velocityEngine.setProperty(
                "resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        final VelocityContext context = new VelocityContext();
        context.put("cardATR", atr);
        context.put("code", StringEscapeUtils.escapeHtml4(method.prettyprint()).split(System.lineSeparator()));
        context.put("inputs", inputs.stream().map(s -> "'" + s + "'").collect(Collectors.toList()));
        context.put("methodName", profiledMethodSignature);
        context.put("measurements", measurements);
        context.put("filteredMeasurements", filteredMeasurements);
        context.put("timeUnit", JCProfilerUtil.getTimeUnitSymbol(args.timeUnit));

        final Path output = args.workDir.resolve("measurements.html");
        log.info("Generating {}.", output);

        try (final Writer writer = new FileWriter(output.toFile())) {
            final Template template = velocityEngine.getTemplate("jcprofiler/visualisation/template.vm");
            template.merge(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
