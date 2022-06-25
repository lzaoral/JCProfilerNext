package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InsertTimesProcessor extends AbstractProcessor<CtInvocation<Void>> {
    private final Args args;
    private final String cardName;
    private final Map<String, List<Long>> measurements;

    public InsertTimesProcessor(Args args, String cardName, Map<String, List<Long>> measurements) {
        this.args = args;
        this.cardName = cardName;
        this.measurements = measurements;
    }

    @Override
    public boolean isToBeProcessed(CtInvocation<Void> statement) {
        final CtExecutableReference<?> executable = statement.getExecutable();
        return executable.getDeclaringType().getSimpleName().equals("PM")
                && executable.getSignature().equals("check(short)")
                && executable.getType().equals(getFactory().createCtTypeReference(Void.TYPE));
    }

    @Override
    public void process(CtInvocation<Void> invocation) {
        final CtFieldRead<?> trapFieldRead = (CtFieldRead<?>) invocation.getArguments().get(0);
        final List<Long> values = measurements.get(trapFieldRead.getVariable().getSimpleName());

        final CtComment comment = getFactory().createInlineComment(
                // TODO: use IntSummaryStatistics if args.repeat_count is too big?
                String.format("%s: %s", cardName, values.stream()
                        .map(v -> v != null ? v + "ms" : "unreachable")
                        .collect(Collectors.joining(", "))));

        invocation.insertAfter(comment);
        invocation.delete();
    }
}
