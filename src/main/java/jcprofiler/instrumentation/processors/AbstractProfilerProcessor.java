// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <x456487@fi.muni.cz>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

/**
 * Base class for modification of the source code
 *
 * @param <T> processed AST node type and an instance of {@link CtElement}
 */
public abstract class AbstractProfilerProcessor<T extends CtElement> extends AbstractProcessor<T> {
    /**
     * Commandline arguments
     */
    protected final Args args;

    /**
     * Instance of the PM class
     */
    protected CtType<?> PM;

    /**
     * Instance of the PMC class
     */
    protected CtType<?> PMC;

    /**
     * Constructs the {@link AbstractProfilerProcessor} class.
     *
     * @param args object with commandline arguments
     */
    protected AbstractProfilerProcessor(final Args args) {
        this.args = args;
    }

    /**
     * Initializes the given {@link spoon.processing.Processor} instance.
     */
    @Override
    public void init() {
        super.init();

        // initialise PM and PMC fields
        final CtModel model = getFactory().getModel();
        PM = JCProfilerUtil.getToplevelType(model, "PM");
        PMC = JCProfilerUtil.getToplevelType(model, "PMC");
    }
}
