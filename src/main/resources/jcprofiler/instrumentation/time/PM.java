// SPDX-FileCopyrightText: 2017-2021 Petr Švenda <petrsgit@gmail.com>
// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: MIT

package jcprofiler;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * PM class for time measurement
 */
public class PM {
    // Performance measurement stop indicator
    private static short nextPerfStop = PMC.PERF_START;

    /**
     * If the argument equals to {@link #nextPerfStop}, an exception
     * with its ID as the causewill be thrown.
     *
     * @param  stopCondition ID of the currently visited trap
     * @throws ISOException  if the fatal trap was reached
     */
    public static void check(short stopCondition) {
        if (nextPerfStop == stopCondition)
            ISOException.throwIt(stopCondition);
    }

    /**
     * Set the ID of next fatal trap.
     *
     * @param apdu input APDU
     */
    public static void set(APDU apdu) {
        nextPerfStop = Util.getShort(apdu.getBuffer(), ISO7816.OFFSET_CDATA);
    }
}
