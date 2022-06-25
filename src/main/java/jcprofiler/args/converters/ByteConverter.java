package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import cz.muni.fi.crocs.rcard.client.Util;

public class ByteConverter extends BaseConverter<Byte> {
    public ByteConverter(final String optionName) {
        super(optionName);
    }

    @Override
    public Byte convert(String value) {
        try {
            if (value.startsWith("0x"))
                value = value.replaceFirst("^0x", "");

            byte[] ret = Util.hexStringToByteArray(value);
            if (ret.length == 1)
                return ret[0];
        } catch (NumberFormatException ignored) {}
        throw new ParameterException(getErrorString(value, "a single byte"));
    }
}
