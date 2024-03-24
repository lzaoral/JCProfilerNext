// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;
import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Class for modification of entry point classes in custom mode
 */
public class ModifyCustomEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    /**
     * Constructs the {@link ModifyCustomEntryPointProcessor} class.
     *
     * @param args object with commandline arguments
     */
    public ModifyCustomEntryPointProcessor(final Args args) {
        super(args);
    }

    /**
     * Decides whether the input {@link CtClass} should be processed.
     *
     * @param  cls the candidate class
     * @return     true if yes, otherwise false
     */
    @Override
    public boolean isToBeProcessed(final CtClass<?> cls) {
        return args.customHandler != null && super.isToBeProcessed(cls);
    }

    /**
     * Inserts an {@code INS_PERF_CUSTOM} instruction and its handler
     * into a given {@link CtClass} instance.
     *
     * @param cls class to be processed
     */
    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_CUSTOM");
    }

    /**
     * Creates a body of the {@code INS_PERF_CUSTOM} instruction handler.
     *
     * @param  apdu process method argument instance
     * @return      a {@link CtBlock} instance with the {@code INS_PERF_CUSTOM}
     *              instruction handler body
     */
    @Override
    protected CtBlock<Void> createInsHandlerBody(final CtVariableRead<APDU> apdu) {
        try {
            final CtStatement customBlock = getFactory().createCodeSnippetStatement(
                    new String(Files.readAllBytes(args.customHandler)));

            // {
            //     ${customBlock}
            //     return;
            // }
            return getFactory().createBlock().addStatement(customBlock).addStatement(getFactory().createReturn());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
