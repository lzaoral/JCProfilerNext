package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;
import java.util.stream.Collectors;

public class InsertTrapProcessor extends AbstractProcessor<CtMethod<?>> {
    private final Args args;

    private CtClass<?> PMC;
    private String trapNamePrefix;
    private int methodCount = 1;
    private int trapCount;

    public InsertTrapProcessor(final Args args) {
        this.args = args;
    }

    @Override
    public boolean isToBeProcessed(final CtMethod<?> method) {
        // TODO: comment
        return method.getBody() != null && !method.getDeclaringType().getSimpleName().matches("PMC?")
                && method.getSimpleName().equals(args.method);
    }

    @Override
    public void process(final CtMethod<?> method) {
        // TODO: this is not ideal solution
        if (PMC == null)
            PMC = method.getDeclaringType().getPackage().getType("PMC");

        trapCount = 1;

        // TODO: what about method overloading?
        trapNamePrefix = getTrapNamePrefix(method);

        if (args.maxTraps * methodCount >= 0xffff)
            throw new RuntimeException(String.format("Class has more than %d (%d)",
                    (int) Math.ceil((float) 0xffff / args.maxTraps), methodCount));

        // TODO: make it possible to instrument constructors?

        addTrapField(trapNamePrefix, String.format("(short) %d", args.maxTraps * methodCount++))
                .addComment(getFactory().createInlineComment(
                        String.format("%s.%s",
                                method.getDeclaringType().getQualifiedName(),
                                method.getSignature())));

        processBody(method.getBody());
    }

    private String getTrapNamePrefix(final CtMethod<?> method) {
        // simple name should be enough as all classes should be in the same package
        // TODO: what about nested classes?
        final String prefix = String.format(
                "TRAP_%s_%s", method.getDeclaringType().getSimpleName(), method.getSignature());

        // list may not be exhaustive
        return prefix.replace('.', '_') // used in qualified types
                .replace(",", "__") // args delimiter
                .replace("(", "_argb_")
                .replace(")", "_arge")
                .replace("<", "_genb_")
                .replace(">", "_gene_")
                .replace("[]", "arr"); // used in arrays
    }

    private void processBody(final CtStatementList block) {
        // remove empty stand-alone code blocks to get a consistent behaviour in tests,
        // SPOON does it automatically only sometimes
        final List<CtStatement> forDeletion = block.getStatements().stream()
                .filter(x -> x instanceof CtBlock && ((CtBlock<?>) x).getStatements().isEmpty())
                .collect(Collectors.toList());
        forDeletion.forEach(CtStatement::delete);

        // copy is needed as we modify the collection
        final List<CtStatement> statements = block.getStatements().stream()
                // skip comments (SPOON classifies comments as statements)
                // e.g. JCMathLib/JCMathLib/src/opencrypto/jcmathlib/OCUnitTests.java:198
                .filter(x -> !(x instanceof CtComment))
                .collect(Collectors.toList());

        forDeletion.forEach(CtElement::delete);

        for (final CtStatement statement : statements) {
            insertTrapCheck(statement, INSERT.BEFORE);

            // skip insertion after ISOException calls as all such statements are unreachable
            if (isISOException(statement))
                return;

            // CtTry is an instance of CtBodyHolder, but we want to process catch and final blocks as well
            if (statement instanceof CtTry) {
                final CtTry t = (CtTry) statement;
                processBody(t.getBody());
                t.getCatchers().forEach(c -> processBody(c.getBody()));
                if (t.getFinalizer() != null)
                    processBody(t.getFinalizer());
            } else if (statement instanceof CtBodyHolder) {
                final CtBodyHolder b = (CtBodyHolder) statement;
                if (b.getBody() != null)
                    processBody((CtBlock<?>) b.getBody());
            } else if (statement instanceof CtIf) {
                final CtIf i = (CtIf) statement;
                if (i.getThenStatement() != null)
                    processBody(i.getThenStatement());
                if (i.getElseStatement() != null)
                    processBody(i.getElseStatement());
            } else if (statement instanceof CtBlock) {
                processBody((CtBlock<?>) statement);
            } else if (statement instanceof CtSwitch) {
                ((CtSwitch<?>) statement).getCases().forEach(this::processBody);
            }
        }

        // TODO: insert trap in an empty block?
        if (!statements.isEmpty()) {
            final CtStatement last = statements.get(statements.size() - 1);
            if (isTerminator(last))
                return;

            insertTrapCheck(last, INSERT.AFTER);
        }
    }

