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
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MemoryProfiler extends AbstractProfiler {
    // use LinkedHashX to preserve insertion order
    private final Map<String, Integer> memoryUsageTransientDeselect = new LinkedHashMap<>();
    private final Map<String, Integer> memoryUsageTransientReset = new LinkedHashMap<>();
    private final Map<String, Integer> memoryUsagePersistent = new LinkedHashMap<>();

    private final int valueBytes;

    private static final Logger log = LoggerFactory.getLogger(MemoryProfiler.class);

    public MemoryProfiler(final Args args, final CardManager cardManager, final SpoonAPI spoon) {
        super(args, cardManager, JCProfilerUtil.getProfiledExecutable(spoon, args.entryPoint, args.method));

        if (JCProfilerUtil.getEntryPoint(spoon, args.entryPoint).getField("INS_PERF_GETMEM") == null)
            throw new RuntimeException(
                    "Profiling in " + args.mode + " mode but PM class does not contain INS_PERF_GETMEM field!");

        valueBytes = getValueBytes(spoon);
        if (valueBytes == Integer.BYTES && args.useSimulator)
            throw new UnsupportedOperationException(
                    "jCardSim does not support the 3.0.4+ JCSystem.getAvailableMemory(short[],short,byte) overload!");
    }

    /**
     * Gets size of the measurement from the javacard.framework.JCSystem.getAvailableMemory
     * static method used in the PM.check(short) method in bytes. JCSDK 3.0.4+ return an
     * int and older return a short.
     *
     * @param spoon - Spoon instance
     * @return size of the getAvailableMemory measurement in bytes
     */
    private int getValueBytes(final SpoonAPI spoon) {
        // get PM.check(short) method
        final CtMethod<?> PM = spoon.getFactory().getModel().filterChildren(
                (CtMethod<?> m) -> m.getSignature().equals("check(short)") &&
                        m.getDeclaringType().getSimpleName().equals("PM")).first();

        // get all distinct return types of getAvailableMemory calls
        final List<CtTypeReference<?>> returnTypes = PM.getBody().getElements(
                (CtExecutableReference<?> e) -> e.getSimpleName().equals("getAvailableMemory") &&
                        e.getDeclaringType().getQualifiedName().equals("javacard.framework.JCSystem"))
                .stream().map(CtExecutableReference::getType).distinct().collect(Collectors.toList());
        if (returnTypes.size() != 1)
            throw new RuntimeException("The sources are broken! The PM.check(short) method contains more than one" +
                    "javacard.framework.JCSystem.getAvailableMemory overload!");

        return returnTypes.get(0).getSimpleName().equals("short") ? Short.BYTES : Integer.BYTES;
    }

    private void getMeasurements(final Map<String, Integer> map, final byte memType) throws CardException {
        final int arrayLength = trapNameMap.size() * valueBytes;
        final byte[] buffer = new byte[arrayLength];

        int part = 0;
        int remainingLength = arrayLength;

        while (remainingLength > 0) {
            final int nextLength = Math.min(remainingLength, 256);

            final CommandAPDU getMeasurements = new CommandAPDU(args.cla, JCProfilerUtil.INS_PERF_HANDLER, memType, part++);
            final ResponseAPDU response = cardManager.transmit(getMeasurements);
            if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
                throw new RuntimeException(
                        "Getting memory measurements failed with SW " + Integer.toHexString(response.getSW()));

            final byte[] responseData = response.getData();
            if (responseData.length != nextLength)
                throw new RuntimeException(String.format(
                        "The incoming measurement data have incorrect length! Expected: %d Actual: %d",
                        arrayLength, responseData.length));


            System.arraycopy(responseData, 0, buffer, arrayLength - remainingLength, responseData.length);
            remainingLength -= nextLength;
        }

        trapNameMap.forEach((trapID, trapName) -> {
            int idx = (Short.toUnsignedInt(trapID) - /* PERF_START */ 2) * valueBytes;

            Integer val;
            if (valueBytes == Short.BYTES) {
                val = (int) Util.getShort(buffer, idx);
            } else {
                val = Short.toUnsignedInt(Util.getShort(buffer, idx)) << Short.SIZE;
                val |= Short.toUnsignedInt(Util.readShort(buffer, idx + Short.BYTES));
            }

            // -1 corresponds to an unreachable trap
            if (val < 0) {
                if (val != -1)
                    throw new RuntimeException("The value of free memory measurement must be greater or equal -1");

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
            generateInputs(/* size */ 1);

            final CommandAPDU triggerAPDU = getInputAPDU(/* round */ 1);
            final String input = Util.bytesToHex(triggerAPDU.getBytes());
            log.info("APDU: {}", input);

            final ResponseAPDU response = cardManager.transmit(triggerAPDU);
            if (response.getSW() != JCProfilerUtil.SW_NO_ERROR)
                throw new RuntimeException(
                        "Executing the applet failed with SW " + Integer.toHexString(response.getSW()));

            log.info("Measuring {} complete.", profiledExecutableSignature);
        }

        final int limit = valueBytes == Integer.BYTES ? Integer.MAX_VALUE : Short.MAX_VALUE;
        log.info("Using JCSystem.getAvailableMemory with {} B limit.", limit);

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
