package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;

import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;

/**
 * Class for modification of entry point classes in time mode
 */
public class ModifyTimeEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    /**
     * Constructs the {@link ModifyTimeEntryPointProcessor} class.
     *
     * @param args object with commandline arguments
     */
    public ModifyTimeEntryPointProcessor(final Args args) {
        super(args);
    }

    /**
     * Inserts an {@code INS_PERF_SETSTOP} instruction and its handler
     * into a given {@link CtClass} instance.
     *
     * @param cls class to be processed
     */
    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_SETSTOP");
    }

    /**
     * Creates a body of the {@code INS_PERF_SETSTOP} instruction handler.
     *
     * @param  apdu process method argument instance
     * @return      a {@link CtBlock} instance with the {@code INS_PERF_SETSTOP}
     *              instruction handler body
     */
    @Override
    protected CtBlock<Void> createInsHandlerBody(final CtVariableRead<APDU> apdu) {
        // PM.set(${param})
        final CtInvocation<?> PMSetCall = getFactory().createInvocation(
                getFactory().createTypeAccess(PM.getReference(), false),
                PM.getMethod("set", apdu.getType()).getReference(), apdu);

        // {
        //     PM.set(${param});
        //     return;
        // }
        return getFactory().createBlock().addStatement(PMSetCall).addStatement(getFactory().createReturn());
    }
}
