// SPDX-FileCopyrightText: 2022-2024 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * Parameter validator for positive integers
 */
public class PositiveIntegerValidator implements IParameterValidator {
    /**
     * Checks that the parameter represents a positive integer.
     *
     * @param  name  parameter name
     * @param  value input string
     *
     * @throws ParameterException if the value does not represent a positive integer
     */
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
