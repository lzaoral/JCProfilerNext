// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import pro.javacard.sdk.JavaCardSDK;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Parameter converter for the {@link JavaCardSDK} type
 */
public class JCKitConverter extends BaseConverter<JavaCardSDK> {
    public JCKitConverter(final String optionName) {
        super(optionName);
    }

    /**
     * Coverts a hexadecimal string into a single byte.
     *
     * @param  value input string
     * @return       converted {@link JavaCardSDK} object
     *
     * @throws ParameterException if the value does not point to a valid path with JavaCard SDK
     */
    @Override
    public JavaCardSDK convert(final String value) {
        final Optional<JavaCardSDK> jcSDK = JavaCardSDK.detectSDK(Paths.get(value));
        return jcSDK.orElseThrow(() -> new ParameterException(
                getErrorString(value, "a valid path to directory with JavaCard SDK")));
    }
}
