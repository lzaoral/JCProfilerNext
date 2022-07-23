package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import java.io.File;
import java.nio.file.Path;

public class DirectoryPathConverter extends BaseConverter<Path> {
    public DirectoryPathConverter(String optionName) {
        super(optionName);
    }

    @Override
    public Path convert(String value) {
        final File input = new File(value);
        if (!input.isDirectory())
            throw new ParameterException(getErrorString(value, "a path to existing directory"));
        return input.toPath();
    }
}
