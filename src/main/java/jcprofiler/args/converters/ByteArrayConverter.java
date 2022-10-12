package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import cz.muni.fi.crocs.rcard.client.Util;

public class ByteArrayConverter extends BaseConverter<byte[]> {
    public ByteArrayConverter(final String optionName) {
        super(optionName);
    }

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
