package jcprofiler;

// Explicit imports to ensure that they are not shadowed by other classes from the applet package.
import java.lang.Integer;
import java.lang.Short;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

/**
 * PM class for memory usage measurement for JCSDK 3.0.4 and newer
 */
public class PM {
    private static final short ARRAY_LENGTH = 0;
    private static final short MAX_APDU_LENGTH = 256;

    // Arrays storing the amount of free memory for each trap as integers
    private static final byte[] memoryUsageTransientDeselect = new byte[ARRAY_LENGTH];
    private static final byte[] memoryUsageTransientReset = new byte[ARRAY_LENGTH];
    private static final byte[] memoryUsagePersistent = new byte[ARRAY_LENGTH];

    // buffer
    private static final short[] buffer = new short[Integer.BYTES];

    private static boolean initialised = false;

    /**
     * Initialise all arrays with -1 integer values which
     * correspond to unreachable traps.
     */
    private static void initialise() {
        Util.arrayFillNonAtomic(memoryUsageTransientDeselect, (short) 0, ARRAY_LENGTH, (byte) 0xFF);
        Util.arrayFillNonAtomic(memoryUsageTransientReset, (short) 0, ARRAY_LENGTH, (byte) 0xFF);
        Util.arrayFillNonAtomic(memoryUsagePersistent, (short) 0, ARRAY_LENGTH, (byte) 0xFF);

        initialised = true;
    }

    /**
     * Stores the amount of free memory for the given trap.
     * The maximum value is capped by {@link Integer#MAX_VALUE}.
     *
     * @param stopCondition ID of the reached trap
     */
    public static void check(short stopCondition) {
        if (!initialised)
            initialise();

        short trapID = (short) ((stopCondition - /* PERF_START */ 2) * Integer.BYTES);

        JCSystem.getAvailableMemory(buffer, (short) 0, JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT);
        Util.setShort(memoryUsageTransientDeselect, trapID, buffer[0]);
        Util.setShort(memoryUsageTransientDeselect, (short) (trapID + Short.BYTES), buffer[1]);

        JCSystem.getAvailableMemory(buffer, (short) 0, JCSystem.MEMORY_TYPE_TRANSIENT_RESET);
        Util.setShort(memoryUsageTransientReset, trapID, buffer[0]);
        Util.setShort(memoryUsageTransientReset, (short) (trapID + Short.BYTES), buffer[1]);

        JCSystem.getAvailableMemory(buffer, (short) 0, JCSystem.MEMORY_TYPE_PERSISTENT);
        Util.setShort(memoryUsagePersistent, trapID, buffer[0]);
        Util.setShort(memoryUsagePersistent, (short) (trapID + Short.BYTES), buffer[1]);
    }

    /**
     * Copy and send the P2th part of the selected byte
     * array back to the profiler.
     *
     * @param  arr          byte array
     * @param  apdu         input APDU
     * @throws ISOException if the P2 byte has a wrong value
     */
    private static void sendArray(byte[] arr, APDU apdu) {
        short part = (short) (apdu.getBuffer()[ISO7816.OFFSET_P2] & 0x00FF);
        short beginOffset = (short) (part * MAX_APDU_LENGTH);

        if (beginOffset > ARRAY_LENGTH)
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);

        short remainingLength = (short) (ARRAY_LENGTH - beginOffset);
        short length = remainingLength > MAX_APDU_LENGTH ? MAX_APDU_LENGTH : remainingLength;

        Util.arrayCopyNonAtomic(arr, beginOffset, apdu.getBuffer(), (short) 0, length);
        apdu.setOutgoingAndSend((short) 0, length);
    }

    /**
     * Sends a part of the memory usage info back to the profiler.
     * The P1 byte selects the memory type. See {@link #sendArray}
     * for details about the P2 byte.
     *
     * @param  apdu         input APDU
     * @throws ISOException if the P1 byte has a wrong value
     */
    public static void send(APDU apdu) {
        switch (apdu.getBuffer()[ISO7816.OFFSET_P1]) {
            case JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT:
                sendArray(memoryUsageTransientDeselect, apdu);
                break;
            case JCSystem.MEMORY_TYPE_TRANSIENT_RESET:
                sendArray(memoryUsageTransientReset, apdu);
                break;
            case JCSystem.MEMORY_TYPE_PERSISTENT:
                sendArray(memoryUsagePersistent, apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }
}
