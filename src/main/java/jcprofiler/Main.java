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

        if ((args.dataRegex == null) == (args.dataFile == null)) {
            if (args.dataRegex == null)
                throw new RuntimeException("Neither --data-file or --data-regex option were specified!");
            throw new RuntimeException("Options --data-file or --data-regex cannot be specified simultaneously.");
        }

        if (args.startFrom.ordinal() > args.stopAfter.ordinal())
            throw new RuntimeException(String.format(
                    "Nothing to do! Cannot start with %s and end with %s.", args.startFrom, args.stopAfter));

        JCProfiler.run(args);
    }
}
