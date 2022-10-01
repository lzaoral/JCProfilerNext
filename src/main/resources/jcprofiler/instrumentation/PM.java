package @PACKAGE@;

import javacard.framework.ISOException;

public class PM {
    // Performance measurement stop indicator
    public static short m_perfStop = PMC.PERF_START;

    // if m_perfStop equals to stopCondition, exception is thrown (trap hit)
    public static void check(short stopCondition) {
        if (PM.m_perfStop == stopCondition) {
            ISOException.throwIt(stopCondition);
        }
    }
}
