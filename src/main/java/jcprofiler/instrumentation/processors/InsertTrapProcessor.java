package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;
import java.util.stream.Collectors;

public class InsertTrapProcessor extends AbstractProcessor<CtMethod<?>> {
    private static final Logger log = LoggerFactory.getLogger(InsertTrapProcessor.class);

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
        final String actualMethodSignature = method.getDeclaringType().getQualifiedName() + "." + method.getSignature();
        return actualMethodSignature.equals(args.method);
    }

    @Override
    public void process(final CtMethod<?> method) {
        log.info("Instrumenting {}.{}.", method.getDeclaringType().getQualifiedName(), method.getSignature());

        // TODO: this is not ideal solution
        if (PMC == null)
            PMC = method.getDeclaringType().getPackage().getType("PMC");

        trapCount = 1;
        trapNamePrefix = JCProfilerUtil.getTrapNamePrefix(method);

        if (args.maxTraps * methodCount >= 0xffff)
            throw new RuntimeException(String.format("Class has more than %d (%d)",
                    (int) Math.ceil((float) 0xffff / args.maxTraps), methodCount));

        // TODO: make it possible to instrument constructors?

        log.debug("Adding {} trap.", trapNamePrefix);
        addTrapField(trapNamePrefix, "(short) " + (args.maxTraps * methodCount++))
                .addComment(getFactory().createInlineComment(
                        method.getDeclaringType().getQualifiedName() + "." + method.getSignature()));

        final CtBlock<?> methodBody = method.getBody();
        if (isEmptyBlock(methodBody)) {
            insertTrapCheck(methodBody);
            return;
        }

        processBody(methodBody);
    }

    private void processBody(final CtStatementList block) {
        // copy is needed as we modify the collection
        final List<CtStatement> statements = block.getStatements().stream()
                // skip comments (SPOON classifies comments as statements)
                // e.g. JCMathLib/JCMathLib/src/opencrypto/jcmathlib/OCUnitTests.java:198
                .filter(x -> !(x instanceof CtComment) && !isEmptyBlock(x))
                .collect(Collectors.toList());

        if (!statements.isEmpty())
            // always insert first trap at the beginning of the ORIGINAL block
            insertTrapCheck(block.getStatement(0), Insert.BEFORE);

        for (final CtStatement statement : statements) {
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

            final CtStatement last = statements.get(statements.size() - 1);
            if (statement == last && isTerminator(statement))
                return;

            insertTrapCheck(statement, Insert.AFTER);
        }
    }

    private boolean isEmptyBlock(final CtStatement statement) {
        if (!(statement instanceof CtBlock))
            return false;

        final List<CtStatement> blockContents = ((CtBlock<?>) statement).getStatements();
        return blockContents.isEmpty() || blockContents.stream().allMatch(s -> isEmptyBlock(s) || s instanceof CtComment);
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
     * Statement is a terminator iff any statement inserted after in the corresponding block will be unreachable, e.g.:
     *     1. the statement is a break or continue,
     *     2. the statement is a complete terminator.
     * <p>
     * for (int i = 1; i < 100; i++) {
     *     if (cond) {
     *         return true;
     *         PM.check(...) <- this would be unreachable!
     *     }
     *     break;
     *     PM.check(...) <- this would be unreachable!
     * }
     * PM.check(...) <- this is still reachable!
     */
    private boolean isTerminator(final CtStatement statement) {
        return statement instanceof CtCFlowBreak || isCompleteTerminator(statement);
    }

    /***
     * Statement is a full terminator iff any statement inserted after it into a given method will be unreachable, e.g.:
     *     1. the statement is a jump out of the method,
     *     2. the last statement is not an expression and all its held block statements are full terminators, e.g.:
     * <p>
     * if (cond) {
     *     return true;
     * } else {
     *     return false;
     * }
     * PM.check(...) <- this would be unreachable!
     */
    private boolean isCompleteTerminator(final CtStatement statement) {
        if (statement instanceof CtReturn || statement instanceof CtThrow || isISOException(statement))
            return true;

        // CtTry is an instance of CtBodyHolder, but we want to process catch and final blocks as well
        if (statement instanceof CtTry) {
            final CtTry t = (CtTry) statement;
            return isCompleteTerminator(t.getBody()) &&
                    t.getCatchers().stream().allMatch(c -> isCompleteTerminator(c.getBody())) &&
                    (t.getFinalizer() == null || isCompleteTerminator(t.getFinalizer()));
        }

        if (statement instanceof CtBodyHolder)
            return isCompleteTerminator(((CtBodyHolder) statement).getBody());

        if (statement instanceof CtStatementList) {
            final CtStatementList stl = (CtStatementList) statement;
            return !stl.getStatements().isEmpty() && isCompleteTerminator(stl.getLastStatement());
        }

        if (statement instanceof CtIf) {
            final CtIf i = (CtIf) statement;
            return isCompleteTerminator(i.getThenStatement()) && isCompleteTerminator(i.getElseStatement());
        }

        if (statement instanceof CtSwitch)
            return ((CtSwitch<?>) statement).getCases().stream().allMatch(this::isCompleteTerminator);

        return false;
    }

    private CtInvocation<?> createPmCall(final CtElement element, final Insert where) {
        if (args.maxTraps * methodCount + trapCount >= 0xffff) {
            final CtMethod<?> parentMethod = element.getParent(CtMethod.class::isInstance);
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

        log.debug("Adding {} trap {} {}.",
                trapField.getSimpleName(), where.toString().toLowerCase(), element.getPosition());

        return pmCall;
    }

    private void insertTrapCheck(final CtStatementList block) {
        final CtInvocation<?> pmCall = createPmCall(block, Insert.INTO);
        if (block.getStatements().isEmpty())
            block.addStatement(pmCall);
        else
            block.getStatement(0).insertBefore(pmCall);
    }

    private void insertTrapCheck(final CtStatement statement, final Insert where) {
        final CtInvocation<?> pmCall = createPmCall(statement, where);

        if (where == Insert.AFTER)
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

    private enum Insert {
        AFTER,
        BEFORE,
        INTO
    }
}
