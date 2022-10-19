package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;

import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;

public class ModifyMemoryEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    public ModifyMemoryEntryPointProcessor(final Args args) {
        super(args);
    }

    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_GETMEM");
    }

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
