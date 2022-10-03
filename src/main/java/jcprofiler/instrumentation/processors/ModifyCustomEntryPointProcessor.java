package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtParameter;

import java.io.IOException;
import java.nio.file.Files;

public class ModifyCustomEntryPointProcessor extends AbstractModifyEntryPointProcessor {
    public ModifyCustomEntryPointProcessor(final Args args) {
        super(args);
    }

    @Override
    public boolean isToBeProcessed(final CtClass<?> cls) {
        return args.customHandler != null && super.isToBeProcessed(cls);
    }

    @Override
    public void process(final CtClass<?> cls) {
        process(cls, "INS_PERF_CUSTOM");
    }

    @Override
    protected CtBlock<Void> createInsHandlerBody(final CtParameter<?> apdu) {
        try {
            final CtStatement customBlock = getFactory().createCodeSnippetStatement(
                    new String(Files.readAllBytes(args.customHandler)));

            // {
            //     ${customBlock}
            //     return;
            // }
            return getFactory().createBlock().addStatement(customBlock).addStatement(getFactory().createReturn());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
