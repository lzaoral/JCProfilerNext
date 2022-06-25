package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.util.Objects;

public class OutputPathValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        final File path = new File(value);
        if (path.exists() && !path.isDirectory())
            throw new ParameterException(String.format(
                    "\"%s\": \"%s\" is not a path to directory", name, value));

        if (path.isDirectory() && Objects.requireNonNull(path.list()).length > 1)
            System.err.printf("WARNING: Path %s is non-empty!%n", path);

        if (!path.exists() && !path.mkdirs())
            throw new ParameterException(String.format(
                    "Directories in path %s were not created successfully!", value));
    }
}
