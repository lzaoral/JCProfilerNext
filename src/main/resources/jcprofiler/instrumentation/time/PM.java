package jcprofiler;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class PM {
    // Performance measurement stop indicator
    private static short nextPerfStop = PMC.PERF_START;

    // if m_perfStop equals to stopCondition, exception is thrown (trap hit)
    public static void check(short stopCondition) {
        if (PM.nextPerfStop == stopCondition) {
            ISOException.throwIt(stopCondition);
        }
    }

    // Set next fatal performace trap
    public static void set(APDU apdu) {
        nextPerfStop = Util.getShort(apdu.getBuffer(), ISO7816.OFFSET_CDATA);
    }
}
