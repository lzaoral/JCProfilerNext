package jcprofiler;

import com.beust.jcommander.JCommander;
import jcprofiler.args.Args;

public class Main {
    public static void main(final String[] argv) {
        Args args = new Args();
        JCommander jc = JCommander.newBuilder().addObject(args).build();

        jc.setProgramName("JCProfiler");
        jc.parse(argv);

        if (args.help) {
            jc.usage();
            return;
        }

        JCProfiler.run(args);
    }
}
