package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;

import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;

public class ModifyTimeEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    public ModifyTimeEntryPointProcessor(final Args args) {
        super(args);
    }

    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_SETSTOP");
    }

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
