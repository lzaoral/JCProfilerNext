// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import cz.muni.fi.crocs.rcard.client.Util;

/**
 * Parameter converter for single bytes
 */
public class ByteConverter extends BaseConverter<Byte> {
    public ByteConverter(final String optionName) {
        super(optionName);
    }

    /**
     * Coverts a hexadecimal string into a single byte.
     *
     * @param  value input string
     * @return       converted {@link byte}
     *
     * @throws ParameterException if the value is not a valid single byte hexadecimal string
     */
    @Override
    public Byte convert(String value) {
        if (value.startsWith("0x"))
            value = value.trim().replaceFirst("^0x", "");

        try {
            byte[] ret = Util.hexStringToByteArray(value);
            if (ret.length == 1)
                return ret[0];
        } catch (NumberFormatException e) {
            throw new ParameterException(getErrorString(value, "a single byte"), e);
        }

        throw new ParameterException(getErrorString(value, "a single byte"));
    }
}
