package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.visualisation.processors.InsertTimesProcessor;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtMethod;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Visualiser {
    private final Args args;
    private final String atr;
    private final SpoonAPI spoon;
    private final Map<String, List<Long>> measurements;

    public Visualiser(final Args args, final String atr,
                      final SpoonAPI spoon, final Map<String, List<Long>> measurements) {
        this.args = args;
        // TODO: integrate with https://smartcard-atr.apdu.fr/parse?ATR=XXX?
        this.atr = atr;
        this.spoon = spoon;
        this.measurements = measurements;
    }

    // TODO: aggregate results when there's too many of them
    public void insertMeasurementsToSources() {
        spoon.setSourceOutputDirectory(Paths.get(args.outputDir, "sources_perf").toFile());
        spoon.addProcessor(new InsertTimesProcessor(args, atr, measurements));
        spoon.process();
        spoon.prettyprint();
    }

    public void generateCSV() {
        final File csv = Paths.get(args.outputDir, "measurements.csv").toFile();
        try (final PrintWriter writer = new PrintWriter(csv)) {
            writer.printf("trap,%s (measurements in nanoseconds)%n",
                    IntStream.rangeClosed(1, args.repeat_count)
                            .mapToObj(i -> "round " + i)
                            .collect(Collectors.joining(",")));
            measurements.forEach((trap, values) ->
                    writer.printf("%s,%s%n", trap,
                            values.stream().map(v -> v != null ? v.toString() : "unreach")
                                    .collect(Collectors.joining(",")))
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: must be executed before insertMeasurementsToSources()
    public void generateHTML() {
        // TODO: this might break if more methods have the same name
        final CtMethod<?> method = spoon.getModel()
                .filterChildren((CtMethod<?> m) -> m.getSimpleName().equals(args.method)).first();

        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loaders", "class");
        velocityEngine.setProperty(
                "resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        final VelocityContext context = new VelocityContext();
        context.put("cardATR", atr);
        context.put("code", StringEscapeUtils.escapeHtml4(method.prettyprint()).split(System.lineSeparator()));
        context.put("methodName", method.getDeclaringType().getQualifiedName() + "." + method.getSignature());
        context.put("measurements", measurements);
        context.put("null", null);

        final File output = Paths.get(args.outputDir, "measurements.html").toFile();
        try (final Writer writer = new FileWriter(output)) {
            final Template template = velocityEngine.getTemplate("jcprofiler/visualisation/template.vm");
            template.merge(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
