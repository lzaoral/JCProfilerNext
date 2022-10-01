package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

public abstract class AbstractProfilerProcessor<T extends CtElement> extends AbstractProcessor<T> {
    protected final Args args;

    protected CtClass<?> PM;
    protected CtClass<?> PMC;

    protected AbstractProfilerProcessor(final Args args) {
        this.args = args;
    }

    private CtClass<?> getClass(final String simpleName) {
        final CtClass<?> cls = getFactory().getModel().filterChildren(
                (CtType<?> c) -> c.isTopLevel() && c.getSimpleName().equals(simpleName)).first();
        if (cls == null)
            throw new RuntimeException("Class " + simpleName + " not found!");

        return cls;
    }

    @Override
    public void init() {
        super.init();
        PM = getClass("PM");
        PMC = getClass("PMC");
    }
}
