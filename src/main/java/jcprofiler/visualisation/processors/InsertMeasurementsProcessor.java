package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InsertMeasurementsProcessor extends AbstractProcessor<CtInvocation<Void>> {
    private final Args args;
    private final Map<String, List<Long>> measurements;
    private final Map<String, DescriptiveStatistics> statisticsMap;

    private static final Logger log = LoggerFactory.getLogger(InsertMeasurementsProcessor.class);

    public InsertMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements,
                                       final Map<String, DescriptiveStatistics> statisticsMap) {
        this.args = args;
        this.measurements = measurements;
        this.statisticsMap = statisticsMap;
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
        final String fieldName = trapFieldRead.getVariable().getSimpleName();

        // skip if this trap was not measured
        if (!measurements.containsKey(fieldName))
            return;

        final List<Long> values = measurements.get(fieldName);
        final DescriptiveStatistics statistics = statisticsMap.get(fieldName);
        final String unitSymbol = JCProfilerUtil.getTimeUnitSymbol(args.timeUnit);
        final long unreachableCount = values.stream().filter(Objects::isNull).count();

        final String commentContents = String.format(
                "Mean: %.2f %s, Std Dev: %.2f %s, Max: %d %s, Min: %d %s, Unreachable: %d/%d, %d outliers skipped",
                statistics.getMean(), unitSymbol,
                statistics.getStandardDeviation(), unitSymbol,
                (int) statistics.getMax(), unitSymbol,
                (int) statistics.getMin(), unitSymbol,
                unreachableCount, values.size(),
                values.size() - unreachableCount - statistics.getN());

        log.debug("Inserting comment with measurements at {}.", invocation.getPosition());
        invocation.replace(getFactory().createInlineComment(commentContents));
    }
}
