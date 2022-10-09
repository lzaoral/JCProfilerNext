package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;

import java.util.List;
import java.util.Map;

public abstract class AbstractInsertMeasurementsProcessor extends AbstractProcessor<CtInvocation<Void>> {
    protected final Args args;
    protected final Map<String, List<Long>> measurements;

    private static final Logger log = LoggerFactory.getLogger(AbstractInsertMeasurementsProcessor.class);

    protected AbstractInsertMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements) {
        this.args = args;
        this.measurements = measurements;
    }

    @Override
    public boolean isToBeProcessed(final CtInvocation<Void> statement) {
        final CtExecutableReference<?> executable = statement.getExecutable();
        return executable.getDeclaringType().getSimpleName().equals("PM")
                && executable.getSignature().equals("check(short)")
                && executable.getType().equals(getFactory().createCtTypeReference(Void.TYPE))
                && executable.isStatic();
    }

    protected abstract String getCommentString(final String fieldName);

    @Override
    public void process(final CtInvocation<Void> invocation) {
        final CtFieldRead<?> trapFieldRead = (CtFieldRead<?>) invocation.getArguments().get(0);
        final String fieldName = trapFieldRead.getVariable().getSimpleName();

        // skip if this trap was not measured
        if (!measurements.containsKey(fieldName))
            return;

        final String commentContents = getCommentString(fieldName);

        log.debug("Inserting comment with measurements at {}.", invocation.getPosition());
        invocation.replace(getFactory().createInlineComment(commentContents));
    }
}
