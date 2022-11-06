package jcprofiler.profiling;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.csv.CSVPrinter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.CtModel;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.IOException;
import java.util.*;

/**
 * @author Lukáš Zaoral and Petr Svenda
 */
public class TimeProfiler extends AbstractProfiler {
    // use LinkedHashX to preserve insertion order
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(TimeProfiler.class);

    public TimeProfiler(final Args args, final CardManager cardManager, final CtModel model) {
        super(args, cardManager, JCProfilerUtil.getProfiledMethod(model, args.method), "INS_PERF_SETSTOP");
    }

    @Override
    public void profileImpl() {
        try {
            // reset if possible and erase any previous performance stop
            resetApplet();
            setTrap(PERF_START);

            generateInputs(args.repeatCount);
            for (int round = 1; round <= args.repeatCount; round++) {
                final CommandAPDU triggerAPDU = getInputAPDU(round);

                final String input = Util.bytesToHex(triggerAPDU.getBytes());
                log.info("Round: {}/{} APDU: {}", round, args.repeatCount, input);
                profileSingleStep(triggerAPDU);
            }

            // sanity check
            log.debug("Checking that no measurements are missing.");
            measurements.forEach((k, v) -> {
                if (v.size() != args.repeatCount)
                    throw new RuntimeException(k + ".size() != " + args.repeatCount);
            });
            if (inputs.size() != args.repeatCount)
                throw new RuntimeException("inputs.size() != " + args.repeatCount);


        } catch (CardException e) {
            throw new RuntimeException(e);
        }

        log.info("Collecting measurements complete.");
    }

    private void setTrap(short trapID) throws CardException {
        log.debug("Setting next trap to {}.", getTrapName(trapID));

        CommandAPDU setTrap = new CommandAPDU(args.cla, JCProfilerUtil.INS_PERF_HANDLER, 0, 0,
                                              Util.shortToByteArray(trapID));
        ResponseAPDU response = cardManager.transmit(setTrap);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException(String.format(
                    "Setting \"%s\" trap failed with SW %s",
                    getTrapName(trapID), Integer.toHexString(response.getSW())));
    }

    private void profileSingleStep(CommandAPDU triggerAPDu) throws CardException {
        long prevTransmitDuration = 0;
        long currentTransmitDuration;

        for (short trapID : trapNameMap.keySet()) {
            // set performance trap
            setTrap(trapID);

            // execute target operation
            final String trapName = getTrapName(trapID);
            log.debug("Measuring {}.", trapName);
            final ResponseAPDU response = cardManager.transmit(triggerAPDu);

            // SW should be equal to the trap ID
            final int SW = response.getSW();
            if (SW != Short.toUnsignedInt(trapID)) {
                // unknown SW returned
                if (SW != JCProfilerUtil.SW_NO_ERROR)
                    throw new RuntimeException(String.format(
                            "Unexpected SW received when profiling trap %s: %s", trapName, Integer.toHexString(SW)));

                // we have not reached expected performance trap
                unreachedTraps.add(trapName);
                measurements.computeIfAbsent(trapName, k -> new ArrayList<>()).add(null);
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

    @Override
    protected void saveMeasurements(final CSVPrinter printer) throws IOException {
        printer.printComment("trapName,measurement1,measurement2,...");
        for (final Map.Entry<String, List<Long>> e : measurements.entrySet()) {
            printer.print(e.getKey());
            printer.printRecord(e.getValue());
        }
    }
}
