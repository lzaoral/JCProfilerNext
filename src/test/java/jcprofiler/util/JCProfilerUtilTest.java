package jcprofiler.util;

import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.support.compiler.VirtualFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JCProfilerUtilTest {

    @Test
    void getProfiledMethodNull() {
        final String input = "package test; public class Test { void foo() {}; }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, null));

        String expected = "--executable argument was not provided!";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getProfiledMethodSimple() {
        final String input = "package test; public class Test { void foo() {}; }";
        final CtModel model = prepareModel(input);

        final CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "foo");

        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodMissing() {
        final String input = "package test; public class Test {}";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = "None of the provided types contain foo executable!";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getProfiledMethodSimpleNoBody() {
        final String input = "package test; public class Test { void foo(); }";
        final CtModel model = prepareModel(input);
        final CtMethod<?> foo = model.filterChildren(CtMethod.class::isInstance).first();
        foo.getBody().delete();

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = "Found the foo executable but it has no body! Found in type test.Test.";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getProfiledMethodTwoClasses() {
        final String input = "package test;" +
                "public class Test1 { void foo() {}; }" +
                "public class Test2 { void foo() {}; }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More of the provided types contain the foo executable!%n" +
                "Please, specify the --executable parameter in the 'type#foo' format where type is one of:%n" +
                "[test.Test1, test.Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test2#foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesComplexSignature() {
        final String input = "package test;" +
                             "public class Test1 { void foo(Integer a) {}; }" +
                             "public class Test2 { void foo(Integer a) {}; }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo(java.lang.Integer)"));

        String expected = String.format(
                "More of the provided types contain the foo(java.lang.Integer) executable!%n" +
                "Please, specify the --executable parameter in the 'type#foo(java.lang.Integer)' format where type " +
                "is one of:%n[test.Test1, test.Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test2#foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoMethods() {
        final String input = "package test; public class Test { void foo(double a) {}; void foo(int a) {} }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More foo executables with distinct signatures found in the test.Test type!%n" +
                "Please, add the corresponding signature to the --executable parameter%n" +
                "Found: [foo(double), foo(int)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "foo(double)");
        assertEquals("foo(double)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "foo(int)");
        assertEquals("foo(int)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoMethodsComplexSignatures() {
        final String input = "package test; public class Test { void foo(Double a) {}; void foo(Integer a) {} }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More foo executables with distinct signatures found in the test.Test type!%n" +
                "Please, add the corresponding signature to the --executable parameter%n" +
                "Found: [foo(java.lang.Double), foo(java.lang.Integer)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "foo(java.lang.Double)");
        assertEquals("foo(java.lang.Double)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesTwoMethods() {
        final String input = "package test;" +
                "public class Test1 { void foo(double a) {}; void foo(int a) {} }" +
                "public class Test2 { void foo(double a) {}; void foo(int a) {} }";
        final CtModel model = prepareModel(input);

        // everything together
        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More foo executables with distinct signatures found in more types!%n" +
                "Please, use one of the following values as an argument to the --executable parameter:%n" +
                "[test.Test1#foo(double), test.Test1#foo(int)," +
                " test.Test2#foo(double), test.Test2#foo(int)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo(double)"));

        expected = String.format(
                "More of the provided types contain the foo(double) executable!%n" +
                "Please, specify the --executable parameter in the 'type#foo(double)' format where type is one of:%n" +
                "[test.Test1, test.Test2]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified class
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo"));

        expected = String.format(
                "More foo executables with distinct signatures found in the test.Test1 type!%n" +
                "Please, add the corresponding signature to the --executable parameter%n" +
                "Found: [foo(double), foo(int)]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo(double)");
        assertEquals("foo(double)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo(int)");
        assertEquals("foo(int)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test2#foo(double)");
        assertEquals("foo(double)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test2#foo(int)");
        assertEquals("foo(int)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesTwoMethodsComplexSignatures() {
        final String input = "package test;" +
                             "public class Test1 { void foo(Double a) {}; void foo(Integer a) {} }" +
                             "public class Test2 { void foo(Double a) {}; void foo(Integer a) {} }";
        final CtModel model = prepareModel(input);

        // everything together
        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More foo executables with distinct signatures found in more types!%n" +
                "Please, use one of the following values as an argument to the --executable parameter:%n" +
                "[test.Test1#foo(java.lang.Double), test.Test1#foo(java.lang.Integer)," +
                " test.Test2#foo(java.lang.Double), test.Test2#foo(java.lang.Integer)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo(java.lang.Double)"));

        expected = String.format(
                "More of the provided types contain the foo(java.lang.Double) executable!%n" +
                "Please, specify the --executable parameter in the 'type#foo(java.lang.Double)' " +
                "format where type is one of:%n[test.Test1, test.Test2]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified class
        e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo"));

        expected = String.format(
                "More foo executables with distinct signatures found in the test.Test1 type!%n" +
                "Please, add the corresponding signature to the --executable parameter%n" +
                "Found: [foo(java.lang.Double), foo(java.lang.Integer)]");
        actual = e.getMessage();

        assertEquals(expected, actual);

        // specified signature
        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo(java.lang.Double)");
        assertEquals("foo(java.lang.Double)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test2#foo(java.lang.Double)");
        assertEquals("foo(java.lang.Double)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test2#foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesNested() {
        final String input = "package test;" +
                "public class Test1 { void foo() {};" +
                "public class Test2 { void foo() {}; }; }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More of the provided types contain the foo executable!%n" +
                "Please, specify the --executable parameter in the 'type#foo' format where type is one of:%n" +
                "[test.Test1, test.Test1$Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test1$Test2#foo");
        assertEquals("foo()", method.getSignature());
        assertEquals("test.Test1$Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodTwoClassesNestedComplexSignatures() {
        final String input = "package test;" +
                "public class Test1 { void foo(Integer a) {};" +
                "public class Test2 { void foo(Integer a) {}; }; }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo(java.lang.Integer)"));

        String expected = String.format(
                "More of the provided types contain the foo(java.lang.Integer) executable!%n" +
                "Please, specify the --executable parameter in the 'type#foo(java.lang.Integer)' format where type " +
                "is one of:%n[test.Test1, test.Test1$Test2]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "test.Test1#foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "test.Test1$Test2#foo(java.lang.Integer)");
        assertEquals("foo(java.lang.Integer)", method.getSignature());
        assertEquals("test.Test1$Test2", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodNestedType() {
        final String input = "package test;" +
                "public class Test { public class Test1 {}; public class Test2 {};" +
                "void foo(Test1 a) {}; void foo(Test2 a) {} }";
        final CtModel model = prepareModel(input);

        Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "foo"));

        String expected = String.format(
                "More foo executables with distinct signatures found in the test.Test type!%n" +
                "Please, add the corresponding signature to the --executable parameter%n" +
                "Found: [foo(test.Test$Test1), foo(test.Test$Test2)]");
        String actual = e.getMessage();

        assertEquals(expected, actual);

        CtMethod<?> method = JCProfilerUtil.getProfiledMethod(model, "foo(test.Test$Test1)");
        assertEquals("foo(test.Test$Test1)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());

        method = JCProfilerUtil.getProfiledMethod(model, "foo(test.Test$Test2)");
        assertEquals("foo(test.Test$Test2)", method.getSignature());
        assertEquals("test.Test", method.getDeclaringType().getQualifiedName());
    }

    @Test
    void getProfiledMethodConstructor() {
        final String input = "package test;" +
                "public class Test {}";
        final CtModel model = prepareModel(input);

        final Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getProfiledMethod(model, "Test"));

        final String expected = "Executable test.Test#Test() is not a method!";
        final String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getFullSignature() {
        final String input = "package test;" +
                "public class Test {" +
                "   public class Test1 {};" +
                "   public class Test2 {" +
                "       void foo(Test1 a, Long b, int[] c) {};" +
                "   }" +
                " };";
        final CtModel model = prepareModel(input);
        final CtMethod<?> method = model.filterChildren(CtMethod.class::isInstance).first();

        assertEquals("test.Test$Test2#foo(test.Test$Test1,java.lang.Long,int[])",
                JCProfilerUtil.getFullSignature(method));

        final List<String> trapNames = model.filterChildren(CtConstructor.class::isInstance)
                .map(JCProfilerUtil::getFullSignature).list();
        assertEquals("test.Test#Test()", trapNames.get(0));
        assertEquals("test.Test$Test1#Test1()", trapNames.get(1));
        assertEquals("test.Test$Test2#Test2()", trapNames.get(2));
    }

    @Test
    void getFullSignatureDefaultPackage() {
        final String input = "public class Test {" +
                "    void foo(short a, Long b, int[] c) {};" +
                "};";
        final CtModel model = prepareModel(input);

        final CtMethod<?> method = model.filterChildren(CtMethod.class::isInstance).first();
        assertEquals("Test#foo(short,java.lang.Long,int[])", JCProfilerUtil.getFullSignature(method));

        final CtConstructor<?> ctor = model.filterChildren(CtConstructor.class::isInstance).first();
        assertEquals("Test#Test()", JCProfilerUtil.getFullSignature(ctor));
    }

    @Test
    void getTrapNamePrefix() {
        final String input = "package test;" +
                "public class Test { public class Test1 {}; public class Test2 {" +
                "void foo(Test1 a, Long b, int[] c) {};" +
                "} };";
        final CtModel model = prepareModel(input);
        final CtMethod<?> method = model.filterChildren(CtMethod.class::isInstance).first();

        assertEquals("TRAP_test_Test_dol_Test2_hash_foo_argb_test_Test_dol_Test1__java_lang_Long__int_arr_arge",
                JCProfilerUtil.getTrapNamePrefix(method));

        final List<String> trapNames = model.filterChildren(CtConstructor.class::isInstance)
                        .map(JCProfilerUtil::getTrapNamePrefix).list();
        assertEquals("TRAP_test_Test_hash_Test_argb_arge", trapNames.get(0));
        assertEquals("TRAP_test_Test_dol_Test1_hash_Test1_argb_arge", trapNames.get(1));
        assertEquals("TRAP_test_Test_dol_Test2_hash_Test2_argb_arge", trapNames.get(2));
    }

    @Test
    void isClsEntryPointRegularClass() {
        final String input = "package test;" +
                "public class Test {};";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren(CtClass.class::isInstance).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointSimple() {
        final String input = "package test;" +
                "public class Entry extends javacard.framework.Applet {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {} +" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren(CtClass.class::isInstance).first();

        assertTrue(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointNoProcess() {
        final String input = "package test;" +
                "public class Entry extends javacard.framework.Applet {" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {}" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren(CtClass.class::isInstance).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointNoInstall() {
        final String input = "package test;" +
                "public class Entry extends javacard.framework.Applet {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren(CtClass.class::isInstance).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointAbstractProcess() {
        final String input = "package test;" +
                "public abstract class Entry extends javacard.framework.Applet {}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren(CtClass.class::isInstance).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointInherited1() {
        final String input = "package test;" +
                "public class Test extends javacard.framework.Applet {}" +
                "public class Entry extends Test {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {}" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren((CtClass<?> c) -> c.getSimpleName().equals("Entry")).first();

        assertTrue(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointInherited2() {
        final String input = "package test;" +
                "public class Test extends javacard.framework.Applet {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "}" +
                "public class Entry extends Test {" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {}" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren((CtClass<?> c) -> c.getSimpleName().equals("Entry")).first();

        assertTrue(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointInheritedNoProcess() {
        final String input = "package test;" +
                "public class Test extends javacard.framework.Applet {}" +
                "public class Entry extends Test {" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {}" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren((CtClass<?> c) -> c.getSimpleName().equals("Entry")).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointInheritedNoInstall() {
        final String input = "package test;" +
                "public class Test extends javacard.framework.Applet {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "}" +
                "public class Entry extends Test {}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren((CtClass<?> c) -> c.getSimpleName().equals("Entry")).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    @Test
    void isClsEntryPointInheritedAbstractProcess() {
        final String input = "package test;" +
                "public abstract class Test extends javacard.framework.Applet {}" +
                "public abstract class Entry extends Test {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "}";
        final CtModel model = prepareModel(input);
        final CtClass<?> cls = model.filterChildren((CtClass<?> c) -> c.getSimpleName().equals("Entry")).first();

        assertFalse(JCProfilerUtil.isTypeEntryPoint(cls));
    }

    private static CtModel prepareModel(final String cls) {
        final Launcher spoon = new Launcher();
        spoon.addInputResource(new VirtualFile(cls));
        return spoon.buildModel();
    }

    @Test
    void getEntryPointConstructorNoCall() {
        final String input = "package test;" +
                "public class Entry extends javacard.framework.Applet {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {}" +
                "}";
        final CtModel model = prepareModel(input);

        final Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getEntryPointConstructor(model, ""));

        final String expected =
                "The test.Entry#install(byte[],short,byte) method does not call any constructor of the test.Entry type!";
        final String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getEntryPointConstructorTwoCalls() {
        final String input = "package test;" +
                "public class Entry extends javacard.framework.Applet {" +
                "   Entry() {}" +
                "   Entry(int a) {}" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {" +
                "       new Entry();" +
                "       new Entry(1);" +
                "   }" +
                "}";
        final CtModel model = prepareModel(input);

        final Exception e = assertThrows(RuntimeException.class,
                () -> JCProfilerUtil.getEntryPointConstructor(model, ""));

        final String expected = String.format(
                "The test.Entry#install(byte[],short,byte) method calls more than one constructor" +
                " of the test.Entry type: [test.Entry(), test.Entry(int)]%n" +
                "Specify it using the --executable option.");
        final String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    void getEntryPointConstructor() {
        final String input = "package test;" +
                "public class Entry extends javacard.framework.Applet {" +
                "   @Override" +
                "   public void process(javacard.framework.APDU apdu) {}" +
                "   public static void install(byte[] bArray, short bOffset, byte bLength) {" +
                "       new Entry();" +
                "   }" +
                "}";
        final CtModel model = prepareModel(input);
        final CtConstructor<?> constructor = model.filterChildren(CtConstructor.class::isInstance).first();

        assertEquals(constructor, JCProfilerUtil.getEntryPointConstructor(model, ""));
    }
}
