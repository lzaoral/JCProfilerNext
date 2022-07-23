package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import pro.javacard.JavaCardSDK;

public class JCKitConverter extends BaseConverter<JavaCardSDK> {
    public JCKitConverter(final String optionName) {
        super(optionName);
    }

    @Override
    public JavaCardSDK convert(final String value) {
        final JavaCardSDK jcSDK = JavaCardSDK.detectSDK(value);
        if (jcSDK == null)
            throw new ParameterException(getErrorString(value, "a valid path to directory with JavaCard SDK"));

        return jcSDK;
    }
}
