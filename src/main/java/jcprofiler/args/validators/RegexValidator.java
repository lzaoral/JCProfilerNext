package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        try {
            Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" is not a valid regular expression", name, value));
        }
    }
}
