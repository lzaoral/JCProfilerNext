package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.support.compiler.VirtualFile;
import spoon.testing.AbstractCtElementAssert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static spoon.testing.Assert.assertThat;

class InsertTrapProcessorTest {
    // TODO: instrument everything when method is null?
    final static List<String> methods = Arrays.asList(
            "foo",
            "terminator1", "terminator2", "terminator3", "terminator4", "terminator5", "terminator6", "terminator7",
            "terminator8");

    @Test
    public void process() {
        final CtClass<?> input = parseClass("InsertTrapProcessorTestInput.java");
        final CtClass<?> expected = parseClass("InsertTrapProcessorTestExpected.java");

        AbstractCtElementAssert<?> assertThat = assertThat(input);
        for (String method : methods) {
            final Args args = new Args();
            args.method = method;
            assertThat = assertThat.withProcessor(new InsertTrapProcessor(args));
        }
        assertThat.isEqualTo(expected);
    }

    public CtClass<?> parseClass(final String fileName) {
        final Launcher spoon = new Launcher();

        // add PM stub
        spoon.addInputResource(new VirtualFile(
                "public class PM { public static void check(short s) {} }", "PM.java"));

        // SPOON must know the types of PMC fields used in SimpleClass
        if (fileName.equals("InsertTrapProcessorTestExpected.java")) {
            StringBuilder sb = new StringBuilder("public class PMC { ");

            for (String method : methods) {
                for (int i = 1; i <= 100; i++)
                    sb.append("public static short TRAP_SimpleClass_").append(method).append("_argb_int_arge_")
                            .append(i).append(" = 0;");
            }

            sb.append("}");
            spoon.addInputResource(new VirtualFile(sb.toString(), "PMC.java"));
        } else {
            spoon.addInputResource(new VirtualFile("public class PMC {}", "PMC.java"));
        }

        // add the input
        spoon.addInputResource(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        spoon.buildModel();

        return spoon.getModel().getElements((CtClass<?> cls) -> cls.getSimpleName().equals("SimpleClass")).get(0);
    }
}
