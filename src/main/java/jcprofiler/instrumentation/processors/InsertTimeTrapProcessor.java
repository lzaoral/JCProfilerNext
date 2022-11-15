package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import spoon.reflect.declaration.CtMethod;

/**
 * Class for performance trap insertion in time mode
 * <br>
 * Applicable to instances of {@link CtMethod}.
 *
 */
public class InsertTimeTrapProcessor extends AbstractInsertTrapProcessor<CtMethod<?>> {
    /**
     * Constructs the {@link InsertTimeTrapProcessor} class.
     *
     * @param args object with commandline arguments
     */
    public InsertTimeTrapProcessor(final Args args) {
        super(args);
    }
}
