package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class PositiveIntegerValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        int n = Integer.parseInt(value);
        if (n < 1)
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" is not a positive integer", name, value));
    }
}
