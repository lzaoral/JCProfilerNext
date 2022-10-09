package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.visualisation.processors.AbstractInsertMeasurementsProcessor;

import jcprofiler.visualisation.processors.InsertTimeMeasurementsProcessor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.velocity.VelocityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.SpoonAPI;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimeVisualiser extends AbstractVisualiser {
    private final Map<String, List<Long>> filteredMeasurements = new LinkedHashMap<>();
    private final Map<String, DescriptiveStatistics> filteredStatistics = new LinkedHashMap<>();
    private final List<Double> heatmapValues = new ArrayList<>();

    private static final Logger log = LoggerFactory.getLogger(TimeVisualiser.class);

    public TimeVisualiser(final Args args, final SpoonAPI spoon) {
        super(args, spoon);
    }

    @Override
    public void loadAndProcessMeasurements() {
        super.loadAndProcessMeasurements();
        filterOutliers();
        prepareHeatmap();
    }

    private void filterOutliers() {
        log.info("Filtering outliers from the loaded measurements.");
        measurements.forEach((k, v) -> {
            final DescriptiveStatistics ds = new DescriptiveStatistics();
            v.stream().filter(Objects::nonNull).map(Long::doubleValue).forEach(ds::addValue);

            if (ds.getN() == 0) {
                filteredMeasurements.put(k, new ArrayList<>());
                filteredStatistics.put(k, ds);
                return;
            }

            final long n = ds.getN();
            final double mean = ds.getMean();
            final double standardDeviation = ds.getStandardDeviation();

            final List<Long> filteredValues = v.stream().map(l -> {
                if (l == null || n == 1)
                    return l;

                // replace outliers with null
                double zValue = Math.abs(l - mean) / standardDeviation;
                if (inputs.size() < 100 && zValue <= 3.)
                    return l;

                if (inputs.size() < 100000 && zValue <= 2.)
                    return l;

                return zValue <= 1. ? l : null;
            }).collect(Collectors.toList());

            filteredMeasurements.put(k, filteredValues);

            final DescriptiveStatistics filteredDs = new DescriptiveStatistics();
            filteredValues.stream().filter(Objects::nonNull).map(Long::doubleValue).forEach(filteredDs::addValue);
            filteredStatistics.put(k, filteredDs);
        });
    }

    private void prepareHeatmap() {
        // prepare values for the heatMap
        sourceCode.forEach(s -> {
            if (!s.contains("PM.check(PMC.TRAP")) {
                heatmapValues.add(null);
                return;
            }

            final int beginPos = s.indexOf('(') + 1 + "PMC.".length();
            final int endPos = s.indexOf(')');
            final DescriptiveStatistics ds = filteredStatistics.get(s.substring(beginPos, endPos));

            heatmapValues.add(ds == null ? Double.NaN // trap is completely unreachable
                                         : Math.round(ds.getMean() * 100.) / 100.);
        });
    }

    @Override
    protected Long convertValues(final String input) {
        if (input.isEmpty())
            return null;

        long nanos = Long.parseLong(input);
        switch (args.timeUnit) {
            case nano:
                return nanos; // noop
            case micro:
                return TimeUnit.NANOSECONDS.toMicros(nanos);
            case milli:
                return TimeUnit.NANOSECONDS.toMillis(nanos);
            case sec:
                return TimeUnit.NANOSECONDS.toSeconds(nanos);
            default:
                throw new RuntimeException("Unreachable statement reached!");
        }
    }

    @Override
    public AbstractInsertMeasurementsProcessor getInsertMeasurementsProcessor() {
        return new InsertTimeMeasurementsProcessor(args, measurements, filteredStatistics);
    }

    @Override
    public void prepareVelocityContext(final VelocityContext context) {
        context.put("filteredMeasurements", filteredMeasurements);
        context.put("heatmapValues", heatmapValues);
        context.put("roundCount", measurements.values().iterator().next().size());
    }
}