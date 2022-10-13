package jcprofiler.profiling;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;

import javacard.framework.JCSystem;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.csv.CSVPrinter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;
import spoon.reflect.declaration.CtMethod;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryProfiler extends AbstractProfiler {
    // use LinkedHashX to preserve insertion order
    final Map<String, Integer> memoryUsageTransientDeselect = new LinkedHashMap<>();
    final Map<String, Integer> memoryUsageTransientReset = new LinkedHashMap<>();
    final Map<String, Integer> memoryUsagePersistent = new LinkedHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(MemoryProfiler.class);

    public MemoryProfiler(final Args args, final CardManager cardManager, final SpoonAPI spoon) {
        super(args, cardManager, JCProfilerUtil.getProfiledExecutable(spoon, args.entryPoint, args.method));

        if (JCProfilerUtil.getEntryPoint(spoon, args.entryPoint).getField("INS_PERF_GETMEM") == null)
            throw new RuntimeException(
                    "Profiling in " + args.mode + " mode but PM class does not contain INS_PERF_GETMEM field!");
    }

    private void getMeasurements(final Map<String, Integer> map, final byte memType) throws CardException {
        final int arrayLength = trapNameMap.size() * Short.BYTES;
        final byte[] buffer = new byte[arrayLength];

        int part = 0;
        int remainingLength = arrayLength;

        while (remainingLength > 0) {
            final int nextLength = Math.min(remainingLength, 256);

            final CommandAPDU getMeasurements = new CommandAPDU(args.cla, JCProfilerUtil.INS_PERF_HANDLER, memType, part++);
            final ResponseAPDU response = cardManager.transmit(getMeasurements);
            if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
                throw new RuntimeException("Getting memory measurements failed with SW " + response.getSW());

            final byte[] responseData = response.getData();
            if (responseData.length != nextLength)
                throw new RuntimeException(String.format(
                        "The incoming measurement data have incorrect length! Expected: %d Actual: %d",
                        arrayLength, responseData.length));


            System.arraycopy(responseData, 0, buffer, arrayLength - remainingLength, responseData.length);
            remainingLength -= nextLength;
        }

        trapNameMap.forEach((trapID, trapName) -> {
            int idx = (Short.toUnsignedInt(trapID) - /* PERF_START */ 2) * Short.BYTES;

            Integer val = Short.toUnsignedInt(Util.getShort(buffer, idx));
            if (val == 0) {
                unreachedTraps.add(trapName);
                val = null;
            }
            map.put(trapName, val);
        });
    }

    @Override
    protected void profileImpl() throws CardException {
        // methods must be executed explicitly
        if (profiledExecutable instanceof CtMethod) {
            resetApplet();

            final CommandAPDU triggerAPDU = getRandomAPDU();
            final ResponseAPDU response = cardManager.transmit(triggerAPDU);

            if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
                throw new RuntimeException("Executing the applet failed with SW " + response.getSW());

            log.info("Measuring {} complete.", profiledExecutableSignature);
        }

        log.info("Retrieving measurements from the card.");
        getMeasurements(memoryUsageTransientDeselect, JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT);
        getMeasurements(memoryUsageTransientReset, JCSystem.MEMORY_TYPE_TRANSIENT_RESET);
        getMeasurements(memoryUsagePersistent, JCSystem.MEMORY_TYPE_PERSISTENT);
        log.info("Measurements retrieved successfully.");
    }

    @Override
    protected void saveMeasurements(final CSVPrinter printer) throws IOException {
        printer.printComment("trapName,freeTransientDeselect,freeTransientReset,freePersistent");
        for (final String k : memoryUsagePersistent.keySet()) {
            printer.printRecord(k, memoryUsageTransientDeselect.get(k), memoryUsageTransientReset.get(k),
                    memoryUsagePersistent.get(k));
        }
    }
}
