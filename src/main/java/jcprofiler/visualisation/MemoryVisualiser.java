package jcprofiler.visualisation;

import jcprofiler.args.Args;
import jcprofiler.visualisation.processors.AbstractInsertMeasurementsProcessor;

import jcprofiler.visualisation.processors.InsertMemoryMeasurementsProcessor;
import org.apache.velocity.VelocityContext;

import spoon.SpoonAPI;

public class MemoryVisualiser extends AbstractVisualiser {
    public MemoryVisualiser(final Args args, final SpoonAPI spoon) {
        super(args, spoon);
    }

    @Override
    public AbstractInsertMeasurementsProcessor getInsertMeasurementsProcessor() {
        return new InsertMemoryMeasurementsProcessor(args, measurements);
    }

    @Override
    public void prepareVelocityContext(final VelocityContext context) {}
}
