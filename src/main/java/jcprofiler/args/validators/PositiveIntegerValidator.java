package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class PositiveIntegerValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        try {
            int n = Integer.parseInt(value);
            if (n < 1)
                throw new ParameterException(String.format(
                        "\"%s\": \"%s\" is not a positive integer", name, value));
        } catch (NumberFormatException e) {
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" is not a positive integer", name, value), e);
        }
    }
}
