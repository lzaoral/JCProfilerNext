package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import spoon.reflect.declaration.CtExecutable;

public class InsertCustomTrapProcessor extends AbstractInsertTrapProcessor<CtExecutable<?>> {
    public InsertCustomTrapProcessor(final Args args) {
        super(args);
    }
}
