package jcprofiler;

import com.beust.jcommander.JCommander;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.util.enums.Mode;
import jcprofiler.util.enums.Stage;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] argv) {
        Configurator.setRootLevel(Level.INFO);
        // TODO: add proper versioning info as well
        log.info("Welcome to JCProfilerNext!");

        Args args = new Args();
        JCommander jc = JCommander.newBuilder().addObject(args).build();

        jc.setProgramName("JCProfiler");
        try {
            jc.parse(argv);
        } catch (Exception e) {
            log.error("Argument parsing failed!", e);
            System.exit(1);
        }

        if (args.help) {
            jc.usage();
            return;
        }

        log.info("Command-line arguments parsed successfully.");
        if (args.debug) {
            Configurator.setRootLevel(Level.DEBUG);
            log.info("LogLevel set to DEBUG.");
        }

        // Log basic info
        log.info("Found JavaCard SDK {} ({})", args.jcSDK.getRelease(), args.jcSDK.getRoot().getAbsolutePath());
        log.info("Working directory: {}", args.workDir);
        log.info("Start from: {}", args.startFrom);
        log.info("Stop after: {}", args.stopAfter);
        log.info("Executed in {} mode.", args.mode);

        try {
            validateArgs(args);
            JCProfiler.run(args);
            log.info("Success!");
        } catch (Exception e) {
            log.error("Caught exception!", e);
            System.exit(1);
        }
    }

    private static void validateArgs(final Args args) {
        // this is practically a noop but probably not a deliberate one
        if (args.startFrom.ordinal() > args.stopAfter.ordinal())
            throw new UnsupportedOperationException(String.format(
                    "Nothing to do! Cannot start with %s and end with %s.",
                    args.startFrom, args.stopAfter));

        // validate custom mode
        if (args.mode == Mode.custom) {
            // --custom-pm must be set
            if (args.customPM == null)
                throw new UnsupportedOperationException("Option --custom-pm must be set in custom mode!");

            // TODO: We may agree on a subset of customizable profiling strategies.
            if (args.stopAfter.ordinal() > Stage.installation.ordinal())
                throw new UnsupportedOperationException(
                        "Profiling and visualisation of applet instrumented in custom mode are unsupported!");
        }

        // validate --data-regex and --data-file
        if ((args.dataRegex == null) == (args.dataFile == null)) {
            if (args.dataRegex != null)
                throw new UnsupportedOperationException(
                        "Options --data-file or --data-regex cannot be specified simultaneously.");

            // following check is applicable only for the profiling stage when we're not memory profiling a constructor
            final int profilingStage = Stage.profiling.ordinal();
            if (args.startFrom.ordinal() <= profilingStage && profilingStage <= args.stopAfter.ordinal() &&
                    ((args.mode != Mode.memory && args.mode != Mode.stats) || args.method != null))
                throw new UnsupportedOperationException(
                        "Either --data-file or --data-regex options must be specified for the profiling stage!");
        }

        // fail if --inst equals to JCProfilerUtil.INS_PERF_HANDLER
        if (args.inst == JCProfilerUtil.INS_PERF_HANDLER)
            throw new UnsupportedOperationException(String.format(
                    "Applet instruction byte has the same value as profiler's custom internal instruction: %d%n" +
                    "This is temporarily unsupported!", Short.toUnsignedInt(JCProfilerUtil.INS_PERF_HANDLER)));
    }
}
