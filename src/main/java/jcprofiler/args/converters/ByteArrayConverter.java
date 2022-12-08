// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import cz.muni.fi.crocs.rcard.client.Util;

/**
 * Parameter converter for byte arrays
 */
public class ByteArrayConverter extends BaseConverter<byte[]> {
    public ByteArrayConverter(final String optionName) {
        super(optionName);
    }

    /**
     * Coverts a hexadecimal string into an array of bytes.
     *
     * @param  value input string
     * @return       converted {@link byte} array
     *
     * @throws ParameterException if the value is not a valid single byte hexadecimal string
     */
    @Override
    public byte[] convert(String value) {
        if (value.startsWith("0x"))
            value = value.trim().replaceFirst("^0x", "");

        if (value.length() % 2 == 1)
            throw new ParameterException(getErrorString(value, "is not a valid hex string"));

        try {
            // check that value is a hexstring
            return Util.hexStringToByteArray(value);
        } catch (NumberFormatException e) {
            throw new ParameterException(getErrorString(value, "is not a valid hex string"), e);
        }
    }
}
