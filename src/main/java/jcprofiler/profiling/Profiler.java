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
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;

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
public class Profiler {
    private static final short PERF_START = 0x0001;
    private final Args args;
    private final CardManager cardManager;
    private final CtMethod<?> profiledMethod;
    private final SpoonAPI spoon;

    // use LinkedHashX to preserve insertion order
    private final Map<Short, String> trapNameMap = new LinkedHashMap<>();
    private final Set<String> unreachedTraps = new LinkedHashSet<>();
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();
    private final List<String> inputs = new ArrayList<>();

    private String elapsedTime;

    private static final Logger log = LoggerFactory.getLogger(Profiler.class);

    public Profiler(final Args args, final CardManager cardManager, final SpoonAPI spoon) {
        this.args = args;
        this.cardManager = cardManager;
        this.profiledMethod = JCProfilerUtil.getProfiledMethod(spoon, args.method);
        this.spoon = spoon;

        buildPerfMapping();
    }

    private String getTrapName(final short trapID) {
        return trapID == PERF_START ? "PERF_START" : trapNameMap.get(trapID);
    }

    private void buildPerfMapping() {
        log.info("Looking for traps in the {} method.", args.method);
        final String trapNamePrefix = JCProfilerUtil.getTrapNamePrefix(profiledMethod) + "_";

        // TODO: what about more classes with such name?
        // idea: make PM and PMC both inherit from our special and empty abstract class
        final CtClass<?> pmc = spoon.getModel().filterChildren((CtClass<?> c) -> c.getSimpleName().equals("PMC")).first();

        final List<CtField<Short>> traps = pmc.getElements((CtField<Short> f) -> f.getSimpleName().startsWith(trapNamePrefix));
        if (traps.isEmpty())
            throw new RuntimeException(String.format(
                    "Extraction of traps from PM for %s failed!", args.method));

        for (final CtField<Short> f : traps) {
            final CtLiteral<Integer> evaluated = f.getDefaultExpression().partiallyEvaluate();
            trapNameMap.put(evaluated.getValue().shortValue(), f.getSimpleName());
            log.info("Found {}.", f.getSimpleName());
        }
    }

