package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.support.compiler.VirtualFile;

import java.util.Objects;

import static spoon.testing.Assert.assertThat;

class ModifyEntryPointProcessorTest {

    @Test
    public void process() {
        final CtClass<?> input = parseClass("ModifyEntryPointProcessorTestInput.java");
        final CtClass<?> expected = parseClass("ModifyEntryPointProcessorTestExpected.java");

        assertThat(input).withProcessor(new ModifyEntryPointProcessor(new Args())).isEqualTo(expected);
    }

    public CtClass<?> parseClass(final String fileName) {
        final Launcher spoon = new Launcher();

        // add PM stub
        spoon.addInputResource(new VirtualFile(
                "public class PM { public static short m_perfStop = -1; }", "PM.java"));

        // add the input
        spoon.addInputResource(Objects.requireNonNull(getClass().getResource(fileName)).getPath());
        spoon.buildModel();

        return spoon.getModel().getElements((CtClass<?> cls) -> cls.getSimpleName().equals("Example")).get(0);
    }
}
