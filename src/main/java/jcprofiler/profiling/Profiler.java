package jcprofiler.profiling;

import com.github.curiousoddman.rgxgen.RgxGen;
import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.args.Args;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Lukáš Zaoral and Petr Svenda
 */
public class Profiler {
    private static final short PERF_START = 0x0001;
    private final Args args;
    private final CardManager cardManager;
    private final CtModel model;

    // use LinkedHashX to preserve insertion order
    private final Map<Short, String> trapNameMap = new LinkedHashMap<>();
    private final Set<String> unreachedTraps = new LinkedHashSet<>();
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();
    private final List<String> inputs = new ArrayList<>();

    public Profiler(final Args args, final CardManager cardManager, final SpoonAPI spoon) {
        this.args = args;
        this.cardManager = cardManager;
        this.model = spoon.getModel();

        buildPerfMapping();
    }

    private String getTrapName(final short trapID) {
        return trapID == PERF_START ? "PERF_START" : trapNameMap.get(trapID);
    }

    private void buildPerfMapping() {
        // TODO: what about more classes with such name?
        // idea: make PM and PMC both inherit from our special and empty abstract class
        final CtClass<?> pmc = model.filterChildren((CtClass<?> c) -> c.getSimpleName().equals("PMC")).first();

        // FIXME: this may fail, may get more methods, die to dependence on mangling
        final String regex = String.format(".*_%s_argb_.*_arge_[^_]*", args.method);
        final List<CtField<Short>> traps = pmc.getElements((CtField<Short> f) -> f.getSimpleName().matches(regex));
        if (traps.isEmpty())
            throw new RuntimeException(String.format(
                    "Extraction of traps from PM for %s failed!", args.method));

        for (final CtField<Short> f : traps) {
            final CtLiteral<Integer> evaluated = f.getDefaultExpression().partiallyEvaluate();
            trapNameMap.put(evaluated.getValue().shortValue(), f.getSimpleName());
        }
    }

    public void profile() {
        try {
            // reset if possible and erase any previous performance stop and reset if possible
            resetApplet();
            setTrap(PERF_START);

            // TODO: add input to measured data
            // TODO: more formats? hamming weight?

            // TODO: print seed for reproducibility
            final Random rdn = new Random();

            // TODO: The lambdas could be replaced with a regular classes if their count will increase.
            InputGenerator inputGen;
            if (args.dataRegex != null) {
                final RgxGen rgxGen = new RgxGen(args.dataRegex);
                inputGen = () -> rgxGen.generate(rdn);
            } else {
                final List<String> inputs = Files.readAllLines(args.dataFile);
                inputGen = () -> inputs.get(rdn.nextInt(inputs.size()));
            }

            System.out.println("\n-------------- Performance profiling start --------------\n\n");

            for (int repeat = 1; repeat <= args.repeatCount; repeat++) {
                final byte[] arr = Util.hexStringToByteArray(inputGen.getInput());
                final CommandAPDU triggerAPDU = new CommandAPDU(args.cla, args.inst, args.p1, args.p2, arr);

                final String input =  Util.bytesToHex(triggerAPDU.getBytes());
                inputs.add(input);

                System.out.printf("Profiling %s, round: %d%n", args.method, repeat);
                System.out.println("APDU: " + input);

                profileSingleStep(triggerAPDU);
            }

            System.out.println("\n-------------- Performance profiling finished --------------\n\n");
            System.out.print("Disconnecting from card...");

            // TODO: what difference is between true and false? javax.smartcardio.Card javadoc is now very helpful
            cardManager.disconnect(true);
            System.out.println(" Done.");

            if (unreachedTraps.isEmpty()) {
                System.out.println("#######################################");
                System.out.println("ALL PERFORMANCE TRAPS REACHED CORRECTLY");
                System.out.println("#######################################");
            } else {
                System.out.println("################################################");
                System.out.println("!!! SOME PERFORMANCE TRAPS NOT ALWAYS REACHED!!!");
                System.out.println("################################################");
                unreachedTraps.forEach(System.out::println);
            }

            // sanity check
            measurements.values().forEach(v -> {
                if (v.size() != args.repeatCount)
                    throw new RuntimeException(String.format("%s.size() == %d", v, args.repeatCount));
            });
            if (inputs.size() != args.repeatCount)
                throw new RuntimeException("inputs.size() == " + args.repeatCount);
        } catch (CardException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTrap(short trapID) throws CardException {
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

        CommandAPDU reset = new CommandAPDU(args.cla, args.cleanupInst, 0, 0, 0);
        ResponseAPDU response = cardManager.transmit(reset);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException("Resetting the applet failed with SW " + response.getSW());
    }

    private void profileSingleStep(CommandAPDU triggerAPDu) throws CardException {
        Duration prevTransmitDuration = Duration.ZERO;
        Duration currentTransmitDuration;

        for (short trapID : trapNameMap.keySet()) {
            // set performance trap
            setTrap(trapID);

            // execute target operation
            ResponseAPDU response = cardManager.transmit(triggerAPDu);

            // Check expected error to be equal performance trap
            if (response.getSW() != (trapID & 0xFFFF)) {
                // we have not reached expected performance trap
                unreachedTraps.add(getTrapName(trapID));
                measurements.computeIfAbsent(getTrapName(trapID), k -> new ArrayList<>()).add(null);
                continue;
            }

            // TODO: set precision with a commandline-option
            currentTransmitDuration = cardManager.getLastTransmitTimeDuration();

            measurements.computeIfAbsent(getTrapName(trapID), k -> new ArrayList<>())
                    .add(currentTransmitDuration.minus(prevTransmitDuration).toNanos());

            prevTransmitDuration = currentTransmitDuration;

            // free memory after command
            resetApplet();
        }
    }

    public void generateCSV() {
        final String atr = Util.bytesToHex(cardManager.getChannel().getCard().getATR().getBytes());
        final File csv = args.workDir.resolve("measurements.csv").toFile();

        try (PrintWriter writer = new PrintWriter(csv)) {
            writer.printf("%s,%s%n", atr, String.join(",", inputs));
            measurements.forEach((trap, values) ->
                    writer.printf("%s,%s%n", trap,
                            values.stream().map(v -> v != null ? v.toString() : "unreach")
                                    .collect(Collectors.joining(",")))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
