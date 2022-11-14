package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InsertTimeMeasurementsProcessor extends AbstractInsertMeasurementsProcessor {
    private final Map<String, DescriptiveStatistics> statisticsMap;

    public InsertTimeMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements,
                                           final Map<String, DescriptiveStatistics> statisticsMap) {
        super(args, measurements);
        this.statisticsMap = statisticsMap;
    }

    @Override
    protected String getCommentString(String fieldName) {
        final List<Long> values = measurements.get(fieldName);
        final DescriptiveStatistics statistics = statisticsMap.get(fieldName);
        final String unitSymbol = args.timeUnit.toString();
        final long unreachableCount = values.stream().filter(Objects::isNull).count();

        return String.format(
                "Mean: %.2f %s, Std Dev: %.2f %s, Max: %d %s, Min: %d %s, Unreachable: %d/%d, %d outliers skipped",
                statistics.getMean(), unitSymbol,
                statistics.getStandardDeviation(), unitSymbol,
                (int) statistics.getMax(), unitSymbol,
                (int) statistics.getMin(), unitSymbol,
                unreachableCount, values.size(),
                values.size() - unreachableCount - statistics.getN());
    }
}
