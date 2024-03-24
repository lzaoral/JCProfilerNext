// SPDX-FileCopyrightText: 2022-2024 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Parameter converter for absolute paths to existing directory
 */
public class DirectoryPathConverter extends BaseConverter<Path> {
    public DirectoryPathConverter(String optionName) {
        super(optionName);
    }

    /**
     * Coverts a string into an absolute path to existing directory.
     *
     * @param  value input string
     * @return       absolute {@link Path} to the directory
     *
     * @throws ParameterException if the value is not a path or points to a regular file
     */
    @Override
    public Path convert(String value) {
        final Path input = Paths.get(value);
        if (!Files.isDirectory(input))
            throw new ParameterException(getErrorString(value, "a path to existing directory"));
        return input.toAbsolutePath().normalize();
    }
}
