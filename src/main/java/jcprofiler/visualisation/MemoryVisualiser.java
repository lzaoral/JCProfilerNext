package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.visualisation.processors.AbstractInsertMeasurementsProcessor;
import jcprofiler.visualisation.processors.InsertMemoryMeasurementsProcessor;

import org.apache.velocity.VelocityContext;

import spoon.SpoonAPI;

import java.util.List;
import java.util.stream.IntStream;

public class MemoryVisualiser extends AbstractVisualiser {
    public MemoryVisualiser(final Args args, final SpoonAPI spoon) {
        super(args, spoon);
    }

    @Override
    public void loadAndProcessMeasurements() {
        super.loadAndProcessMeasurements();
        prepareHeatmap();
    }

    @Override
    public AbstractInsertMeasurementsProcessor getInsertMeasurementsProcessor() {
        return new InsertMemoryMeasurementsProcessor(args, measurements);
    }

    private void prepareHeatmap() {
        String prevActualTrap = null;
        // prepare values for the heatMap
        for (final String line : sourceCode) {
            if (!line.contains("PM.check(PMC.TRAP")) {
                heatmapValues.add(null);
                continue;
            }

            final int beginPos = line.indexOf('(') + 1 + "PMC.".length();
            final int endPos = line.indexOf(')');
            final String currentTrap = line.substring(beginPos, endPos);

            // unreachable trap or first reachable trap
            if (measurements.get(currentTrap).contains(null) || prevActualTrap == null) {
                heatmapValues.add(0.0);

                // first reachable processed trap
                if (prevActualTrap == null)
                    prevActualTrap = currentTrap;

                continue;
            }

            final List<Long> prev = measurements.get(prevActualTrap);
            final List<Long> current = measurements.get(currentTrap);

            // get the biggest difference in available memory
            heatmapValues.add(IntStream.range(0, 3).mapToDouble(i -> prev.get(i) - current.get(i)).max().orElse(0.0));

            prevActualTrap = currentTrap;
        }
    }

    @Override
    public void prepareVelocityContext(final VelocityContext context) {
        context.put("measureUnit", "B");
    }
}
