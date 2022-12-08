// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

import javacard.framework.*;

public class Example extends Applet {
    public static final byte INS_EXAMPLE = (byte) 0xEE;

    Example() {}

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Example().register();
    }

    @Override
    public void process(APDU apdu) throws ISOException {
        if (selectingApplet())
            return;

        switch (apdu.getBuffer()[ISO7816.OFFSET_INS]) {
            case INS_EXAMPLE:
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
