package jcprofiler.instrumentation.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Workarounds for bugs in automatic generation of import statements from other compilation units:
 *     - Fixes import generation for nested types.
 *     - Fixes import generation for static fields imported through a static import.
 *     - Fixes import generation for static methods imported through a static import.
 */
public class SpoonWorkarounds {
    static public class FixNestedClassImportProcessor extends AbstractProcessor<CtTypeReference<?>> {
        @Override
        public boolean isToBeProcessed(final CtTypeReference<?> typeRef) {
            // check that the type is nested
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
            final CtExecutableReference<?> execRef = invocation.getExecutable();
            final CtTypeReference<?> parentTypeRef = invocation.getParent(CtType.class).getReference();

            // check that the invoked method is static and that this occurs in a different class
            return execRef.isStatic() && !execRef.getDeclaringType().equals(parentTypeRef);
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
            final CtFieldReference<?> fieldRef = fieldAccess.getVariable();
            final CtTypeReference<?> parentTypeRef = fieldAccess.getParent(CtType.class).getReference();

            // check that the accessed field is static and that this occurs in a different class
            return fieldRef.isStatic() && !fieldRef.getDeclaringType().equals(parentTypeRef);
        }

        @Override
        public void process(final CtFieldAccess<?> fieldAccess) {
            // make the parent class access explicit
            fieldAccess.getTarget().setImplicit(false);
        }
    }
}
