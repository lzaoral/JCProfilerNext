// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.Mode;

/**
 * Parameter converter for the {@link Mode} enum
 */
public class ModeConverter extends EnumConverter<Mode> {
    public ModeConverter(String optionName, Class<Mode> clazz) {
        super(optionName, clazz);
    }
}
