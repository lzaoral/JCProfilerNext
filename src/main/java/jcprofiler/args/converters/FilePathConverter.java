package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import java.io.File;
import java.nio.file.Path;

public class FilePathConverter extends BaseConverter<Path> {
    public FilePathConverter(String optionName) {
        super(optionName);
    }

    @Override
    public Path convert(String value) {
        final File input = new File(value);
        if (input.isDirectory())
            throw new ParameterException(getErrorString(value, "a path to an existing regular file"));
        return input.toPath();
    }
}
