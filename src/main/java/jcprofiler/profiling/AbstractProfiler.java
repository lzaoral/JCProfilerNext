package jcprofiler.profiling;

import com.github.curiousoddman.rgxgen.RgxGen;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;

import jcprofiler.args.Args;
import jcprofiler.util.enums.InputDivision;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.util.enums.Mode;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.CtModel;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.*;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Lukáš Zaoral and Petr Svenda
 */
public abstract class AbstractProfiler {
    protected static final short PERF_START = 0x0001;

    protected final Args args;
    protected final CardManager cardManager;
    protected final CtExecutable<?> profiledExecutable;
    protected final String profiledExecutableSignature;

    protected final CtType<?> PM;
    protected final CtType<?> PMC;

    // use LinkedHashX to preserve insertion order
    protected final Map<Short, String> trapNameMap = new LinkedHashMap<>();
    protected final Set<String> unreachedTraps = new LinkedHashSet<>();
    protected final List<String> inputs = new ArrayList<>();

    private String elapsedTime;

    private static final Logger log = LoggerFactory.getLogger(AbstractProfiler.class);

    protected AbstractProfiler(final Args args, final CardManager cardManager, final CtExecutable<?> executable,
                               final String handlerInsField) {
        final CtModel model = executable.getFactory().getModel();
        PM = JCProfilerUtil.getToplevelType(model, "PM");
        PMC = JCProfilerUtil.getToplevelType(model, "PMC");

        this.args = args;
        this.cardManager = cardManager;

        if (!JCProfilerUtil.entryPointHasField(executable.getFactory().getModel(), args.entryPoint, handlerInsField))
            throw new RuntimeException(String.format(
                    "Profiling in %s mode but entry point class does not contain %s field!",
                    args.mode, handlerInsField));

        profiledExecutable = executable;
        profiledExecutableSignature = JCProfilerUtil.getFullSignature(executable);

        buildPerfMapping();
    }

