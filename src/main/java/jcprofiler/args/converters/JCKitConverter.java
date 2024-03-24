// SPDX-FileCopyrightText: 2022-2024 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import pro.javacard.JavaCardSDK;

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
        final JavaCardSDK jcSDK = JavaCardSDK.detectSDK(value);
        if (jcSDK == null)
            throw new ParameterException(getErrorString(value, "a valid path to directory with JavaCard SDK"));

        return jcSDK;
    }
}
