package jcprofiler;

import cz.muni.fi.crocs.rcard.client.CardManager;
import jcprofiler.args.Args;
import jcprofiler.compilation.Compiler;
import jcprofiler.installation.Installer;
import jcprofiler.instrumentation.Instrumenter;
import jcprofiler.profiling.Profiler;
import jcprofiler.visualisation.Visualiser;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;

import java.util.List;
import java.util.Map;

public class JCProfiler {
    // static class!
    private JCProfiler() {}

    public static void run(final Args args) {
        // TODO: support already instrumented stuff
        final SpoonAPI spoon = new Instrumenter(args).process();
        if (args.instrument_only)
            return;

        final List<CtClass<?>> entryPoints = Utils.getEntryPoints(spoon.getModel());
        final CtClass<?> entryPoint = args.entryPoint.isEmpty()
                ? entryPoints.get(0)
                : entryPoints.stream().filter(cls -> cls.getQualifiedName().equals(args.entryPoint)).findAny().get();

        Compiler.compile(args, entryPoint);

        final CardManager cardMgr = Installer.install(args, entryPoint);
        final Map<String, List<Long>> measurements = new Profiler(args, cardMgr, spoon).profile();

        final Visualiser vis = new Visualiser(args, cardMgr.getChannel().getCard().toString(), spoon, measurements);
        vis.generateHTML();
        vis.generateCSV();
        vis.insertMeasurementsToSources();
    }
}
