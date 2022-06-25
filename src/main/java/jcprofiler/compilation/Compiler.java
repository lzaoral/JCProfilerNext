package jcprofiler.compilation;

import jcprofiler.args.Args;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Echo;
import pro.javacard.ant.JavaCard;
import pro.javacard.ant.JavaCard.JCApplet;
import pro.javacard.ant.JavaCard.JCCap;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.nio.file.Paths;

public class Compiler {
    // static class
    private Compiler() {}

    // Inspired by https://stackoverflow.com/questions/6440295/is-it-possible-to-call-ant-or-nsis-scripts-from-java-code/6440342#6440342
    public static void compile(final Args args, final CtClass<?> entryPoint) {
        final Project p = new Project();
        p.init();
        p.setName("JavaCard");
        p.setBaseDir(new File(args.outputDir));

        /*
         TODO: make it possible to select the JDK that is used to compile the project if possible
         Right now we use the compiler that's part of the JDK that was used to execute this project.
         */

        final DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        p.addBuildListener(consoleLogger);

        final Target jcTarget = new Target();
        jcTarget.setName("javacard");
        p.addTarget(jcTarget);
        p.setDefault("javacard");

        final Echo echo = new Echo();
        echo.setTaskName("echo");
        echo.setProject(p);
        echo.setMessage("Java version: " + p.getProperty("java.version"));
        jcTarget.addTask(echo);

        final JavaCard jc = new JavaCard();
        jc.setTaskName("JavaCard");
        jc.setProject(p);
        jc.setJCKit(args.jcSDK.getRoot().getAbsolutePath());
        jcTarget.addTask(jc);

        final JCCap cap = jc.createCap();
        cap.setTaskName("JavaCard");
        cap.setProject(p);
        cap.setSources(Paths.get(args.outputDir, "sources").toString());
        cap.setExport("bin");

        // TODO: check if this ok
        cap.setDebug(false);
        cap.setStrip(false);

        cap.setAID("1234567890");
        cap.setPackage(entryPoint.getPackage().getQualifiedName());
        cap.setOutput(String.format("%s.cap", entryPoint.getSimpleName()));

        final JCApplet app = cap.createApplet();
        app.setClass(entryPoint.getQualifiedName());

        p.executeTarget(p.getDefaultTarget());
    }
}
