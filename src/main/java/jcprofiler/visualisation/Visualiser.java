package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.visualisation.processors.InsertTimesProcessor;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtMethod;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Visualiser {
    private final Args args;
    private final SpoonAPI spoon;

    private String atr;
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();

    public Visualiser(final Args args, final SpoonAPI spoon) {
        this.args = args;
        this.spoon = spoon;

        loadCSV();
    }

    private void loadCSV() {
        final File csv = args.workDir.resolve("measurements.csv").toFile();
        try (Scanner scan = new Scanner(csv)) {
            // parse header
            atr = scan.nextLine();

            while (scan.hasNextLine()) {
                final String trap = scan.findInLine("[^,]+");
                scan.skip(",");
                final List<Long> trapMeasurements = Arrays.stream(scan.nextLine().split(","))
                        .map(s -> s.equals("unreach") ? null : TimeUnit.NANOSECONDS.toMicros(Long.parseLong(s)))
                        .collect(Collectors.toList());

                measurements.put(trap, trapMeasurements);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: aggregate results when there's too many of them
    public void insertMeasurementsToSources() {
        spoon.setSourceOutputDirectory(JCProfilerUtil.getPerfOutputDirectory(args.workDir).toFile());
        spoon.addProcessor(new InsertTimesProcessor(atr, measurements));
        spoon.process();
        spoon.prettyprint();
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

        // TODO: integrate with https://smartcard-atr.apdu.fr/parse?ATR=XXX?

        final VelocityContext context = new VelocityContext();
        context.put("cardATR", atr);
        context.put("code", StringEscapeUtils.escapeHtml4(method.prettyprint()).split(System.lineSeparator()));
        context.put("methodName", method.getDeclaringType().getQualifiedName() + "." + method.getSignature());
        context.put("measurements", measurements);
        context.put("null", null);

        final File output = args.workDir.resolve("measurements.html").toFile();
        try (final Writer writer = new FileWriter(output)) {
            final Template template = velocityEngine.getTemplate("jcprofiler/visualisation/template.vm");
            template.merge(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
