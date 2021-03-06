package jcprofiler.instrumentation.processors;

import jcprofiler.util.JCProfilerUtil;
import jcprofiler.args.Args;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.lang.reflect.Method;
import java.util.*;

public class ModifyEntryPointProcessor extends AbstractProcessor<CtClass<?>> {
    final Args args;

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
        final CtField<Byte> insPerfSetStop = addInsPerfSetStopField(cls);
        addInsPerfSetStopSwitchCase(cls, insPerfSetStop);
    }

    private CtField<Byte> addInsPerfSetStopField(final CtClass<?> cls) {
        // private static final byte INS_PERF_STOP = (byte) 0xf5
        final CtTypeReference<Byte> byteType = getFactory().createCtTypeReference(Byte.TYPE);
        CtField<Byte> insPerfSetStop = cls.filterChildren(
                (CtField<Byte> f) -> f.getSimpleName().equals("INS_PERF_SETSTOP")).first();
        if (insPerfSetStop != null) {
            final boolean hasCorrectType = insPerfSetStop.getType().equals(byteType);
            // private or public modifier does NOT make a difference in this case
            final boolean hasCorrectModifiers = insPerfSetStop.hasModifier(ModifierKind.FINAL) &&
                    insPerfSetStop.hasModifier(ModifierKind.STATIC);

            if (hasCorrectType && hasCorrectModifiers) {
                final CtLiteral<Integer> lit = insPerfSetStop.getAssignment().partiallyEvaluate();
                if (lit.getValue() == JCProfilerUtil.INS_PERF_SETSTOP)
                    return insPerfSetStop;
            }

            // TODO: log the deletion with a warning and reason
            insPerfSetStop.delete();
        }

        insPerfSetStop = getFactory().Field().create(
                cls, new HashSet<>(Arrays.asList(ModifierKind.PUBLIC, ModifierKind.FINAL, ModifierKind.STATIC)),
                byteType, "INS_PERF_SETSTOP", getFactory().createLiteral(JCProfilerUtil.INS_PERF_SETSTOP));
        return insPerfSetStop;
    }

    private CtCase<Byte> createPerfStopSwitchCase(final CtMethod<?> processMethod, final CtField<Byte> insPerfSetStop) {
        final CtClass<?> cls = processMethod.getParent(CtClass.class::isInstance);
        final CtCase<Byte> ctCase = getFactory().createCase();

        // case INS_PERF_SETSTOP:
        {
            final CtFieldRead<Byte> fieldAccess = getFactory().createFieldRead();
            fieldAccess.setVariable(insPerfSetStop.getReference());
            ctCase.setCaseExpression(fieldAccess);
        }

        //     PM.m_perfStop = Util.makeShort(apdu.getBuffer()[ISO7816.OFFSET_CDATA],
        //                                    apdu.getBuffer()[(short) (ISO7816.OFFSET_CDATA + 1)])
        final CtAssignment<Short, Short> perfStopAssign = getFactory().createAssignment();
        {
            final CtFieldRead<Short> fieldAccess = getFactory().createFieldRead();
            final CtField<?> perfStopField = cls.getPackage().getType("PM").getField("m_perfStop");
            if (!perfStopField.getType().equals(getFactory().createCtTypeReference(Short.TYPE)))
                throw new RuntimeException(String.format("PM.m_perfStop has type %s which is unexpected!",
                        perfStopField.getType().getQualifiedName()));

            @SuppressWarnings("unchecked") // the runtime check is above
            final CtField<Short> perfStopFieldCasted = (CtField<Short>) cls.getPackage()
                    .getType("PM").getField("m_perfStop");
            fieldAccess.setVariable(perfStopFieldCasted.getReference());
            perfStopAssign.setAssigned(fieldAccess);
        }

        try {
            // get javacard.framework.Util.getShort(byte[] bArray, short bOff)
            final Class<?> utilClass = getEnvironment().getInputClassLoader().loadClass("javacard.framework.Util");
            final Method getShortMethod = utilClass.getMethod("getShort", byte[].class, Short.TYPE);

            final String paramName = processMethod.getParameters().get(0).getSimpleName();
            final CtInvocation<Short> makeShortInvocation = getFactory().createInvocation(
                    getFactory().createTypeAccess(getFactory().Class().createReference(utilClass)),
                    getFactory().Method().createReference(getShortMethod),
                    getFactory().createCodeSnippetExpression(paramName + ".getBuffer()"),
                    getFactory().createCodeSnippetExpression("ISO7816.OFFSET_CDATA"));

            perfStopAssign.setAssignment(makeShortInvocation);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        ctCase.addStatement(perfStopAssign);

        //     break;
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

        // TODO: will this work for switch in a block?
        final CtTypeReference<Byte> byteRef = getFactory().createCtTypeReference(Byte.TYPE);
        final List<CtSwitch<Byte>> ctSwitches = processMethod.getElements(
                (CtSwitch<Byte> s) -> s.getSelector().getType().equals(byteRef));

        final CtCase<Byte> newPerfStopCase = createPerfStopSwitchCase(processMethod, insPerfSetStop);

        if (ctSwitches.size() != 1) {
            // TODO: make this fatal?
            System.out.printf("WARNING: Switch in %s.%s method either not found or more than two are present!%n",
                    cls.getQualifiedName(), processMethod.getSignature());
            System.out.println("Adapt the following piece of code so that your applet correctly responds to the " +
                    "INS_PERF_SETSTOP instruction");
            System.out.println(newPerfStopCase.prettyprint());
            return;
        }

        CtSwitch<Byte> ctSwitch = ctSwitches.get(0);

        // get case with the same value as Utils.INS_PERF_SETSTOP
        final Optional<CtCase<? super Byte>> maybeExistingPerfStopCase = ctSwitch.getCases().stream().filter(
                c -> {
                    final CtExpression<? super Byte> e = c.getCaseExpression();
                    if (e == null) // default case
                        return false;

                    final CtLiteral<Integer> lit = e.partiallyEvaluate();
                    return lit.getValue().byteValue() == JCProfilerUtil.INS_PERF_SETSTOP;
                }).findAny();

        if (maybeExistingPerfStopCase.isPresent()) {
            // cases are equal
            CtCase<? super Byte> existingPerfStopCase = maybeExistingPerfStopCase.get();
            if (existingPerfStopCase.equals(newPerfStopCase))
                return;

            // check that the value in label corresponds to a read of INS_PERF_SETSTOP field
            CtExpression<?> expr = existingPerfStopCase.getCaseExpression();
            if (!(expr instanceof CtFieldRead) ||
                    !((CtFieldRead<?>) expr).getVariable().getSimpleName().equals("INS_PERF_SETSTOP"))
                throw new RuntimeException(String.format(
                        "The switch in process method already contains a case for 0x%02x distinct from the " +
                        "INS_PERF_SETSTOP field: %s", JCProfilerUtil.INS_PERF_SETSTOP, existingPerfStopCase.prettyprint()));

            throw new RuntimeException(String.format(
                    "The body of the INS_PERF_SETSTOP switch case has unexpected contents:%n%s%nExpected:%n%s",
                    existingPerfStopCase.prettyprint(), newPerfStopCase.prettyprint()));
        }

        ctSwitch.addCaseAt(0, newPerfStopCase);
    }
}
