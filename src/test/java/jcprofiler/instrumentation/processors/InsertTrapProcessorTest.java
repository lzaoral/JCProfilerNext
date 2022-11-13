package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import jcprofiler.util.JCProfilerUtil;
import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.support.compiler.VirtualFile;
import spoon.testing.AbstractCtElementAssert;

import java.util.List;
import java.util.Objects;

import static spoon.testing.Assert.assertThat;

class InsertTrapProcessorTest {
    @Test
    public void process() {
        final CtClass<?> input = parseInputClass("InsertTrapProcessorTestInput.java");
        final CtClass<?> expected = parseExpectedClass(input, "InsertTrapProcessorTestExpected.java");

        final List<String> methods = input.filterChildren(CtMethod.class::isInstance)
                .map(JCProfilerUtil::getFullSignature).list();

        AbstractCtElementAssert <?> assertThat = assertThat(input);
        for (String method : methods) {
            final Args args = new Args();
            args.executable = method;
            assertThat = assertThat.withProcessor(new InsertTimeTrapProcessor(args));
        }
        assertThat.isEqualTo(expected);
    }

    public CtClass<?> parseInputClass(final String fileName) {
        final Launcher spoon = new Launcher();

        // add PM stub
        spoon.addInputResource(new VirtualFile(
                "public class PM { public static void check(short s) {} }"));
        spoon.addInputResource(new VirtualFile(
                "public class PMC { public static final short PERF_START = (short) 0x1; }"));

        // add the input
        spoon.addInputResource(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        spoon.buildModel();

        return spoon.getModel().filterChildren((CtClass<?> cls) -> cls.getSimpleName().equals("SimpleClass")).first();
    }

    public CtClass<?> parseExpectedClass(final CtClass<?> input, final String fileName) {
        final Launcher spoon = new Launcher();

        // add PM stub
        spoon.addInputResource(new VirtualFile("public class PM { public static void check(short s) {} }"));

        // Spoon must know the types of PMC fields used in SimpleClass
        final List<CtMethod<?>> methods = input.getElements(CtMethod.class::isInstance);
        StringBuilder sb = new StringBuilder("public class PMC {");
        for (final CtMethod<?> method : methods) {
            for (int i = 1; i <= 100; i++)
                sb.append("public static short ").append(JCProfilerUtil.getTrapNamePrefix(method)).append("_").append(i)
                        .append(" = 0;");
        }
        sb.append("}");

        spoon.addInputResource(new VirtualFile(sb.toString()));

        // add the input
        spoon.addInputResource(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        spoon.buildModel();

        return spoon.getModel().filterChildren((CtClass<?> cls) -> cls.getSimpleName().equals("SimpleClass")).first();
    }
}
