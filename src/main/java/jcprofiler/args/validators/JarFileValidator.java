// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.IOException;
import java.util.jar.JarFile;

/**
 * Parameter validator for JAR archives
 */
public class JarFileValidator implements IParameterValidator {
    /**
     * Checks that the parameter points to a path to a JAR archive.
     *
     * @param  name  parameter name
     * @param  value input string
     *
     * @throws ParameterException if the value does not point to a valid path to a JAR archive
     */
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
