package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.ModifierKind;
import spoon.support.compiler.VirtualFile;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static spoon.testing.Assert.assertThat;

class ModifyEntryPointProcessorTest {

    @Test
    public void process() {
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestInput.java");
        final CtClass<?> expected = parseClass("ModifyEntryPointProcessorTestExpected.java");

        assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(expected);
    }

    @Test
    public void alreadyInstrumented() {
        // Using the same file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");
        final CtClass<?> expected = parseClass("ModifyEntryPointProcessorTestExpected.java");

        assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(expected);
    }

    @Test
    public void existingFieldWrongType() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");

        // change type from byte to short
        input.getField("INS_PERF_SETSTOP").setType(input.getFactory().createCtTypeReference(short.class));

        Exception e = assertThrows(
                RuntimeException.class,
                () -> assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(input));

        String expected = "Existing INS_PERF_SETSTOP field has type short! Expected: byte";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    public void existingFieldMissingFinal() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");

        // remove final modifier
        input.getField("INS_PERF_SETSTOP").removeModifier(ModifierKind.FINAL);

        Exception e = assertThrows(
                RuntimeException.class,
                () -> assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(input));

        String expected = "Existing INS_PERF_SETSTOP field is not static and final! Got: [public, static]";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    public void existingFieldMissingStatic() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");

        // remove static modifier
        input.getField("INS_PERF_SETSTOP").removeModifier(ModifierKind.STATIC);

        Exception e = assertThrows(
                RuntimeException.class,
                () -> assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(input));

        String expected = "Existing INS_PERF_SETSTOP field is not static and final! Got: [public, final]";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    public void existingFieldWrongInitializer() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");

        // sanity check
        assertNotEquals(JCProfilerUtil.INS_PERF_HANDLER, 0xFF);

        @SuppressWarnings("unchecked") // we know that the field is a byte
        // change initializer from (byte) 0xF5 to (byte) 0xFF
        final CtField<Byte> field = (CtField<Byte>) input.getField("INS_PERF_SETSTOP");

        @SuppressWarnings("unchecked") // to overcome SPOON's limitation
        final CtLiteral<Byte> lit = (CtLiteral<Byte>) (Object) input.getFactory()
                .createLiteral(0xFF)
                .setBase(LiteralBase.HEXADECIMAL)
                .addTypeCast(input.getFactory().Type().bytePrimitiveType());
        field.setAssignment(lit);

        Exception e = assertThrows(
                RuntimeException.class,
                () -> assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(input));

        String expected = String.format(
                "Existing INS_PERF_SETSTOP field has ((byte) (0xff)) as initializer! Expected: (byte) 0x%02x",
                JCProfilerUtil.INS_PERF_HANDLER);
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    public void missingProcessMethod() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");

        // delete the process method
        input.getMethodsByName("process").get(0).delete();

        assertFalse(new ModifyTimeEntryPointProcessor(new Args()).isToBeProcessed(input));
    }

    @Test
    public void declaredProcessMethod() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");

        // delete the process method body
        input.getMethodsByName("process").get(0).getBody().delete();

        Exception e = assertThrows(
                RuntimeException.class,
                () -> assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(input));

        String expected = "Class Example inherits from javacard.framework.Applet but does not implement the " +
                "'process(javacard.framework.APDU)' method!";
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    @Test
    public void inheritedProcessMethod() {
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestInput.java");
        final CtClass<?> expected = parseClass("ModifyEntryPointProcessorTestExpected.java");

        final CtClass<?> extendedInput = input.getFactory().createClass("ExtendedExample");
        extendedInput.setSuperclass(input.getReference());
        final CtClass<?> extendedInputExpected = extendedInput.clone();

        assertThat(extendedInput).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(extendedInputExpected);
        assertThat(input).isEqualTo(expected);
    }

    @Test
    public void wrongIfBody() {
        // Using already instrumented file for input is NOT a bug!
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestExpected.java");
        final CtIf ctIf = input.filterChildren(CtIf.class::isInstance).first();

        // change the if body
        final CtIf ctIfBackup = ctIf.clone();
        final CtBlock<Void> thenBlock = ctIf.getThenStatement();
        thenBlock.getStatement(0).delete();

        Exception e = assertThrows(
                RuntimeException.class,
                () -> assertThat(input).withProcessor(new ModifyTimeEntryPointProcessor(new Args())).isEqualTo(input));

        String expected = String.format(
                "The body of the INS_PERF_SETSTOP handle has unexpected contents:%n%s%nExpected:%n%s%n",
                ctIf.getThenStatement().prettyprint(), ctIfBackup.getThenStatement().prettyprint());
        String actual = e.getMessage();

        assertEquals(expected, actual);
    }

    public CtClass<?> parseClass(final String fileName) {
        final Launcher spoon = new Launcher();

        // add PM and PMC stubs
        spoon.addInputResource(new VirtualFile(
                "import javacard.framework.APDU;" +
                "public class PM { public static void set(APDU apdu) {} }", "PM.java"));
        spoon.addInputResource(new VirtualFile("public class PMC { }", "PMC.java"));

        // add the input
        spoon.addInputResource(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        spoon.buildModel();

        return spoon.getModel().getElements((CtClass<?> cls) -> cls.getSimpleName().equals("Example")).get(0);
    }
}
