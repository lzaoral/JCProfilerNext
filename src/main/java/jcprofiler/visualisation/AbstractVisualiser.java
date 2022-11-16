package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.util.enums.InputDivision;
import jcprofiler.util.enums.Mode;
import jcprofiler.util.enums.Stage;
import jcprofiler.visualisation.processors.AbstractInsertMeasurementsProcessor;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtExecutable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractVisualiser {
    protected final Args args;
    protected final CtModel model;

    // CSV header
    protected Mode mode;
    protected String atr;
    protected String profiledExecutableSignature;
    protected String elapsedTime;
    protected String apduHeader;
    protected String[] inputDescription;
    protected InputDivision inputDivision;

    protected List<String> inputs;
    protected final List<List<Double>> heatmapValues = new ArrayList<>();
    protected final Map<String, List<Long>> measurements = new LinkedHashMap<>();

    protected List<String> sourceCode;

    private static final Logger log = LoggerFactory.getLogger(AbstractVisualiser.class);

    protected AbstractVisualiser(final Args args, final CtModel model) {
        this.args = args;
        this.model = model;
    }

    public static AbstractVisualiser create(final Args args, final CtModel model) {
        switch (args.mode) {
            case memory:
                return new MemoryVisualiser(args, model);
            case time:
                return new TimeVisualiser(args, model);
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
    }

    public void loadAndProcessMeasurements() {
        loadCSV();
        loadSourceCode();
    }

    private void loadCSV() {
        final Path csv = JCProfilerUtil.checkFile(args.workDir.resolve("measurements.csv"), Stage.profiling);
        log.info("Loading measurements from {}.", csv);

        try (final CSVParser parser = CSVParser.parse(csv, Charset.defaultCharset(), JCProfilerUtil.getCSVFormat())) {
            final Iterator<CSVRecord> it = parser.iterator();

            // parse header
            final List<String> header = it.next().toList();
            mode = Mode.valueOf(header.get(0));
            if (args.mode != mode)
                throw new UnsupportedOperationException(String.format(
                        "Visualisation executed in %s mode but CSV was generated in %s mode.", args.mode, mode));

            profiledExecutableSignature = header.get(1);
            atr = header.get(2);
            elapsedTime = header.get(3);
            apduHeader = header.get(4);
            inputDescription = header.get(5).split(":", 2);
            inputDivision = InputDivision.valueOf(header.get(6));

            // parse inputs
            inputs = it.next().toList();

            // parse measurements
            do {
                final List<String> line = it.next().toList();
                final List<Long> values = line.stream().skip(1).map(this::convertValues).collect(Collectors.toList());
                measurements.put(line.get(0), values);
            } while (it.hasNext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSourceCode() {
        // get source code, escape it for HTML and strip empty lines
        final CtExecutable<?> executable = JCProfilerUtil.getProfiledExecutable(model, profiledExecutableSignature);
        sourceCode = Arrays.stream(StringEscapeUtils.escapeHtml4(executable.prettyprint())
                .split(System.lineSeparator())).filter(x -> !x.isEmpty()).collect(Collectors.toList());
    }

    protected Long convertValues(final String value) {
        if (value.isEmpty())
            return null;

        return Long.parseLong(value);
    }

    protected abstract AbstractInsertMeasurementsProcessor getInsertMeasurementsProcessor();

    public void insertMeasurementsToSources() {
        // always recreate the output directory
        final Path outputDir = JCProfilerUtil.getPerfOutputDirectory(args.workDir);
        JCProfilerUtil.recreateDirectory(outputDir);

        log.info("Inserting measurements into sources.");
        final SpoonAPI spoon = JCProfilerUtil.getInstrumentedSpoon(args);
        spoon.setSourceOutputDirectory(outputDir.toFile());
        spoon.addProcessor(getInsertMeasurementsProcessor());
        spoon.process();
        spoon.prettyprint();
    }

    protected abstract void prepareVelocityContext(final VelocityContext context);

    public void generateHTML() {
        log.info("Initializing Apache Velocity.");
        final Properties props = new Properties();
        props.put(RuntimeConstants.EVENTHANDLER_INCLUDE, IncludeRelativePath.class.getName());
        props.put(RuntimeConstants.RUNTIME_REFERENCES_STRICT, true);
        props.put(RuntimeConstants.RESOURCE_LOADERS, RuntimeConstants.RESOURCE_LOADER_CLASS);
        props.put(RuntimeConstants.RESOURCE_LOADER + '.' + RuntimeConstants.RESOURCE_LOADER_CLASS + ".class",
                ClasspathResourceLoader.class.getName());

        final VelocityEngine velocityEngine = new VelocityEngine(props);
        velocityEngine.init();

        // the heatmap is in reverse
        Collections.reverse(heatmapValues);

        final VelocityContext context = new VelocityContext();
        context.put("apduHeader", apduHeader);
        context.put("cardATR", atr);
        context.put("code", sourceCode);
        context.put("elapsedTime", elapsedTime);
        context.put("executableName", profiledExecutableSignature);
        context.put("heatmapValues", heatmapValues);
        context.put("inputDescription", inputDescription);
        context.put("inputDivision", inputDivision.prettyPrint());
        context.put("inputs", inputs.stream().map(s -> "'" + s + "'").collect(Collectors.toList()));
        context.put("measurements", measurements);
        context.put("mode", args.mode);

        // add mode specific stuff
        prepareVelocityContext(context);

        final Path output = args.workDir.resolve("measurements.html");
        log.info("Generating {}.", output);

        try (final Writer writer = new FileWriter(output.toFile())) {
            final Template template = velocityEngine.getTemplate("jcprofiler/visualisation/template.html.vm");
            template.merge(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
