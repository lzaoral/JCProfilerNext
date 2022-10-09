package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;

import java.util.List;
import java.util.Map;

public class InsertMemoryMeasurementsProcessor extends AbstractInsertMeasurementsProcessor {
    public InsertMemoryMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements) {
        super(args, measurements);
    }

    @Override
    protected String getCommentString(String fieldName) {
        final List<Long> values = measurements.get(fieldName);

        // trap was unreachable
        if (values.contains(null))
            return "Unreachable";

        return String.format(
                "Free Transient Deselect: %d B, Free Transient Reset: %d B, Free Persistent: %d B",
                values.get(0), values.get(1), values.get(2));
    }
}
