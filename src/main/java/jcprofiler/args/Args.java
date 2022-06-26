package jcprofiler.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import jcprofiler.args.converters.ByteConverter;
import jcprofiler.args.converters.JCKitConverter;
import jcprofiler.args.validators.InputPathValidator;
import jcprofiler.args.validators.OutputPathValidator;
import jcprofiler.args.validators.RegexValidator;
import pro.javacard.JavaCardSDK;

import java.util.ArrayList;
import java.util.List;

public class Args {
    @Parameter(names = {"-h", "--help"},
            description = "Show help",
            help = true)
    public boolean help = false;

    // TODO: add some filter for input files?
    @Parameter(names = {"-i", "--input-file", "--input-dir"},
            description = "Input files or directories (can be specified multiple times)",
            required = true,
            validateWith = InputPathValidator.class)
    public List<String> inputs;

    @Parameter(names = {"-o", "--output-dir"},
            description = "Output directory",
            required = true,
            validateWith = OutputPathValidator.class)
    public String outputDir;

    @Parameter(names = {"--instrument-only"},
            description = "Only instrument the code (default false)")
    public boolean instrument_only = false;

//    @Parameter(names = {"-p", "--profile-only"},
//               description = "Profile already installed applet")
//    public boolean profile_only = false;

    @Parameter(names = {"--max-traps-per-method"},
            description = "Maximum number of traps to be inserted in a method (default 100)",
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
            description = "Path to the root directory with JavaCard development kit")
    public boolean use_simulator = false;

    @Parameter(names = {"--method"},
            description = "Method to profile")
    public String method = "process";

    @Parameter(names = {"--entry-point"},
            description = "Main class to be used as an entry point (useful when there are more)")
    public String entryPoint = "";

    @Parameter(names = {"--repeat-count"},
            description = "Number of profiling rounds (default 1000)")
    public int repeat_count = 1000;

    @Parameter(names = {"--reset-inst"},
            description = "Applet reset instruction in hex",
            converter = ByteConverter.class)
    public Byte cleanupInst;

    // TODO: check that this (and related options) are correctly passed to the applet due
    // to possible implicit conversions that may change the value
    @Parameter(names = {"--cla"},
            description = "Applet CLA in hex (default 0x00)",
            converter = ByteConverter.class)
    public byte cla = 0;

    @Parameter(names = {"--inst"},
            description = "Applet instruction in hex (default 0x00)",
            converter = ByteConverter.class)
    public byte inst = 0;

    @Parameter(names = {"--p1"},
            description = "Applet P1 in hex (default 0x00)",
            converter = ByteConverter.class)
    public byte p1 = 0;

    @Parameter(names = {"--p2"},
            description = "Applet P2 in hex (default 0x00)",
            converter = ByteConverter.class)
    public byte p2 = 0;

    @Parameter(names = {"--input-regex"},
            description = "Regex specifying input format in hex",
            validateWith = RegexValidator.class)
    public String inputRegex;
}