    public void profile() {
        try {
            // reset if possible and erase any previous performance stop
            resetApplet();
            setTrap(PERF_START);

            // TODO: more formats? hamming weight?
            // TODO: print seed for reproducibility
            final Random rdn = new Random();

            // TODO: These lambdas could be replaced with regular classes if we are going to add more of them.
            InputGenerator inputGen;
            if (args.dataRegex != null) {
                log.info("Generating inputs from regular expression {}.", args.dataRegex);
                final RgxGen rgxGen = new RgxGen(args.dataRegex);
                inputGen = () -> rgxGen.generate(rdn);
            } else {
                log.info("Choosing inputs from text file {}.", args.dataFile);
                final List<String> inputs = Files.readAllLines(args.dataFile);
                inputGen = () -> inputs.get(rdn.nextInt(inputs.size()));
            }

            final long startTime = System.nanoTime();
            for (int repeat = 1; repeat <= args.repeatCount; repeat++) {
                final byte[] arr = Util.hexStringToByteArray(inputGen.getInput());
                final CommandAPDU triggerAPDU = new CommandAPDU(args.cla, args.inst, args.p1, args.p2, arr);

                final String input =  Util.bytesToHex(triggerAPDU.getBytes());
                inputs.add(input);

                log.info("Round: {}/{} APDU: {}", repeat, args.repeatCount, input);
                profileSingleStep(triggerAPDU);
            }
            log.info("Collecting measurements complete.");

            // measure the time spent profiling
            final long endTimeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            elapsedTime = DurationFormatUtils.formatDuration(endTimeMillis, "d' days 'HH:mm:ss.SSS");
            log.info("Elapsed time: {}", elapsedTime);

            // TODO: what difference is between true and false? javax.smartcardio.Card javadoc is now very helpful
            cardManager.disconnect(true);
            log.info("Disconnected from card.");

            // sanity check
            log.debug("Checking that no measurements are missing.");
            measurements.forEach((k, v) -> {
                if (v.size() != args.repeatCount)
                    throw new RuntimeException(k + ".size() != " + args.repeatCount);
            });
            if (inputs.size() != args.repeatCount)
                throw new RuntimeException("inputs.size() != " + args.repeatCount);

            if (!unreachedTraps.isEmpty()) {
                log.warn("Some traps were not always reached:");
                unreachedTraps.forEach(System.out::println);
                return;
            }

            log.info("All traps reached correctly.");
        } catch (CardException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTrap(short trapID) throws CardException {
        log.debug("Setting next trap to {}.", getTrapName(trapID));

        CommandAPDU setTrap = new CommandAPDU(args.cla, JCProfilerUtil.INS_PERF_SETSTOP, 0, 0,
                                              Util.shortToByteArray(trapID));
        ResponseAPDU response = cardManager.transmit(setTrap);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException(String.format(
                    "Setting \"%s\" trap failed with SW %d", getTrapName(trapID), response.getSW()));
    }

    private void resetApplet() throws CardException {
        if (args.cleanupInst == null)
            return;

        log.debug("Resetting applet before measurement.");

        CommandAPDU reset = new CommandAPDU(args.cla, args.cleanupInst, 0, 0);
        ResponseAPDU response = cardManager.transmit(reset);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException("Resetting the applet failed with SW " + response.getSW());
    }

    private void profileSingleStep(CommandAPDU triggerAPDu) throws CardException {
        long prevTransmitDuration = 0;
        long currentTransmitDuration;

        for (short trapID : trapNameMap.keySet()) {
            // set performance trap
            setTrap(trapID);

            // execute target operation
            log.debug("Measuring {}.", getTrapName(trapID));
            ResponseAPDU response = cardManager.transmit(triggerAPDu);

            // Check expected error to be equal performance trap
            if (response.getSW() != Short.toUnsignedInt(trapID)) {
                // we have not reached expected performance trap
                unreachedTraps.add(getTrapName(trapID));
                measurements.computeIfAbsent(getTrapName(trapID), k -> new ArrayList<>()).add(null);
                log.debug("Duration: unreachable");
                continue;
            }

            // compute the difference
            currentTransmitDuration = cardManager.getLastTransmitTimeNano();
            final long diff = currentTransmitDuration - prevTransmitDuration;
            prevTransmitDuration = currentTransmitDuration;

            log.debug("Duration: {} ns", diff);

            // store the difference
            measurements.computeIfAbsent(getTrapName(trapID), k -> new ArrayList<>()).add(diff);

            // free memory after command
            resetApplet();
        }
    }

    public void generateCSV() {
        final String atr = Util.bytesToHex(cardManager.getChannel().getCard().getATR().getBytes());
        final String apduHeader = Util.bytesToHex(new byte[]{args.cla, args.inst, args.p1, args.p2});

        final Path csv = args.workDir.resolve("measurements.csv");
        try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csv.toFile()), JCProfilerUtil.getCsvFormat())) {
            printer.printComment("class.methodSignature,ATR,elapsedTime,APDUHeader,inputType:value");
            printer.print(profiledMethod.getDeclaringType().getQualifiedName() + "." + profiledMethod.getSignature());
            printer.printRecord(atr, elapsedTime, apduHeader,
                    args.dataRegex != null ? "regex:" + args.dataRegex : "file:" + args.dataFile);

            printer.printComment("input1,input2,input3,...");
            printer.printRecord(inputs);

            printer.printComment("trapName,measurement1,measurement2,...");
            for (final Map.Entry<String, List<Long>> e : measurements.entrySet()) {
                printer.print(e.getKey());
                printer.printRecord(e.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Measurements saved to {}.", csv);
    }
}
