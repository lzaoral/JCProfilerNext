package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import spoon.reflect.declaration.CtExecutable;

public class InsertCustomTrapProcessor extends AbstractInsertTrapProcessor<CtExecutable<?>> {
    public InsertCustomTrapProcessor(final Args args) {
        super(args);
    }

    @Override
    public boolean isToBeProcessed(final CtExecutable<?> executable) {
        return JCProfilerUtil.getFullSignature(executable).equals(args.executable);
    }

    @Override
    public void process(final CtExecutable<?> executable) {
        super.process(executable);
    }
}
