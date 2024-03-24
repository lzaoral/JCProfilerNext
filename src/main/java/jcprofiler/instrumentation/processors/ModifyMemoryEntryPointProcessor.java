// SPDX-FileCopyrightText: 2022-2024 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;

import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;

/**
 * Class for modification of entry point classes in memory mode
 */
public class ModifyMemoryEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    /**
     * Constructs the {@link ModifyMemoryEntryPointProcessor} class.
     *
     * @param args object with commandline arguments
     */
    public ModifyMemoryEntryPointProcessor(final Args args) {
        super(args);
    }

    /**
     * Inserts an {@code INS_PERF_GETMEM} instruction and its handler
     * into a given {@link CtClass} instance.
     *
     * @param cls class to be processed
     */
    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_GETMEM");
    }

    /**
     * Creates a body of the {@code INS_PERF_GETMEM} instruction handler.
     *
     * @param  apdu process method argument instance
     * @return      a {@link CtBlock} instance with the {@code INS_PERF_GETMEM}
     *              instruction handler body
     */
    @Override
    protected CtBlock<Void> createInsHandlerBody(final CtVariableRead<APDU> apdu) {
        // PM.send(${param})
        final CtInvocation<?> PMSendCall = getFactory().createInvocation(
                getFactory().createTypeAccess(PM.getReference(), false),
                PM.getMethod("send", apdu.getType()).getReference(), apdu);

        // {
        //     PM.send(${param});
        //     return;
        // }
        return getFactory().createBlock().addStatement(PMSendCall).addStatement(getFactory().createReturn());
    }
}
