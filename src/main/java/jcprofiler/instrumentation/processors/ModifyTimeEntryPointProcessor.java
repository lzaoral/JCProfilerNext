package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Util;

import jcprofiler.args.Args;

import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ModifyTimeEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    public ModifyTimeEntryPointProcessor(final Args args) {
        super(args);
    }

    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_SETSTOP");
    }

    @Override
    protected CtBlock<Void> createInsHandlerBody(final CtParameter<?> apdu) {
        final CtTypeReference<Short> shortRef = getFactory().createCtTypeReference(Short.TYPE);

        // ${param}
        final CtVariableAccess<?> apduParamRead = getFactory().createVariableRead(apdu.getReference(), false);

        // PM.m_perfStop
        final CtFieldWrite<Short> fieldWrite = getFactory().createFieldWrite();
        {
            final CtField<?> perfStopField = PM.getField("m_perfStop");

            if (!perfStopField.getType().equals(shortRef))
                throw new RuntimeException(String.format(
                        "PM.m_perfStop has type %s! Expected: short", perfStopField.getType().getQualifiedName()));

            @SuppressWarnings("unchecked") // the runtime check is above
            final CtField<Short> perfStopFieldCasted = (CtField<Short>) perfStopField;

            fieldWrite.setTarget(getFactory().createTypeAccess(PM.getReference()));
            fieldWrite.setVariable(perfStopFieldCasted.getReference());
        }

        // get ${param}.getBuffer()
        CtInvocation<?> getBufferInvocation;
        try {
            final Method getBufferMethod = APDU.class.getMethod("getBuffer");
            getBufferInvocation = getFactory().createInvocation(
                    apduParamRead, getFactory().Method().createReference(getBufferMethod));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // Util.getShort(${param}.getBuffer(), ISO7816.OFFSET_CDATA)
        CtInvocation<Short> makeShortInvocation;
        try {
            // get javacard.framework.ISO7816.OFFSET_CDATA
            final Class<?> iso7816Class = ISO7816.class;
            final Field offsetCdata = iso7816Class.getField("OFFSET_CDATA");

            final CtFieldRead<Short> offsetFieldRead = getFactory().createFieldRead();
            offsetFieldRead.setTarget(getFactory().createTypeAccess(getFactory().createCtTypeReference(iso7816Class)));
            offsetFieldRead.setVariable(getFactory().Field().createReference(offsetCdata));

            // get javacard.framework.Util.getShort(byte[] bArray, short bOff)
            final Class<?> utilClass = Util.class;
            final Method getShortMethod = utilClass.getMethod("getShort", byte[].class, Short.TYPE);

            // create Util.getShort(${param}.getBuffer(), ISO7816.OFFSET_CDATA)
            makeShortInvocation = getFactory().createInvocation(
                    getFactory().createTypeAccess(getFactory().Class().createReference(utilClass)),
                    getFactory().Method().createReference(getShortMethod),
                    getBufferInvocation, offsetFieldRead);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // {
        //     PM.m_perfStop = Util.getShort(${param}.getBuffer(), ISO7816.OFFSET_CDATA);
        //     return;
        // }
        final CtBlock<Void> perfStopHandlerBlock = getFactory().createBlock();
        {
            final CtAssignment<Short, Short> perfStopAssign = getFactory().createAssignment();
            perfStopAssign.setType(shortRef);
            perfStopAssign.setAssigned(fieldWrite);
            perfStopAssign.setAssignment(makeShortInvocation);

            perfStopHandlerBlock.addStatement(perfStopAssign);
            perfStopHandlerBlock.addStatement(getFactory().createReturn());
        }

        return perfStopHandlerBlock;
    }
}
