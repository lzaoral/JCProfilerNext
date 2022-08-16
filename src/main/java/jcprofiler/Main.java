package jcprofiler;

import com.beust.jcommander.JCommander;
import jcprofiler.args.Args;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] argv) {
        Configurator.setRootLevel(Level.INFO);
        log.info("Welcome to JCProfiler Next!");

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

        log.info("Found JavaCard SDK {} ({})", args.jcSDK.getRelease(), args.jcSDK.getRoot().getAbsolutePath());
        log.info("Working directory: {}", args.workDir.toAbsolutePath());

        try {
            JCProfiler.run(args);
            log.info("Success!");
        } catch (Exception e) {
            log.error("Caught exception!", e);
            System.exit(1);
        }
    }
}