    public static AbstractProfiler create(final Args args, final CardManager cardManager, final CtModel model) {
        switch (args.mode) {
            case custom:
                return new CustomProfiler(args, cardManager, model);
            case memory:
                return new MemoryProfiler(args, cardManager, model);
            case time:
                return new TimeProfiler(args, cardManager, model);
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
    }

    protected String getTrapName(final short trapID) {
        return trapID == PERF_START ? "PERF_START" : trapNameMap.get(trapID);
    }

    private void buildPerfMapping() {
        log.info("Looking for traps in the {}.", profiledExecutableSignature);
        final String trapNamePrefix = JCProfilerUtil.getTrapNamePrefix(profiledExecutable);

        final List<CtField<Short>> traps = profiledExecutable.filterChildren(CtFieldAccess.class::isInstance)
                .map((CtFieldAccess<Short> fa) -> fa.getVariable().getFieldDeclaration())
                .filterChildren((CtField<Short> f) -> f.getSimpleName().startsWith(trapNamePrefix)).list();
        if (traps.isEmpty())
            throw new RuntimeException(String.format(
                    "Extraction of traps from %s failed!", profiledExecutableSignature));

        final List<CtField<Short>> pmTraps = PMC.getElements((CtField<Short> f) -> f.getSimpleName().startsWith(trapNamePrefix));
        if (pmTraps.isEmpty())
            throw new RuntimeException("Extraction of traps from PMC failed!");

        if (traps.size() != pmTraps.size() || !new HashSet<>(traps).containsAll(pmTraps))
            throw new RuntimeException(String.format(
                    "The profiled method and the PMC class contain different traps!%n" +
                    "Please, reinstrument the given sources!"));

        for (final CtField<Short> f : traps) {
            final CtLiteral<Integer> evaluated = f.getDefaultExpression().partiallyEvaluate();
            trapNameMap.put(evaluated.getValue().shortValue(), f.getSimpleName());
            log.info("Found {}.", f.getSimpleName());
        }
    }

    protected void generateInputs(int size) {
        if (profiledExecutable instanceof CtConstructor)
            throw new RuntimeException("Constructors do not support inputs!");

        // TODO: print seed for reproducibility
        final Random rdn = new Random();
        final List<String> undividedInputs = new ArrayList<>();

        // regex
        if (args.dataRegex != null) {
            log.info("Generating inputs from regular expression {}.", args.dataRegex);
            final RgxGen rgxGen = new RgxGen(args.dataRegex);

            for (int i = 0; i < size * 100; i++) {
                final String input = rgxGen.generate(rdn);
                if (!JCProfilerUtil.isHexString(input))
                    throw new RuntimeException(String.format(
                            "Input %s generated from the %s regular expression not a valid hexstring!",
                            input, args.dataRegex));
                undividedInputs.add(input);
            }
        } else {
            // file
            log.info("Choosing inputs from text file {}.", args.dataFile);

            try {
                final List<String> lines = Files.readAllLines(args.dataFile);
                for (int i = 1; i <= lines.size(); i++) {
                    final String line = lines.get(i - 1);
                    if (!JCProfilerUtil.isHexString(line))
                        throw new RuntimeException(String.format(
                                "Input %s on line %d in file %s is not a valid hexstring!", line, i, args.dataFile));
                }

                for (int i = 0; i < size * 100; i++)
                    undividedInputs.add(lines.get(rdn.nextInt(lines.size())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        int oddSize = size & 0x1;
        switch (args.inputDivision) {
            case effectiveBitLength:
                // strings with more leading zero bits will be sorted first
                undividedInputs.sort(String::compareTo);
                inputs.addAll(undividedInputs.subList(0, size / 2));
                inputs.addAll(undividedInputs.subList(undividedInputs.size() - size / 2 - oddSize, undividedInputs.size()));
                break;

            case hammingWeight:
                final List<String> hwsorted = undividedInputs.stream()
                        .map(s -> new Pair<>(JCProfilerUtil.getHexStringBitCount(s), s))
                        .sorted(Comparator.comparing(Pair::getKey))
                        .map(Pair::getValue).collect(Collectors.toList());
                inputs.addAll(hwsorted.subList(0, size / 2));
                inputs.addAll(hwsorted.subList(hwsorted.size() - size / 2 - oddSize, hwsorted.size()));
                break;

            case none:
                inputs.addAll(undividedInputs.subList(0, size));
                break;
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }

        if (args.inputDivision != InputDivision.none)
            log.info("Inputs divided according to the {}.", args.inputDivision.prettyPrint());
    }

    protected CommandAPDU getInputAPDU(int round) {
        if (round < 1 || inputs.size() < round)
            throw new RuntimeException("Unexpected index: " + round);

        final byte[] arr = Util.hexStringToByteArray(inputs.get(round - 1));
        return new CommandAPDU(args.cla, args.inst, args.p1, args.p2, arr);
    }

    protected void resetApplet() throws CardException {
        if (args.cleanupInst == null)
            return;

        log.debug("Resetting applet before measurement.");

        CommandAPDU reset = new CommandAPDU(args.cla, args.cleanupInst, 0, 0);
        ResponseAPDU response = cardManager.transmit(reset);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException("Resetting the applet failed with SW " + Integer.toHexString(response.getSW()));
    }

    public void profile() {
        try {
            final long startTime = System.nanoTime();

            log.info("Executing profiler in {} mode.", args.mode);
            log.info("Profiling {}.", profiledExecutableSignature);

            if (profiledExecutable instanceof CtConstructor) {
                inputs.add("measured during installation");
                log.info("{} was already profiled during installation.", profiledExecutableSignature);
            }

            profileImpl();

            // measure the time spent profiling
            final long endTimeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            elapsedTime = DurationFormatUtils.formatDuration(endTimeMillis, "d' days 'HH:mm:ss.SSS");
            log.info("Elapsed time: {}", elapsedTime);

            // TODO: what difference is between true and false? javax.smartcardio.Card javadoc is now very helpful
            cardManager.disconnect(true);
            log.info("Disconnected from card.");

            if (!unreachedTraps.isEmpty()) {
                log.warn("Some traps were not always reached:");
                unreachedTraps.forEach(System.out::println);
            }
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void profileImpl() throws CardException;

    public void generateCSV() {
        final String atr = args.useSimulator ? "jCardSim"
                                             : Util.bytesToHex(cardManager.getChannel().getCard().getATR().getBytes());

        String apduHeader, dataSource;
        if (profiledExecutable instanceof CtConstructor) {
            apduHeader = elapsedTime = dataSource = "install";
        } else {
            apduHeader = Util.bytesToHex(new byte[]{args.cla, args.inst, args.p1, args.p2});
            dataSource = args.dataRegex != null ? "regex:" + args.dataRegex
                                                : "file:" + args.dataFile;
        }

        if (args.mode == Mode.custom)
            elapsedTime = "TODO";

        if (inputs.isEmpty())
            throw new RuntimeException("The list of input values is empty!");

        final Path csv = args.workDir.resolve("measurements.csv");
        try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csv.toFile()), JCProfilerUtil.getCSVFormat())) {
            printer.printComment("mode,type#signature,ATR,elapsedTime,APDUHeader,inputType:value,inputFilter");
            printer.printRecord(args.mode, profiledExecutableSignature, atr, elapsedTime, apduHeader, dataSource,
                    args.inputDivision);

            printer.printComment("input1,input2,input3,...");
            printer.printRecord(inputs);

            saveMeasurements(printer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Measurements saved to {}.", csv);
    }

    protected abstract void saveMeasurements(final CSVPrinter printer) throws IOException;
}
