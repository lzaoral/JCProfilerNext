package jcprofiler;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;
import jcprofiler.args.Args;
import jcprofiler.compilation.Compiler;
import jcprofiler.installation.Installer;
import jcprofiler.instrumentation.Instrumenter;
import jcprofiler.profiling.Profiler;
import jcprofiler.util.Stage;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.visualisation.Visualiser;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;

import java.util.List;
import java.util.Map;

public class JCProfiler {
    // static class!
    private JCProfiler() {}

    public static void run(final Args args) {
        // TODO: support already instrumented stuff
        new Instrumenter(args).process();
        if (args.stopAfter == Stage.instrumentation)
            return;

        // check that the generated sources are compilable by rebuilding the model after instrumentation
        final SpoonAPI spoon = new Launcher();
        Instrumenter.setupSpoon(spoon, args);
        spoon.addInputResource(args.outputDir);
        spoon.buildModel();

        // get entry point class
        final CtClass<?> entryPoint = JCProfilerUtil.getEntryPoint(spoon, args.entryPoint);

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

        final String atr = Util.bytesToHex(cardMgr.getChannel().getCard().getATR().getBytes());
        final Visualiser vis = new Visualiser(args, atr, spoon, measurements);
        vis.generateHTML();
        vis.generateCSV();
        vis.insertMeasurementsToSources();
    }
}
