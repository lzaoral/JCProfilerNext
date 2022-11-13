package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import spoon.reflect.declaration.CtMethod;

public class InsertTimeTrapProcessor extends AbstractInsertTrapProcessor<CtMethod<?>> {
    public InsertTimeTrapProcessor(final Args args) {
        super(args);
    }
}
