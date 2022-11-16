package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;

import java.util.List;
import java.util.Map;

/**
 * Class for annotation of profiled sources in memory mode
 */
public class InsertMemoryMeasurementsProcessor extends AbstractInsertMeasurementsProcessor {
    /**
     * Constructs the {@link InsertMemoryMeasurementsProcessor} class.
     *
     * @param args         object with commandline arguments
     * @param measurements map between traps and measurements
     */
    public InsertMemoryMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements) {
        super(args, measurements);
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

        // trap was unreachable
        if (values.contains(null))
            return "Unreachable";

        return String.format(
                "Free Transient Deselect: %d B, Free Transient Reset: %d B, Free Persistent: %d B",
                values.get(0), values.get(1), values.get(2));
    }
}
