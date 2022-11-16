package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;
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

        // get all executable signatures
        final List<String> executables = input.filterChildren(CtExecutable.class::isInstance)
                .map(JCProfilerUtil::getFullSignature).list();

        AbstractCtElementAssert <?> assertThat = assertThat(input);
        for (final String executable : executables) {
            final Args args = new Args();
            args.executable = executable;
            assertThat = assertThat.withProcessor(new InsertCustomTrapProcessor(args));
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

        // gel all executables
        final List<CtExecutable<?>> executables = input.getElements(CtExecutable.class::isInstance);

        // Spoon must know the types of PMC fields used in SimpleClass
        StringBuilder sb = new StringBuilder("public class PMC {");
        for (final CtExecutable<?> executable : executables) {
            for (int i = 1; i <= 100; i++)
                sb.append("public static short ").append(JCProfilerUtil.getTrapNamePrefix(executable))
                        .append("_").append(i).append(" = 0;");
        }
        sb.append("}");

        spoon.addInputResource(new VirtualFile(sb.toString()));

        // add the input
        spoon.addInputResource(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        spoon.buildModel();

        return spoon.getModel().filterChildren((CtClass<?> cls) -> cls.getSimpleName().equals("SimpleClass")).first();
    }
}
