package jcprofiler.instrumentation.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Workarounds for bugs in automatic generation of import statements from other compilation units:
 *     - Fixes import generation for nested classes.
 *     - Fixes import generation for static fields imported through a static import.
 *     - Fixes import generation for static methods imported through a static import.
 */
public class SpoonWorkarounds {
    static public class FixNestedClassImportProcessor extends AbstractProcessor<CtTypeReference<?>> {
        @Override
        public boolean isToBeProcessed(final CtTypeReference<?> typeRef) {
            // check that the type is a nested
            return typeRef.getDeclaringType() != null;
        }

        @Override
        public void process(final CtTypeReference<?> typeRef) {
            // make the parent class access explicit
            typeRef.getDeclaringType().setImplicit(false);
        }
    }

    static public class FixStaticMethodImportProcessor extends AbstractProcessor<CtInvocation<?>> {
        @Override
        public boolean isToBeProcessed(final CtInvocation<?> invocation) {
            // check that the invoked method is static and that this occurs in a different class
            return invocation.getExecutable().isStatic() &&
                    !invocation.getExecutable().getDeclaringType().equals(
                            invocation.getExecutable().getParent(CtType.class).getReference());
        }

        @Override
        public void process(final CtInvocation<?> invocation) {
            // make the parent class access explicit
            invocation.getTarget().setImplicit(false);
        }
    }

    static public class FixStaticFieldImportProcessor extends AbstractProcessor<CtFieldAccess<?>> {
        @Override
        public boolean isToBeProcessed(final CtFieldAccess<?> fieldAccess) {
            // check that the accessed field is static and that this occurs in a different class
            return fieldAccess.getVariable().isStatic() &&
                   !fieldAccess.getVariable().getDeclaringType().equals(
                           fieldAccess.getParent(CtType.class).getReference());
        }

        @Override
        public void process(final CtFieldAccess<?> fieldAccess) {
            // make the parent class access explicit
            fieldAccess.getTarget().setImplicit(false);
        }
    }
}
