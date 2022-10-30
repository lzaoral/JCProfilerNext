package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.IOException;
import java.util.jar.JarFile;

public class JarFileValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        try {
            final JarFile j = new JarFile(value);
            j.close();
        } catch (IOException e) {
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" is not a valid JAR archive", name, value), e);
        }
    }
}
