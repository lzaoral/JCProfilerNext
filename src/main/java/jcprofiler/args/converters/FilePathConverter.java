package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilePathConverter extends BaseConverter<Path> {
    public FilePathConverter(String optionName) {
        super(optionName);
    }

    @Override
    public Path convert(String value) {
        final Path input = Paths.get(value);
        if (!Files.exists(input) || Files.isDirectory(input))
            throw new ParameterException(getErrorString(value, "a path to an existing regular file"));
        return input.toAbsolutePath();
    }
}
