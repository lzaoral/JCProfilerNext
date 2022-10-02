package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

public class InsertTimeTrapProcessor extends AbstractInsertTrapProcessor<CtMethod<?>> {
    public InsertTimeTrapProcessor(final Args args) {
        super(args);
    }

    @Override
    public boolean isToBeProcessed(final CtMethod<?> method) {
        return JCProfilerUtil.getFullSignature(method).equals(args.method);
    }

    @Override
    public void process(final CtMethod<?> method) {
        process((CtExecutable<?>) method);
    }
}
