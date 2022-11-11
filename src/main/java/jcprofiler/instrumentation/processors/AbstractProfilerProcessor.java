package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

public abstract class AbstractProfilerProcessor<T extends CtElement> extends AbstractProcessor<T> {
    protected final Args args;

    protected CtType<?> PM;
    protected CtType<?> PMC;

    protected AbstractProfilerProcessor(final Args args) {
        this.args = args;
    }

    @Override
    public void init() {
        super.init();

        final CtModel model = getFactory().getModel();
        PM = JCProfilerUtil.getToplevelType(model, "PM");
        PMC = JCProfilerUtil.getToplevelType(model, "PMC");
    }
}
