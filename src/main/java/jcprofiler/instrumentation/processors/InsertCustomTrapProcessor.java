// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import spoon.reflect.declaration.CtExecutable;

/**
 * Class for performance trap insertion in custom mode
 * <br>
 * Applicable to instances of {@link CtExecutable}.
 */
public class InsertCustomTrapProcessor extends AbstractInsertTrapProcessor<CtExecutable<?>> {
    /**
     * Constructs the {@link InsertCustomTrapProcessor} class.
     *
     * @param args object with commandline arguments
     */
    public InsertCustomTrapProcessor(final Args args) {
        super(args);
    }
}
