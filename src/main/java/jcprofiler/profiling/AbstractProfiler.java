package jcprofiler.profiling;

import com.github.curiousoddman.rgxgen.RgxGen;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;

import jcprofiler.util.JCProfilerUtil;
import jcprofiler.args.Args;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.time.DurationFormatUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Lukáš Zaoral and Petr Svenda
 */
public abstract class AbstractProfiler {
    protected static final short PERF_START = 0x0001;
    protected final Args args;
    protected final CardManager cardManager;
    protected final CtExecutable<?> profiledExecutable;
    protected final String profiledExecutableSignature;

    // use LinkedHashX to preserve insertion order
    protected final Map<Short, String> trapNameMap = new LinkedHashMap<>();
    protected final Set<String> unreachedTraps = new LinkedHashSet<>();
    protected final List<String> inputs = new ArrayList<>();

    private String elapsedTime;
    private InputGenerator inputGenerator;

    private static final Logger log = LoggerFactory.getLogger(AbstractProfiler.class);

    protected AbstractProfiler(final Args args, final CardManager cardManager, final CtExecutable<?> executable) {
        this.args = args;
        this.cardManager = cardManager;

        profiledExecutable = executable;
        profiledExecutableSignature = JCProfilerUtil.getFullSignature(executable);

        buildPerfMapping();
    }

    public static AbstractProfiler create(final Args args, final CardManager cardManager, final SpoonAPI spoon) {
        switch (args.mode) {
            case memory:
                return new MemoryProfiler(args, cardManager, spoon);
            case time:
                return new TimeProfiler(args, cardManager, spoon);
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

        for (final CtField<Short> f : traps) {
            final CtLiteral<Integer> evaluated = f.getDefaultExpression().partiallyEvaluate();
            trapNameMap.put(evaluated.getValue().shortValue(), f.getSimpleName());
            log.info("Found {}.", f.getSimpleName());
        }
    }

    private void initializeInputGenerator() {
        try {
            // TODO: more formats? hamming weight?
            // TODO: print seed for reproducibility
            final Random rdn = new Random();

            // TODO: These lambdas could be replaced with regular classes if we are going to add more of them.
            if (args.dataRegex != null) {
                log.info("Generating inputs from regular expression {}.", args.dataRegex);
                final RgxGen rgxGen = new RgxGen(args.dataRegex);
                inputGenerator = () -> rgxGen.generate(rdn);
            } else {
                log.info("Choosing inputs from text file {}.", args.dataFile);

                final List<String> inputs = Files.readAllLines(args.dataFile);
                inputGenerator = () -> inputs.get(rdn.nextInt(inputs.size()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CommandAPDU getRandomAPDU() {
        if (inputGenerator == null)
            initializeInputGenerator();

        final byte[] arr = Util.hexStringToByteArray(inputGenerator.getInput());
        final CommandAPDU apdu = new CommandAPDU(args.cla, args.inst, args.p1, args.p2, arr);
        inputs.add(Util.bytesToHex(apdu.getBytes()));

        return apdu;
    }

    protected void resetApplet() throws CardException {
        if (args.cleanupInst == null)
            return;

        log.debug("Resetting applet before measurement.");

        CommandAPDU reset = new CommandAPDU(args.cla, args.cleanupInst, 0, 0);
        ResponseAPDU response = cardManager.transmit(reset);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException("Resetting the applet failed with SW " + response.getSW());
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
            log.info("Collecting measurements complete.");

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
                return;
            }

            log.info("All traps reached correctly.");
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void profileImpl() throws CardException;

    public void generateCSV() {
        final String atr = Util.bytesToHex(cardManager.getChannel().getCard().getATR().getBytes());

        String apduHeader, dataSource;
        if (profiledExecutable instanceof CtConstructor) {
            apduHeader = dataSource = "install";
        } else {
            apduHeader = Util.bytesToHex(new byte[]{args.cla, args.inst, args.p1, args.p2});
            dataSource = args.dataRegex != null ? "regex:" + args.dataRegex
                                                : "file:" + args.dataFile;
        }

        if (inputs.isEmpty())
            throw new RuntimeException("The list of input values is empty!");

        final Path csv = args.workDir.resolve("measurements.csv");
        try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csv.toFile()), JCProfilerUtil.getCsvFormat())) {
            printer.printComment("mode,class#methodSignature,ATR,elapsedTime,APDUHeader,inputType:value");
            printer.printRecord(args.mode, profiledExecutableSignature, atr, elapsedTime, apduHeader, dataSource);

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
