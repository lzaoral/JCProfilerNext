// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class for annotation of profiled sources in time mode
 */
public class InsertTimeMeasurementsProcessor extends AbstractInsertMeasurementsProcessor {
    private final Map<String, DescriptiveStatistics> statisticsMap;

    /**
     * Constructs the {@link InsertTimeMeasurementsProcessor} class.
     *
     * @param args          object with commandline arguments
     * @param measurements  map between traps and measurements
     * @param statisticsMap map between traps and measurement statistics
     */
    public InsertTimeMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements,
                                           final Map<String, DescriptiveStatistics> statisticsMap) {
        super(args, measurements);
        this.statisticsMap = statisticsMap;
    }

    /**
     * Returns a commentary contents to replace the {@code PM#check(short)} call
     * for given performance trap.
     *
     * @param  fieldName name of the performance trap field
     * @return           comment annotation contents
     */
    @Override
    protected String getCommentString(String fieldName) {
        final List<Long> values = measurements.get(fieldName);
        final DescriptiveStatistics statistics = statisticsMap.get(fieldName);
        final String unitSymbol = args.timeUnit.prettyPrint();
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
