package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Util;

import jcprofiler.util.JCProfilerUtil;
import jcprofiler.args.Args;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ModifyEntryPointProcessor extends AbstractProcessor<CtClass<?>> {
    final Args args;

    private static final Logger log = LoggerFactory.getLogger(ModifyEntryPointProcessor.class);

    public ModifyEntryPointProcessor(final Args args) {
        this.args = args;
    }

    @Override
    public boolean isToBeProcessed(final CtClass<?> cls) {
        // isEntryPoint && (!entryPointArg.isEmpty() => cls.SimpleName == entryPointArg)
        return JCProfilerUtil.isClsEntryPoint(cls) &&
                (args.entryPoint.isEmpty() || cls.getQualifiedName().equals(args.entryPoint));
    }

    @Override
    public void process(final CtClass<?> cls) {
        log.info("Instrumenting entry point class {}.", cls.getQualifiedName());
        final CtField<Byte> insPerfSetStop = addInsPerfSetStopField(cls);
        addInsPerfSetStopSwitchCase(cls, insPerfSetStop);
    }

    private CtField<Byte> addInsPerfSetStopField(final CtClass<?> cls) {
        // private static final byte INS_PERF_STOP = (byte) 0xf5
        final CtTypeReference<Byte> byteRef = getFactory().createCtTypeReference(Byte.TYPE);

        Optional<CtField<?>> existingInsPerfSetStop = cls.getFields().stream().filter(
                f -> f.getSimpleName().equals("INS_PERF_SETSTOP")).findFirst();
        if (existingInsPerfSetStop.isPresent()) {
            final CtField<?> insPerfSetStop = existingInsPerfSetStop.get();
            log.info("Existing INS_PERF_SETSTOP field found at {}.", insPerfSetStop.getPosition());

            if (!insPerfSetStop.getType().equals(byteRef))
                throw new RuntimeException(String.format(
                        "Existing INS_PERF_SETSTOP field has type %s! Expected: %s",
                        insPerfSetStop.getType().getQualifiedName(), Byte.TYPE.getTypeName()));

            @SuppressWarnings("unchecked") // the runtime check is above
            final CtField<Byte> insPerfSetStopCasted = (CtField<Byte>) insPerfSetStop;

            // private or public modifier does NOT make a difference in this case
            if (!insPerfSetStopCasted.hasModifier(ModifierKind.FINAL) ||
                    !insPerfSetStopCasted.hasModifier(ModifierKind.STATIC))
                throw new RuntimeException(
                        "Existing INS_PERF_SETSTOP field is not static and final! Got: " +
                        // Set<ModifierKind> does not have a stable ordering
                        insPerfSetStopCasted.getModifiers().stream().sorted().collect(Collectors.toList()));

            final CtLiteral<? extends Number> lit = insPerfSetStopCasted.getAssignment().partiallyEvaluate();
            if (lit.getValue().byteValue() != JCProfilerUtil.INS_PERF_SETSTOP)
                throw new RuntimeException(String.format(
                        "Existing INS_PERF_SETSTOP field has %s as initializer! Expected: (byte) 0x%02x",
                        insPerfSetStopCasted.getAssignment().prettyprint(), JCProfilerUtil.INS_PERF_SETSTOP));

            return insPerfSetStopCasted;
        }

        // create new INS_PERF_SETSTOP field
        final CtField<Byte> insPerfSetStop = getFactory().createCtField(
                "INS_PERF_SETSTOP", byteRef, "", ModifierKind.PUBLIC, ModifierKind.FINAL, ModifierKind.STATIC);

        // create and set the initializer
        final CtLiteral<Integer> initializer = getFactory().createLiteral(Byte.toUnsignedInt(JCProfilerUtil.INS_PERF_SETSTOP));
        initializer.setBase(LiteralBase.HEXADECIMAL);
        initializer.addTypeCast(byteRef);

        @SuppressWarnings("unchecked")
        // Unfortunately, this is the best solution we have since SPOON does not reflect type casts in type parameters.
        final CtLiteral<Byte> initializerCasted = (CtLiteral<Byte>) (Object) initializer;
        insPerfSetStop.setAssignment(initializerCasted);

        // insert the field
        cls.addField(0, insPerfSetStop);
        log.debug("Added INS_PERF_SETSTOP field to {}.", cls.getQualifiedName());
        return insPerfSetStop;
    }

    private CtCase<Byte> createPerfStopSwitchCase(final CtMethod<?> processMethod, final CtField<Byte> insPerfSetStop) {
        final CtClass<?> cls = processMethod.getParent(CtClass.class::isInstance);
        final CtCase<Byte> ctCase = getFactory().createCase();

        final CtTypeReference<Short> shortRef = getFactory().createCtTypeReference(Short.TYPE);

        // case INS_PERF_SETSTOP:
        {
            final CtFieldRead<Byte> fieldAccess = getFactory().createFieldRead();
            fieldAccess.setTarget(getFactory().createTypeAccess(cls.getReference(), true));
            fieldAccess.setVariable(insPerfSetStop.getReference());
            ctCase.setCaseExpression(fieldAccess);
        }

        // PM.m_perfStop
        final CtFieldWrite<Short> fieldWrite = getFactory().createFieldWrite();
        {
            final CtClass<?> pmClass = cls.getPackage().getType("PM");
            final CtField<?> perfStopField = pmClass.getField("m_perfStop");

            if (!perfStopField.getType().equals(shortRef))
                throw new RuntimeException(String.format(
                        "PM.m_perfStop has type %s! Expected: short", perfStopField.getType().getQualifiedName()));

            @SuppressWarnings("unchecked") // the runtime check is above
            final CtField<Short> perfStopFieldCasted = (CtField<Short>) perfStopField;

            fieldWrite.setTarget(getFactory().createTypeAccess(pmClass.getReference()));
            fieldWrite.setVariable(perfStopFieldCasted.getReference());
        }

        // Util.getShort(apdu.getBuffer(), ISO7816.OFFSET_CDATA)
        CtInvocation<Short> makeShortInvocation;
        try {
            // get ${param}.getBuffer()
            final Method getBufferMethod = APDU.class.getMethod("getBuffer");
            final CtParameter<?> apduParam = processMethod.getParameters().get(0);
            final CtVariableAccess<?> apduParamRead = getFactory().createVariableRead(apduParam.getReference(), false);

            final CtInvocation<?> getBufferInvocation = getFactory()
                    .createInvocation(apduParamRead, getFactory().Method().createReference(getBufferMethod));

            // get javacard.framework.ISO7816.OFFSET_CDATA
            final Class<?> iso7816Class = ISO7816.class;
            final Field offsetCdata = iso7816Class.getField("OFFSET_CDATA");

            final CtFieldRead<Short> offsetFieldRead = getFactory().createFieldRead();
            offsetFieldRead.setTarget(getFactory().createTypeAccess(getFactory().createCtTypeReference(iso7816Class)));
            offsetFieldRead.setVariable(getFactory().Field().createReference(offsetCdata));

            // get javacard.framework.Util.getShort(byte[] bArray, short bOff)
            final Class<?> utilClass = Util.class;
            final Method getShortMethod = utilClass.getMethod("getShort", byte[].class, Short.TYPE);

            // create Util.getShort(apdu.getBuffer(), ISO7816.OFFSET_CDATA)
            makeShortInvocation = getFactory().createInvocation(
                    getFactory().createTypeAccess(getFactory().Class().createReference(utilClass)),
                    getFactory().Method().createReference(getShortMethod),
                    getBufferInvocation, offsetFieldRead);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // PM.m_perfStop = Util.getShort(apdu.getBuffer(), ISO7816.OFFSET_CDATA);
        final CtAssignment<Short, Short> perfStopAssign = getFactory().createAssignment();
        perfStopAssign.setType(shortRef);
        perfStopAssign.setAssigned(fieldWrite);
        perfStopAssign.setAssignment(makeShortInvocation);

        ctCase.addStatement(perfStopAssign);

        // break;
        ctCase.addStatement(getFactory().createBreak());
        return ctCase;
    }

    private CtMethod<Void> getProcessMethod(CtClass<?> cls) {
        final CtMethod<Void> processMethod = cls.filterChildren(
                (CtMethod<Void> m) -> m.getSignature().equals("process(javacard.framework.APDU)") &&
                        m.getType().equals(getFactory().createCtTypeReference(Void.TYPE))).first();
        if (processMethod == null || processMethod.getBody() == null)
            throw new RuntimeException(String.format(
                    "Class %s inherits from %s but does not implement the 'process' method!",
                    cls.getQualifiedName(), cls.getSuperclass().getQualifiedName()));
        return processMethod;
    }

    private void addInsPerfSetStopSwitchCase(CtClass<?> cls, CtField<Byte> insPerfSetStop) {
        final CtMethod<Void> processMethod = getProcessMethod(cls);

        final CtTypeReference<Byte> byteRef = getFactory().createCtTypeReference(Byte.TYPE);
        final List<CtSwitch<Byte>> ctSwitches = processMethod.getElements(
                (CtSwitch<Byte> s) -> s.getSelector().getType().equals(byteRef));

        if (ctSwitches.size() != 1) {
            final String msg = "Please adapt your applet so that it uses a single switch statement" +
                    " to determine the instruction to execute.";

            if (ctSwitches.isEmpty())
                throw new RuntimeException(String.format(
                        "No switch statement with byte selector found in %s.process method!%n%s",
                        cls.getQualifiedName(), msg));

            throw new RuntimeException(String.format(
                    "More switch statements with byte selector found in %s.process method!%n%s%nFound:%n%s%n",
                    cls.getQualifiedName(), msg, ctSwitches.stream().map(CtSwitch::prettyprint)
                            .collect(Collectors.joining(System.lineSeparator()))));
        }

        final CtSwitch<Byte> ctSwitch = ctSwitches.get(0);

        // get case with the same value as JCProfilerUtil.INS_PERF_SETSTOP
        final Optional<CtCase<? super Byte>> maybeExistingPerfStopCase = ctSwitch.getCases().stream().filter(
                c -> {
                    final CtExpression<? super Byte> e = c.getCaseExpression();
                    if (e == null) // default case
                        return false;

                    final CtLiteral<? extends Number> lit = e.partiallyEvaluate();
                    return lit.getValue().byteValue() == JCProfilerUtil.INS_PERF_SETSTOP;
                }).findAny();

        final CtCase<Byte> newPerfStopCase = createPerfStopSwitchCase(processMethod, insPerfSetStop);

        if (maybeExistingPerfStopCase.isPresent()) {
            final CtCase<? super Byte> existingPerfStopCase = maybeExistingPerfStopCase.get();
            log.info("Existing INS_PERF_SETSTOP switch case statement found at {}.", insPerfSetStop.getPosition());

            // cases are equal
            if (existingPerfStopCase.equals(newPerfStopCase))
                return;

            // check that the value in label corresponds to a read of INS_PERF_SETSTOP field
            final CtExpression<?> expr = existingPerfStopCase.getCaseExpression();
            if (!(expr instanceof CtFieldRead) ||
                    !((CtFieldRead<?>) expr).getVariable().equals(insPerfSetStop.getReference()))
                throw new RuntimeException(String.format(
                        "The switch in process method already contains a case for 0x%02X distinct from the " +
                        "INS_PERF_SETSTOP field:%n%s", JCProfilerUtil.INS_PERF_SETSTOP, expr.prettyprint()));

            newPerfStopCase.setParent(ctSwitch);
            throw new RuntimeException(String.format(
                    "The body of the INS_PERF_SETSTOP switch case has unexpected contents:%n%s%nExpected:%n%s%n",
                    existingPerfStopCase.prettyprint(), newPerfStopCase.prettyprint()));
        }

        ctSwitch.addCaseAt(0, newPerfStopCase);
        log.debug("Added INS_PERF_SETSTOP switch case statement to {}.{}.",
                cls.getQualifiedName(), processMethod.getSignature());
    }
}
