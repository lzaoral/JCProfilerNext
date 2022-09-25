package jcprofiler.instrumentation.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.reference.CtTypeReference;

/**
 * Workaround for a bug in automatic generation of imports for nested classes in other compilation units.
 */
public class FixNestedClassImportProcessor extends AbstractProcessor<CtTypeReference<?>> {
    @Override
    public boolean isToBeProcessed(final CtTypeReference<?> typeRef) {
        // check that the type is a nested class
        return typeRef.getDeclaringType() != null;
    }

    @Override
    public void process(final CtTypeReference<?> typeRef) {
        // make the parent class access explicit
        typeRef.getDeclaringType().setImplicit(false);
    }
}