    private boolean isISOException(CtStatement statement) {
        // can be the last statement a commentary?
        if (!(statement instanceof CtInvocation))
            return false;

        final CtInvocation<?> call = (CtInvocation<?>) statement;
        if (!(call.getTarget() instanceof CtTypeAccess))
            return false;

        final CtTypeAccess<?> classAccess = (CtTypeAccess<?>) call.getTarget();

        // check for void javacard.framework.ISOException.throwIt(short) call
        return classAccess.getAccessedType().getQualifiedName().equals("javacard.framework.ISOException")
                && call.getExecutable().getSignature().equals("throwIt(short)")
                && call.getType().equals(getFactory().createCtTypeReference(Void.TYPE));
    }

    /***
     * A statement is a terminator iff any statement inserted after it will be unreachable, e.g.:
     *     1. the statement is a jump
     *     2. the last statement is not an expression and all its held block statements are terminators, e.g.:
     *
     * if (cond) {
     *     return true;
     * } else {
     *     return false;
     * }
     * PM.check(...) <- this would be unreachable!
     */
    private boolean isTerminator(final CtStatement statement) {
        if (statement instanceof CtCFlowBreak || isISOException(statement))
            return true;

        // CtTry is an instance of CtBodyHolder, but we want to process catch and final blocks as well
        if (statement instanceof CtTry) {
            final CtTry t = (CtTry) statement;
            return isTerminator(t.getBody()) && t.getCatchers().stream().allMatch(c -> isTerminator(c.getBody()))
                    && (t.getFinalizer() == null || isTerminator(t.getFinalizer()));
        }

        if (statement instanceof CtBodyHolder)
            return isTerminator(((CtBodyHolder) statement).getBody());

        if (statement instanceof CtStatementList) {
            final CtStatementList stl = (CtStatementList) statement;
            return !stl.getStatements().isEmpty() && isTerminator(stl.getLastStatement());
        }

        if (statement instanceof CtIf) {
            final CtIf i = (CtIf) statement;
            return isTerminator(i.getThenStatement()) && isTerminator(i.getElseStatement());
        }

        if (statement instanceof CtSwitch)
            return ((CtSwitch<?>) statement).getCases().stream().allMatch(this::isTerminator);

        return false;
    }

    private void insertTrapCheck(final CtStatement statement, INSERT where) {
        if (args.maxTraps * methodCount + trapCount >= 0xffff) {
            final CtMethod<?> parentMethod = statement.getParent(CtMethod.class::isInstance);
            throw new RuntimeException(
                    String.format("Method %s.%s needs more than %d traps",
                            parentMethod.getDeclaringType().getQualifiedName(),
                            parentMethod.getSignature(),
                            args.maxTraps));
        }

        final String trapName = String.format("%s_%d", trapNamePrefix, trapCount);
        final String initializer = String.format("(short) (%s + %d)", trapNamePrefix, trapCount++);

        final CtField<Short> trapField = addTrapField(trapName, initializer);
        final CtFieldRead<Short> trapFieldRead = getFactory().createFieldRead();
        trapFieldRead.setTarget(getFactory().createTypeAccess(trapField.getDeclaringType().getReference()));
        trapFieldRead.setVariable(trapField.getReference());

        final CtClass<?> pm = getFactory().getModel()
                .getElements((CtClass<?> c) -> c.getSimpleName().equals("PM")).get(0);
        final CtInvocation<?> pmCall = getFactory().createInvocation(
                getFactory().createTypeAccess(pm.getReference()),
                pm.getMethod("check", getFactory().createCtTypeReference(Short.TYPE)).getReference(),
                trapFieldRead);

        // TODO: check ret val
        if (where == INSERT.AFTER)
            statement.insertAfter(pmCall);
        else
            statement.insertBefore(pmCall);
    }

    private CtField<Short> addTrapField(String trapFieldName, String initializer) {
        final CtTypeReference<Short> shortType = getFactory().createCtTypeReference(Short.TYPE);
        final CtField<Short> trapField = getFactory().createCtField(
                trapFieldName, shortType, initializer,
                ModifierKind.PUBLIC, ModifierKind.STATIC, ModifierKind.FINAL);

        PMC.addField(trapField);

        return trapField;
    }

    private enum INSERT {
        AFTER,
        BEFORE
    }
}
