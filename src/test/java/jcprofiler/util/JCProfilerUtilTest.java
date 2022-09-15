package jcprofiler.util;

import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtMethod;
import spoon.support.compiler.VirtualFile;

import static org.junit.jupiter.api.Assertions.*;

class JCProfilerUtilTest {

    @Test
    void getProfiledMethodNull() {
        final String input = "package test; public class Test { void foo() {}; }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, null));

        String expected = "--method argument was not provided!";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getProfiledMethodSimple() {
        final String input = "package test; public class Test { void foo() {}; }";
        final SpoonAPI spoon = prepareSpoon(input);

        final CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "foo");

        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodMissing() {
        final String input = "package test; public class Test {}";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = "None of the provided classes contain foo method!";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getProfiledMethodSimpleNoBody() {
        final String input = "package test; public class Test { void foo(); }";
        final SpoonAPI spoon = prepareSpoon(input);
        final CtMethod<?> foo = spoon.getModel().filterChildren(CtMethod.class::isInstance).first();
        foo.getBody().delete();

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = "Found the foo method but it has no body! Found in class test.Test.";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getProfiledMethodTwoClasses() {
        final String input = "package test;" +
                "public class Test1 { void foo() {}; }" +
                "public class Test2 { void foo() {}; }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More of the provided classes contain the foo method!%n" +
                "Please, specify the --method parameter in the 'class.foo' format where class is one of:%n" +
                "[test.Test1, test.Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test2.foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesComplexSignature() {
        final String input = "package test;" +
                             "public class Test1 { void foo(Integer a) {}; }" +
                             "public class Test2 { void foo(Integer a) {}; }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo(java.lang.Integer)"));

        String expected = String.format(
                "More of the provided classes contain the foo(java.lang.Integer) method!%n" +
                "Please, specify the --method parameter in the 'class.foo(java.lang.Integer)' format where class is one of:%n" +
                "[test.Test1, test.Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test2.foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoMethods() {
        final String input = "package test; public class Test { void foo(double a) {}; void foo(int a) {} }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More foo methods with distinct signatures found in the test.Test class!%n" +
                "Please, add the corresponding signature to the --method parameter%n" +
                "Found: [foo(double), foo(int)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "foo(double)");
        assertEquals("foo(double)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "foo(int)");
        assertEquals("foo(int)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoMethodsComplexSignatures() {
        final String input = "package test; public class Test { void foo(Double a) {}; void foo(Integer a) {} }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More foo methods with distinct signatures found in the test.Test class!%n" +
                "Please, add the corresponding signature to the --method parameter%n" +
                "Found: [foo(java.lang.Double), foo(java.lang.Integer)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "foo(java.lang.Double)");
        assertEquals("foo(java.lang.Double)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesTwoMethods() {
        final String input = "package test;" +
                "public class Test1 { void foo(double a) {}; void foo(int a) {} }" +
                "public class Test2 { void foo(double a) {}; void foo(int a) {} }";
        final SpoonAPI spoon = prepareSpoon(input);

        // everything together
        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More foo methods with distinct signatures found in more classes!%n" +
                "Please, use one of the following values as an argument to the --method parameter:%n" +
                "[test.Test1.foo(double), test.Test1.foo(int)," +
                " test.Test2.foo(double), test.Test2.foo(int)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo(double)"));

        expected = String.format(
                "More of the provided classes contain the foo(double) method!%n" +
                "Please, specify the --method parameter in the 'class.foo(double)' format where class is one of:%n" +
                "[test.Test1, test.Test2]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified class
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo"));

        expected = String.format(
                "More foo methods with distinct signatures found in the test.Test1 class!%n" +
                "Please, add the corresponding signature to the --method parameter%n" +
                "Found: [foo(double), foo(int)]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo(double)");
        assertEquals("foo(double)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo(int)");
        assertEquals("foo(int)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test2.foo(double)");
        assertEquals("foo(double)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test2.foo(int)");
        assertEquals("foo(int)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesTwoMethodsComplexSignatures() {
        final String input = "package test;" +
                             "public class Test1 { void foo(Double a) {}; void foo(Integer a) {} }" +
                             "public class Test2 { void foo(Double a) {}; void foo(Integer a) {} }";
        final SpoonAPI spoon = prepareSpoon(input);

        // everything together
        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More foo methods with distinct signatures found in more classes!%n" +
                "Please, use one of the following values as an argument to the --method parameter:%n" +
                "[test.Test1.foo(java.lang.Double), test.Test1.foo(java.lang.Integer)," +
                " test.Test2.foo(java.lang.Double), test.Test2.foo(java.lang.Integer)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo(java.lang.Double)"));

        expected = String.format(
                "More of the provided classes contain the foo(java.lang.Double) method!%n" +
                "Please, specify the --method parameter in the 'class.foo(java.lang.Double)' format where class is one of:%n" +
                "[test.Test1, test.Test2]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified class
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo"));

        expected = String.format(
                "More foo methods with distinct signatures found in the test.Test1 class!%n" +
                "Please, add the corresponding signature to the --method parameter%n" +
                "Found: [foo(java.lang.Double), foo(java.lang.Integer)]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo(java.lang.Double)");
        assertEquals("foo(java.lang.Double)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test2.foo(java.lang.Double)");
        assertEquals("foo(java.lang.Double)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test2.foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesNested() {
        final String input = "package test;" +
                "public class Test1 { void foo() {};" +
                "public class Test2 { void foo() {}; }; }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More of the provided classes contain the foo method!%n" +
                "Please, specify the --method parameter in the 'class.foo' format where class is one of:%n" +
                "[test.Test1, test.Test1$Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1$Test2.foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test1$Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesNestedComplexSignatures() {
        final String input = "package test;" +
                "public class Test1 { void foo(Integer a) {};" +
                "public class Test2 { void foo(Integer a) {}; }; }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo(java.lang.Integer)"));

        String expected = String.format(
                "More of the provided classes contain the foo(java.lang.Integer) method!%n" +
                "Please, specify the --method parameter in the 'class.foo(java.lang.Integer)' format where class is one of:%n" +
                "[test.Test1, test.Test1$Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1.foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "test.Test1$Test2.foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1$Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodNestedType() {
        final String input = "package test;" +
                "public class Test { public class Test1 {}; public class Test2 {};" +
                "void foo(Test1 a) {}; void foo(Test2 a) {} }";
        final SpoonAPI spoon = prepareSpoon(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(spoon, "foo"));

        String expected = String.format(
                "More foo methods with distinct signatures found in the test.Test class!%n" +
                "Please, add the corresponding signature to the --method parameter%n" +
                "Found: [foo(test.Test$Test1), foo(test.Test$Test2)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(spoon, "foo(test.Test$Test1)");
        assertEquals("foo(test.Test$Test1)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(spoon, "foo(test.Test$Test2)");
        assertEquals("foo(test.Test$Test2)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getTrapNamePrefix() {
        final String input = "package test;" +
                "public class Test { public class Test1 {}; public class Test2 {" +
                "void foo(Test1 a, Long b, int[] c) {};" +
                "} };";
        final SpoonAPI spoon = prepareSpoon(input);
        final CtMethod<?> method = spoon.getModel().filterChildren(CtMethod.class::isInstance).first();

        assertEquals("TRAP_test_Test_dol_Test2_foo_argb_test_Test_dol_Test1__java_lang_Long__int_arr_arge",
                JCProfilerUtil.getTrapNamePrefix(method));
    }

    private static SpoonAPI prepareSpoon(final String cls) {
        final Launcher spoon = new Launcher();
        spoon.addInputResource(new VirtualFile(cls));
        spoon.buildModel();
        return spoon;
    }
}
