package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parameter validator for regular expressions
 */
public class RegexValidator implements IParameterValidator {
    /**
     * Checks that the parameter represents a regular expression.
     *
     * @param  name  parameter name
     * @param  value input string
     *
     * @throws ParameterException if the value does not represent a regular expression
     */
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
