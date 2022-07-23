package jcprofiler;

import cz.muni.fi.crocs.rcard.client.CardManager;
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

public class JCProfiler {
    // static class!
    private JCProfiler() {}

    public static void run(final Args args) {
        // TODO: support already instrumented stuff
        new Instrumenter(args).process();

        // check that the generated sources are compilable by rebuilding the model after instrumentation
        final SpoonAPI spoon = new Launcher();
        Instrumenter.setupSpoon(spoon, args);
        spoon.addInputResource(JCProfilerUtil.getInstrOutputDirectory(args.workDir).toString());
        spoon.buildModel();

        if (args.stopAfter == Stage.instrumentation)
            return;

        // get entry point class
        final CtClass<?> entryPoint = JCProfilerUtil.getEntryPoint(spoon, args.entryPoint);

        Compiler.compile(args, entryPoint);
        if (args.stopAfter == Stage.compilation)
            return;

        CardManager cardMgr = null;
        if (!args.use_simulator)
            cardMgr = Installer.installOnCard(args, entryPoint);
        if (args.stopAfter == Stage.installation)
            return;

        if (cardMgr == null)
            cardMgr = Installer.connect(args, entryPoint);

        new Profiler(args, cardMgr, spoon).profile();
        if (args.stopAfter == Stage.profiling)
            return;

        final Visualiser vis = new Visualiser(args, spoon);
        vis.generateHTML();
        vis.insertMeasurementsToSources();
    }
}
