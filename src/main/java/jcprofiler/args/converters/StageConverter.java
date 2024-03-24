// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.Stage;

/**
 * Parameter converter for the {@link Stage} enum
 */
public class StageConverter extends EnumConverter<Stage> {
    public StageConverter(String optionName, Class<Stage> clazz) {
        super(optionName, clazz);
    }
}
