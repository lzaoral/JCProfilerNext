package jcprofiler.profiling;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.Util;

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
    }

    private void processMeasurement(final Map<String, Integer> map, final String key,
                                    final byte[] array, final int offset) {
        Integer val = Short.toUnsignedInt(Util.getShort(array, offset));
        if (val == 0) {
            unreachedTraps.add(key);
            val = null;
        }
        map.put(key, val);
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

        final int arrayLength = trapNameMap.size() * Short.BYTES;
        if (arrayLength * 3 > 256)
            throw new UnsupportedOperationException(JCProfilerUtil.getFullSignature(profiledExecutable) +
                    " has too many traps! This is temporarily unsupported!");

        log.info("Retrieving measurements from the card.");

        final CommandAPDU getMeasurements = new CommandAPDU(args.cla, JCProfilerUtil.INS_PERF_HANDLER, 0, 0);
        final ResponseAPDU response = cardManager.transmit(getMeasurements);
        if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
            throw new RuntimeException("Getting memory measurements failed with SW " + response.getSW());

        final byte[] responseData = response.getData();
        if (responseData.length != arrayLength * 3)
            throw new RuntimeException(String.format(
                    "The incoming measurement data have incorrect length! Expected: %d Actual: %d",
                    arrayLength * 3, responseData.length));

        log.info("Measurements retrieved successfully.");

        trapNameMap.forEach((k ,v) -> {
            int arrayIdx = (Short.toUnsignedInt(k) - /* PERF_START */ 2) * Short.BYTES;

            processMeasurement(memoryUsageTransientDeselect, v, responseData, arrayIdx);
            processMeasurement(memoryUsageTransientReset, v, responseData, arrayIdx + arrayLength);
            processMeasurement(memoryUsagePersistent, v, responseData, arrayIdx + arrayLength * 2);
        });
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
