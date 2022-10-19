package jcprofiler.instrumentation.processors;

import javacard.framework.APDU;
import javacard.framework.ISO7816;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractModifyEntryPointProcessor extends AbstractProfilerProcessor<CtClass<?>> {
    private static final Logger log = LoggerFactory.getLogger(AbstractModifyEntryPointProcessor.class);

    protected AbstractModifyEntryPointProcessor(final Args args) {
        super(args);
    }

    @Override
    public boolean isToBeProcessed(final CtClass<?> cls) {
        // isEntryPoint && (!entryPointArg.isEmpty() => cls.SimpleName == entryPointArg)
        return JCProfilerUtil.isTypeEntryPoint(cls) &&
                (args.entryPoint.isEmpty() || cls.getQualifiedName().equals(args.entryPoint));
    }

    private CtMethod<Void> getProcessMethod(final CtClass<?> cls) {
        CtMethod<Void> processMethod;
        CtTypeReference<?> clsRef = cls.getReference();

        do {
            processMethod = clsRef.getTypeDeclaration().filterChildren(
                    (CtMethod<Void> m) -> m.getSignature().equals("process(javacard.framework.APDU)") &&
                            m.getType().equals(getFactory().createCtTypeReference(Void.TYPE)) &&
                            !m.isAbstract() && m.getBody() != null).first();
            clsRef = clsRef.getSuperclass();
        } while (clsRef != null && processMethod == null);

        if (processMethod == null)
            throw new RuntimeException(String.format(
                    "Class %s inherits from %s but does not implement the 'process(javacard.framework.APDU)' method!",
                    cls.getQualifiedName(), cls.getSuperclass().getQualifiedName()));
        return processMethod;
    }

    protected void process(final CtClass<?> cls, final String fieldName) {
        log.info("Instrumenting entry point class {}.", cls.getQualifiedName());
        final CtMethod<Void> processMethod = getProcessMethod(cls);
        final CtField<Byte> insPerfField = addInsField(fieldName, processMethod);
        createInsHandler(processMethod, insPerfField);
    }

    private CtField<Byte> addInsField(final String name, final CtMethod<Void> processMethod) {
        final CtType<?> declaringCls = processMethod.getDeclaringType();

        // private static final byte ${name} = (byte) ${JCProfilerUtil.INS_PERF_HANDLER}
        final CtTypeReference<Byte> byteRef = getFactory().createCtTypeReference(Byte.TYPE);

        Optional<CtField<?>> existingInsPerfSetStop = declaringCls.getFields().stream().filter(
                f -> f.getSimpleName().equals(name)).findFirst();
        if (existingInsPerfSetStop.isPresent()) {
            final CtField<?> insPerfSetStop = existingInsPerfSetStop.get();
            log.info("Existing {} field found at {}.", name, insPerfSetStop.getPosition());

            if (!insPerfSetStop.getType().equals(byteRef))
                throw new RuntimeException(String.format(
                        "Existing %s field has type %s! Expected: %s",
                        name, insPerfSetStop.getType().getQualifiedName(), Byte.TYPE.getTypeName()));

            @SuppressWarnings("unchecked") // the runtime check is above
            final CtField<Byte> insPerfSetStopCasted = (CtField<Byte>) insPerfSetStop;

            // private or public modifier does NOT make a difference in this case
            if (!insPerfSetStopCasted.hasModifier(ModifierKind.FINAL) ||
                    !insPerfSetStopCasted.hasModifier(ModifierKind.STATIC))
                throw new RuntimeException(String.format(
                        "Existing %s field is not static and final! Got: %s", name,
                        // Set<ModifierKind> does not have a stable ordering
                        insPerfSetStopCasted.getModifiers().stream().sorted().collect(Collectors.toList())));

            final CtLiteral<? extends Number> lit = insPerfSetStopCasted.getAssignment().partiallyEvaluate();
            if (lit.getValue().byteValue() != JCProfilerUtil.INS_PERF_HANDLER)
                throw new RuntimeException(String.format(
                        "Existing %s field has %s as initializer! Expected: (byte) 0x%02x",
                        name, insPerfSetStopCasted.getAssignment().prettyprint(), JCProfilerUtil.INS_PERF_HANDLER));

            return insPerfSetStopCasted;
        }

        // create new ${name} field
        final CtField<Byte> insPerfSetStop = getFactory().createCtField(
                name, byteRef, "", ModifierKind.PUBLIC, ModifierKind.FINAL, ModifierKind.STATIC);

        // create and set the initializer
        final CtLiteral<Integer> initializer = getFactory().createLiteral(Byte.toUnsignedInt(JCProfilerUtil.INS_PERF_HANDLER));
        initializer.setBase(LiteralBase.HEXADECIMAL);
        initializer.addTypeCast(byteRef);

        @SuppressWarnings("unchecked")
        // Unfortunately, this is the best solution we have since SPOON does not reflect type casts in type parameters.
        final CtLiteral<Byte> initializerCasted = (CtLiteral<Byte>) (Object) initializer;
        insPerfSetStop.setAssignment(initializerCasted);

        // insert the field
        declaringCls.addField(0, insPerfSetStop);
        log.debug("Added {} field to {}.", name, declaringCls.getQualifiedName());
        return insPerfSetStop;
    }

    private void createInsHandler(final CtMethod<Void> processMethod, final CtField<Byte> insPerfField) {
        // ${param}
        @SuppressWarnings("unchecked") // the runtime check was done in getProcessMethod
        final CtParameter<APDU> apduParam = (CtParameter<APDU>) processMethod.getParameters().get(0);
        final CtVariableRead<APDU> apduParamRead = (CtVariableRead<APDU>) getFactory()
                .createVariableRead(apduParam.getReference(), false);

        // ${param}.getBuffer()[ISO7816.OFFSET_INS]
        CtArrayRead<Byte> apduBufferRead;
        try {
            // ${param}.getBuffer()
            final Method getBufferMethod = APDU.class.getMethod("getBuffer");
            final CtInvocation<Byte> apduBufferInvocation = getFactory().createInvocation(
                    apduParamRead, getFactory().Method().createReference(getBufferMethod));

            // get javacard.framework.ISO7816.OFFSET_INS
            final Class<?> iso7816Class = ISO7816.class;
            final Field offsetIns = iso7816Class.getField("OFFSET_INS");

            final CtFieldRead<Integer> offsetFieldRead = getFactory().createFieldRead();
            offsetFieldRead.setTarget(getFactory().createTypeAccess(getFactory().createCtTypeReference(iso7816Class)));
            offsetFieldRead.setVariable(getFactory().Field().createReference(offsetIns));

            // ${param}.getBuffer()[ISO7816.OFFSET_INS]
            apduBufferRead = getFactory().createArrayRead();
            apduBufferRead.setIndexExpression(offsetFieldRead);
            apduBufferRead.setTarget(apduBufferInvocation);
            apduBufferRead.setType(getFactory().createCtTypeReference(Byte.TYPE));
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // ${param}.getBuffer()[ISO7816.OFFSET_INS] == ${insPerfField}
        CtBinaryOperator<Boolean> insEqCond;
        {
            final CtTypeReference<?> processMethodDeclTypeRed = processMethod.getDeclaringType().getReference();
            final CtTypeReference<?> insPerfSetStopDeclTypeRef = insPerfField.getDeclaringType().getReference();

            final CtFieldRead<Byte> insPerfSetStopFieldRead = getFactory().createFieldRead();
            insPerfSetStopFieldRead.setTarget(getFactory().createTypeAccess(insPerfSetStopDeclTypeRef,
                    /* implicit */ processMethodDeclTypeRed.equals(insPerfSetStopDeclTypeRef)));
            insPerfSetStopFieldRead.setVariable(getFactory().Field().createReference(insPerfField));

            insEqCond = getFactory().createBinaryOperator(apduBufferRead, insPerfSetStopFieldRead, BinaryOperatorKind.EQ);
            insEqCond.setType(getFactory().createCtTypeReference(Boolean.TYPE));
        }

        // if (${param}.getBuffer()[ISO7816.OFFSET_INS] == ${insPerfField}) {
        //     ${createInsHandlerBody}
        // }
        final CtBlock<Void> insHandlerBody = createInsHandlerBody(apduParamRead);
        if (!(insHandlerBody.getLastStatement() instanceof CtReturn))
            throw new RuntimeException("The handler body must end with a return statement!");

        final CtIf ifStatement = getFactory().createIf();
        ifStatement.setCondition(insEqCond);
        ifStatement.setThenStatement(insHandlerBody);

        final String fieldName = insPerfField.getSimpleName();

        final CtBlock<Void> processMethodBody = processMethod.getBody();
        Optional<CtIf> maybeExistingIfStatement = processMethodBody.getStatements().stream()
                .filter(ifStatement::equals).map(CtIf.class::cast).findAny();
        if (maybeExistingIfStatement.isPresent()) {
            log.info("Existing {} handler found at {}.", fieldName, maybeExistingIfStatement.get().getPosition());
            return;
        }

        maybeExistingIfStatement = processMethodBody.getStatements().stream().filter(CtIf.class::isInstance)
                .map(CtIf.class::cast).filter(i -> i.getCondition().equals(ifStatement.getCondition())).findAny();
        if (maybeExistingIfStatement.isPresent()) {
            final CtIf existingIf = maybeExistingIfStatement.get();
            log.info("Existing {} handler found at {}.", fieldName, existingIf.getPosition());
            throw new RuntimeException(String.format(
                    "The body of the %s handle has unexpected contents:%n%s%nExpected:%n%s%n",
                    fieldName, existingIf.getThenStatement().prettyprint(),
                    ifStatement.getThenStatement().prettyprint()));
        }

        processMethodBody.addStatement(0, ifStatement);
        log.debug("Added {} handler as the first statement to {}.",
                fieldName, JCProfilerUtil.getFullSignature(processMethod));
    }

    protected abstract CtBlock<Void> createInsHandlerBody(final CtVariableRead<APDU> apdu);
}
