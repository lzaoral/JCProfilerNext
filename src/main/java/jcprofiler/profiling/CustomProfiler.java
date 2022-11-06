package jcprofiler.profiling;

import cz.muni.fi.crocs.rcard.client.CardManager;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.csv.CSVPrinter;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtConstructor;

import java.io.IOException;

// custom profiler will only generate inputs!
public class CustomProfiler extends AbstractProfiler {
    public CustomProfiler(final Args args, final CardManager cardManager, final CtModel model) {
        super(args, cardManager, JCProfilerUtil.getProfiledMethod(model, args.method));
    }

    @Override
    public void profileImpl() {
        if (!(profiledExecutable instanceof CtConstructor))
            generateInputs(args.repeatCount);
    }

    @Override
    protected void saveMeasurements(final CSVPrinter printer) throws IOException {
        printer.printComment("trapName,measurement1,measurement2,...");
        for (final String trapName : trapNameMap.values())
            printer.printRecord(trapName, "TODO");
    }
}
