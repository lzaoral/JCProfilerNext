package jcprofiler.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import jcprofiler.args.converters.*;
import jcprofiler.args.validators.RegexValidator;
import jcprofiler.util.Stage;
import pro.javacard.JavaCardSDK;

import java.nio.file.Path;

public class Args {
    @Parameter(names = {"-h", "--help"},
               description = "Show help",
               help = true)
    public boolean help = false;

    @Parameter(names = {"-w", "--work-dir"},
               description = "Input files or directories (can be specified multiple times)",
               required = true,
               converter = DirectoryPathConverter.class)
    public Path workDir;

    @Parameter(names = {"--start-from"},
               description = "Start from executing the given stage",
               converter = StageConverter.class)
    public Stage startFrom = Stage.instrumentation;

    @Parameter(names = {"--stop-after"},
               description = "Stop after executing the given stage",
               converter = StageConverter.class)
    public Stage stopAfter = Stage.visualisation;

    @Parameter(names = {"--max-traps-per-method"},
               description = "Maximum number of traps to be inserted in a method",
               validateWith = PositiveInteger.class)
    public int max_traps = 100;

//    @Parameter(names = {"-u", "--update"},
//               description = "Update already instrumented files")
//    public boolean update = false;

    @Parameter(names = {"--jckit"},
               required = true,
               description = "Path to the root directory with JavaCard development kit",
               converter = JCKitConverter.class)
    public JavaCardSDK jcSDK;

    @Parameter(names = {"--simulator"},
               description = "Use jCardSim simulator instead of a real card")
    public boolean use_simulator = false;

    @Parameter(names = {"--method"},
               description = "Method to profile")
    public String method = "process";

    @Parameter(names = {"--entry-point"},
               description = "Qualified name of a class to be used as an entry point (useful when there are more)")
    public String entryPoint = "";

    @Parameter(names = {"--repeat-count"},
               description = "Number of profiling rounds")
    public int repeat_count = 1000;

    @Parameter(names = {"--reset-inst"},
               description = "Applet reset instruction in hex",
               converter = ByteConverter.class)
    public Byte cleanupInst;

    // TODO: check that this (and related options) are correctly passed to the applet due
    // to possible implicit conversions that may change the value
    @Parameter(names = {"--cla"},
               description = "Applet CLA in hex",
               converter = ByteConverter.class)
    public byte cla = 0;

    @Parameter(names = {"--inst"},
               description = "Applet instruction in hex",
               converter = ByteConverter.class)
    public byte inst = 0;

    @Parameter(names = {"--p1"},
               description = "Applet P1 in hex",
               converter = ByteConverter.class)
    public byte p1 = 0;

    @Parameter(names = {"--p2"},
               description = "Applet P2 in hex",
               converter = ByteConverter.class)
    public byte p2 = 0;

    @Parameter(names = {"--data-regex"},
               description = "Regex specifying input data format in hex",
               validateWith = RegexValidator.class)
    public String dataRegex;

    @Parameter(names = {"--data-file"},
               description = "Path to a file specifying data inputs in hex",
               converter = FilePathConverter.class)
    public Path dataFile;
}
