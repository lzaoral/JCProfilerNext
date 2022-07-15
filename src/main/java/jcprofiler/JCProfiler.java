package jcprofiler;

import cz.muni.fi.crocs.rcard.client.CardManager;
import jcprofiler.args.Args;
import jcprofiler.compilation.Compiler;
import jcprofiler.installation.Installer;
import jcprofiler.instrumentation.Instrumenter;
import jcprofiler.profiling.Profiler;
import jcprofiler.util.Stage;
import jcprofiler.util.ProfilerUtil;
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
        if (args.stopAfter == Stage.instrumentation)
            return;

        final List<CtClass<?>> entryPoints = ProfilerUtil.getEntryPoints(spoon.getModel());
        final CtClass<?> entryPoint = args.entryPoint.isEmpty()
                ? entryPoints.get(0)
                : entryPoints.stream().filter(cls -> cls.getQualifiedName().equals(args.entryPoint)).findAny().get();

        Compiler.compile(args, entryPoint);
        if (args.stopAfter == Stage.compilation)
            return;

        final CardManager cardMgr = Installer.install(args, entryPoint);
        if (args.stopAfter == Stage.installation)
            return;

        final Map<String, List<Long>> measurements = new Profiler(args, cardMgr, spoon).profile();
        // TODO: what about storing the measured results?
        if (args.stopAfter == Stage.profiling)
            return;

        final Visualiser vis = new Visualiser(args, cardMgr.getChannel().getCard().toString(), spoon, measurements);
        vis.generateHTML();
        vis.generateCSV();
        vis.insertMeasurementsToSources();
    }
}
