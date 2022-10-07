package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;

import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtParameter;

public class ModifyMemoryEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    public ModifyMemoryEntryPointProcessor(final Args args) {
        super(args);
    }

    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_GETMEM");
    }

    @Override
    protected CtBlock<Void> createInsHandlerBody(final CtParameter<?> apdu) {
        // ${param}
        final CtVariableAccess<?> apduParamRead = getFactory().createVariableRead(apdu.getReference(), false);

        // PM.send(${param})
        final CtInvocation<?> PMSendCall = getFactory().createInvocation(
                getFactory().createTypeAccess(PM.getReference(), false),
                PM.getMethod("send", getFactory().createCtTypeReference(APDU.class)).getReference(),
                apduParamRead);

        // {
        //     PM.send(${param});
        //     return;
        // }
        return getFactory().createBlock().addStatement(PMSendCall).addStatement(getFactory().createReturn());
    }
}
