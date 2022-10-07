package @PACKAGE@;

import javacard.framework.APDU;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class PM {
    private static final short ARRAY_LENGTH = 0;

    // Arrays storing the memory consumption for given trap
    public static final byte[] memoryUsageTransientDeselect = new byte[ARRAY_LENGTH];
    public static final byte[] memoryUsageTransientReset = new byte[ARRAY_LENGTH];
    public static final byte[] memoryUsagePersistent = new byte[ARRAY_LENGTH];

    // Store usage info for given trap
    public static void check(short stopCondition) {
        short trapID = (short) ((stopCondition - /* PERF_START */ 2) * Short.BYTES);

        Util.setShort(memoryUsageTransientDeselect, trapID, JCSystem.getAvailableMemory(JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT));
        Util.setShort(memoryUsageTransientReset, trapID, JCSystem.getAvailableMemory(JCSystem.MEMORY_TYPE_TRANSIENT_RESET));
        Util.setShort(memoryUsagePersistent, trapID, JCSystem.getAvailableMemory(JCSystem.MEMORY_TYPE_PERSISTENT));
    }

    // Send usage info back to the profiler
    public static void send(APDU apdu) {
        Util.arrayCopyNonAtomic(memoryUsageTransientDeselect, (short) 0, apdu.getBuffer(), (short) 0, ARRAY_LENGTH);
        Util.arrayCopyNonAtomic(memoryUsageTransientReset, (short) 0, apdu.getBuffer(), ARRAY_LENGTH, ARRAY_LENGTH);
        Util.arrayCopyNonAtomic(memoryUsagePersistent, (short) 0, apdu.getBuffer(), (short) (2 * ARRAY_LENGTH), ARRAY_LENGTH);

        apdu.setOutgoingAndSend((short) 0, (short) (3 * ARRAY_LENGTH));
    }
}
