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
        // Instrumentation
        if (args.startFrom.ordinal() <= Stage.instrumentation.ordinal()) {
            JCProfilerUtil.moveToSubDirIfNotExists(args.workDir, JCProfilerUtil.getSourceInputDirectory(args.workDir));
            // TODO: support already instrumented stuff
            new Instrumenter(args).process();
        }

        // check that the generated sources are compilable by rebuilding the model after instrumentation
        final SpoonAPI spoon = new Launcher();
        Instrumenter.setupSpoon(spoon, args);
        spoon.addInputResource(JCProfilerUtil.getInstrOutputDirectory(args.workDir).toString());
        spoon.buildModel();

        if (args.stopAfter == Stage.instrumentation)
            return;

        // get entry point class
        final CtClass<?> entryPoint = JCProfilerUtil.getEntryPoint(spoon, args.entryPoint);

        // Compilation
        if (args.startFrom.ordinal() <= Stage.compilation.ordinal()) {
            Compiler.compile(args, entryPoint);
            if (args.stopAfter == Stage.compilation)
                return;
        }

        // Installation (noop for --simulator)
        CardManager cardManager = null;
        if (args.startFrom.ordinal() <= Stage.installation.ordinal() && !args.use_simulator)
            cardManager = Installer.installOnCard(args, entryPoint);
        if (args.stopAfter == Stage.installation)
            return;

        // Profiling
        if (args.startFrom.ordinal() <= Stage.profiling.ordinal()) {
            // Connect if the installation was skipped or simulator is used
            if (cardManager == null)
                // TODO: move connection stuff to a separate class?
                cardManager = Installer.connect(args, entryPoint);

            new Profiler(args, cardManager, spoon).profile();
            if (args.stopAfter == Stage.profiling)
                return;
        }

        // Visualisation
        final Visualiser vis = new Visualiser(args, spoon);
        vis.generateHTML();
        vis.insertMeasurementsToSources();
    }
}
