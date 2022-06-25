package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;

public class InputPathValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        if (!new File(value).exists())
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" in not a path to existing file or directory", name, value));
    }
}
