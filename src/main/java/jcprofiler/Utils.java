package jcprofiler;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

public class Utils {
    // static class!
    private Utils() {}

    public static List<CtClass<?>> getEntryPoints(final CtModel model) {
        return model.getElements(Utils::isClsEntryPoint);
    }

    public static boolean isClsEntryPoint(final CtClass<?> cls) {
        final CtTypeReference<?> parent = cls.getSuperclass();
        return parent != null && parent.getQualifiedName().equals("javacard.framework.Applet");
    }
}
