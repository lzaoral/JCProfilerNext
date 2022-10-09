package jcprofiler;

import cz.muni.fi.crocs.rcard.client.CardManager;

import jcprofiler.args.Args;
import jcprofiler.compilation.Compiler;
import jcprofiler.installation.Installer;
import jcprofiler.instrumentation.Instrumenter;
import jcprofiler.profiling.AbstractProfiler;
import jcprofiler.util.Stage;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.visualisation.AbstractVisualiser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtClass;

import java.nio.file.Path;

public class JCProfiler {
    private static final Logger log = LoggerFactory.getLogger(JCProfiler.class);

    // static class!
    private JCProfiler() {}

    public static void run(final Args args) {
        // Instrumentation
        if (args.startFrom.ordinal() <= Stage.instrumentation.ordinal()) {
            log.info("Instrumentation started.");
            JCProfilerUtil.moveToSubDirIfNotExists(args.workDir, JCProfilerUtil.getSourceInputDirectory(args.workDir));

            new Instrumenter(args).process();
            log.info("Instrumentation complete.");
        }

        // check that the generated sources are compilable by rebuilding the model after instrumentation
        final SpoonAPI spoon = new Launcher();
        log.info("Validating SPOON model.");

        Instrumenter.setupSpoon(spoon, args);
        final Path instrOutput = JCProfilerUtil.getInstrOutputDirectory(args.workDir);
        JCProfilerUtil.checkDirectory(instrOutput, Stage.instrumentation);
        spoon.addInputResource(instrOutput.toString());
        spoon.buildModel();

        if (args.stopAfter == Stage.instrumentation)
            return;

        // get entry point class (needed from compilation to profiling)
        final CtClass<?> entryPoint = args.startFrom != Stage.visualisation
                                            ? JCProfilerUtil.getEntryPoint(spoon, args.entryPoint)
                                            : null;

        // Compilation
        if (args.startFrom.ordinal() <= Stage.compilation.ordinal()) {
            log.info("Compilation started.");
            Compiler.compile(args, entryPoint);
            log.info("Compilation complete.");
        }

        if (args.stopAfter == Stage.compilation)
            return;

        // Installation
        CardManager cardManager = null;
        if (args.startFrom.ordinal() <= Stage.installation.ordinal()) {
            // noop for --simulator
            if (args.useSimulator) {
                log.info("Skipping installation because simulator is used.");
            } else {
                log.info("Installation started.");
                cardManager = Installer.installOnCard(args, entryPoint);
                log.info("Installation complete.");
            }
        }

        if (args.stopAfter == Stage.installation)
            return;

        // Profiling
        if (args.startFrom.ordinal() <= Stage.profiling.ordinal()) {
            // Connect if the installation was skipped or simulator is used
            if (cardManager == null)
                // TODO: move connection stuff to a separate class?
                cardManager = Installer.connect(args, entryPoint);

            log.info("Profiling started.");
            final AbstractProfiler profiler = AbstractProfiler.create(args, cardManager, spoon);
            profiler.profile();
            profiler.generateCSV();
            log.info("Profiling complete.");
        }

        if (args.stopAfter == Stage.profiling)
            return;

        // Visualisation
        log.info("Visualising results.");
        final AbstractVisualiser vis = AbstractVisualiser.create(args, spoon);
        vis.loadAndProcessMeasurements();
        vis.generateHTML();
        vis.insertMeasurementsToSources();
        log.info("Visualising results complete.");
    }
}
