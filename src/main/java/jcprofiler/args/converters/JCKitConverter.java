package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import pro.javacard.JavaCardSDK;

import java.io.File;

public class JCKitConverter extends BaseConverter<JavaCardSDK> {
    public JCKitConverter(final String optionName) {
        super(optionName);
    }

    @Override
    public JavaCardSDK convert(final String value) {
        final File path = new File(value);
        if (!path.isDirectory())
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" in not a valid path to a directory", getOptionName(), value));

        final JavaCardSDK jcSDK = JavaCardSDK.detectSDK(value);
        if (jcSDK == null)
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" in not a valid path to directory with JavaCard SDK", getOptionName(), value));

        return jcSDK;
    }
}
