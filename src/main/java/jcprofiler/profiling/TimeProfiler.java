package jcprofiler.profiling;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.csv.CSVPrinter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.IOException;
import java.util.*;

/**
 * @author Lukáš Zaoral and Petr Svenda
 */
public class TimeProfiler extends AbstractProfiler {
    private static final short PERF_START = 0x0001;

    // use LinkedHashX to preserve insertion order
    private final Map<String, List<Long>> measurements = new LinkedHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(TimeProfiler.class);

    public TimeProfiler(final Args args, final CardManager cardManager, final SpoonAPI spoon) {
        super(args, cardManager, JCProfilerUtil.getProfiledMethod(spoon, args.method));
    }

    @Override
    public void profileImpl() {
        try {
            // reset if possible and erase any previous performance stop
            resetApplet();
            setTrap(PERF_START);

            for (int repeat = 1; repeat <= args.repeatCount; repeat++) {
                final CommandAPDU triggerAPDU = getRandomAPDU();

                final String input = Util.bytesToHex(triggerAPDU.getBytes());
                log.info("Round: {}/{} APDU: {}", repeat, args.repeatCount, input);
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
    }

    private void setTrap(short trapID) throws CardException {
        log.debug("Setting next trap to {}.", getTrapName(trapID));

        CommandAPDU setTrap = new CommandAPDU(args.cla, JCProfilerUtil.INS_PERF_HANDLER, 0, 0,
                                              Util.shortToByteArray(trapID));
        ResponseAPDU response = cardManager.transmit(setTrap);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException(String.format(
                    "Setting \"%s\" trap failed with SW %d", getTrapName(trapID), response.getSW()));
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

    @Override
    protected void saveMeasurements(final CSVPrinter printer) throws IOException {
        printer.printComment("trapName,measurement1,measurement2,...");
        for (final Map.Entry<String, List<Long>> e : measurements.entrySet()) {
            printer.print(e.getKey());
            printer.printRecord(e.getValue());
        }
    }
}
