package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InsertMeasurementsProcessor extends AbstractProcessor<CtInvocation<Void>> {
    private final Args args;
    private final String atr;
    private final Map<String, List<Long>> measurements;

    private static final Logger log = LoggerFactory.getLogger(InsertMeasurementsProcessor.class);

    public InsertMeasurementsProcessor(final Args args, final String atr, final Map<String, List<Long>> measurements) {
        this.args = args;
        this.atr = atr;
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

    @Override
    public void process(final CtInvocation<Void> invocation) {
        final CtFieldRead<?> trapFieldRead = (CtFieldRead<?>) invocation.getArguments().get(0);
        final List<Long> values = measurements.get(trapFieldRead.getVariable().getSimpleName());

        // skip if this trap was not measured
        if (values == null)
            return;

        final CtComment comment = getFactory().createInlineComment(
                // TODO: use IntSummaryStatistics if args.repeat_count is too big?
                String.format("ATR %s: %s", atr, values.stream()
                        .map(v -> v != null ? v + JCProfilerUtil.getTimeUnitSymbol(args.timeUnit) : "unreachable")
                        .collect(Collectors.joining(", "))));

        log.debug("Inserting comment with measurements at {}.", invocation.getPosition());

        invocation.insertAfter(comment);
        invocation.delete();
    }
}
