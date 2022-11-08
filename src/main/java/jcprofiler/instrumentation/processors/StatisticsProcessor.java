package jcprofiler.instrumentation.processors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.*;

import java.util.*;

public class StatisticsProcessor extends AbstractProcessor<CtReference> {
    private final Set<CtPackageReference> pkgs = new HashSet<>();

    public SortedMap<Triple<String, String, String>, Integer> getUsedReferences() {
        return Collections.unmodifiableSortedMap(usedReferences);
    }

    private final SortedMap<Triple<String, String, String>, Integer> usedReferences = new TreeMap<>();

    @Override
    public void init() {
        super.init();

        // populate pkgs set
        getFactory().getModel().<CtType<?>>getElements(CtType.class::isInstance).stream()
                .map(CtType::getPackage).filter(Objects::nonNull).map(CtPackage::getReference).forEach(pkgs::add);
    }

    @Override
    public boolean isToBeProcessed(final CtReference ref) {
        return ref instanceof CtTypeReference || ref instanceof CtFieldReference || ref instanceof CtExecutableReference;
    }

    @Override
    public void process(final CtReference ref) {
        if (ref instanceof CtTypeReference) {
            final CtTypeReference<?> typeRef = (CtTypeReference<?>) ref;

            // ignore null, primitive types and arrays (the array element type will be processed separately)
            if (typeRef.isPrimitive() || typeRef.isArray() || ref.equals(getFactory().Type().NULL_TYPE))
                return;

            final CtElement parent = typeRef.getParent();

            // ignore types of expressions (e.g. assignment or field access) except for explicit type accesses
            // (e.g. in instanceof, when accessing a static field or static method or in type casts)
            if (parent instanceof CtExpression && !(parent instanceof CtTypeAccess) &&
                    !((CtExpression<?>)parent).getTypeCasts().contains(typeRef))
                return;

            // ignore types referenced in methods, fields and local variable accesses
            if (parent instanceof CtVariableReference || parent instanceof CtExecutableReference)
                return;

            add(typeRef, /* member */ "");
            return;
        }

        if (ref instanceof CtExecutableReference) {
            final CtExecutableReference<?> execRef = ((CtExecutableReference<?>) ref);
            final CtTypeReference<?> declTypeRef = execRef.getDeclaringType();

            String signature = execRef.getSignature();
            if (execRef.isConstructor()) {
                // Spoon appends a fully qualified outer class or package name to the constructor signature.
                final String prefix = declTypeRef.getPackage() != null
                                        ? declTypeRef.getPackage().getQualifiedName()
                                        : declTypeRef.getDeclaringType().getQualifiedName();
                signature = signature.substring(prefix.length() + /*. or $*/ 1);
            }

            add(declTypeRef, signature);
            return;
        }

        // ref instanceof CtFieldReference
        final CtFieldReference<?> fieldRef = (CtFieldReference<?>) ref;
        final CtTypeReference<?> declTypeRef = fieldRef.getDeclaringType();

        // skip e.g. int[].length
        if (declTypeRef != null && (declTypeRef.isPrimitive() || declTypeRef.isArray()))
            return;

        add(declTypeRef, fieldRef.getSimpleName());
    }

    private void add(final CtTypeReference<?> type, final String member) {
        String parentQualifiedName = "<unknown>";
        String typeSimpleName = "<unknown>";
        if (type != null) {
            typeSimpleName = type.getSimpleName();

            // deal with inner classes
            CtTypeReference<?> outerType = type;
            while (outerType != null && outerType.getPackage() == null)
                outerType = outerType.getDeclaringType();

            if (outerType != null && outerType.getPackage() != null) {
                final CtPackageReference pkg = outerType.getPackage();
                if (pkgs.contains(pkg))
                    return;

                parentQualifiedName = type != outerType ? type.getDeclaringType().getQualifiedName()
                                                        : pkg.getQualifiedName();
                if (parentQualifiedName.isEmpty())
                    parentQualifiedName = "<unknown>";
            }
        }

        usedReferences.compute(new ImmutableTriple<>(parentQualifiedName, typeSimpleName, member),
                (k, v) -> v == null ? 1 : v + 1);
    }
}
