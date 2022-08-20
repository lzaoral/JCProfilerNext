package jcprofiler.compilation;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Echo;

import pro.javacard.ant.JavaCard;
import pro.javacard.ant.JavaCard.JCApplet;
import pro.javacard.ant.JavaCard.JCCap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.declaration.CtClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Compiler {
    private static final Logger log = LoggerFactory.getLogger(Compiler.class);

    // static class
    private Compiler() {}

    // Inspired by https://stackoverflow.com/questions/6440295/is-it-possible-to-call-ant-or-nsis-scripts-from-java-code/6440342#6440342
    public static void compile(final Args args, final CtClass<?> entryPoint) {
        // create the output directory if it does not exist
        final Path appletDir = JCProfilerUtil.getAppletOutputDirectory(args.workDir);
        try {
            if (Files.exists(appletDir)) {
                FileUtils.deleteDirectory(appletDir.toFile());
                log.debug("Deleted existing {}.", appletDir);
            }

            Files.createDirectories(appletDir);
            log.debug("Created {}.", appletDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.debug("Generating ANT JavaCard project");
        final Project p = new Project();
        p.init();
        p.setName("JavaCard");
        p.setBaseDir(appletDir.toFile());

        /*
         TODO: make it possible to select the JDK that is used to compile the project if possible
         Right now we use the compiler that's part of the JDK that was used to execute this project.
         */

        final DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(args.debug ? Project.MSG_VERBOSE : Project.MSG_INFO);
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
        cap.setClasses("classes");
        cap.setSources(JCProfilerUtil.getInstrOutputDirectory(args.workDir).toString());
        cap.setExport(".");

        // TODO: check if this ok
        cap.setDebug(false);
        cap.setStrip(false);

        cap.setAID(JCProfilerUtil.APPLET_AID);
        cap.setJca(entryPoint.getPackage().getSimpleName() + ".jca");
        cap.setPackage(entryPoint.getPackage().getQualifiedName());
        cap.setOutput(entryPoint.getSimpleName() + ".cap");

        final JCApplet app = cap.createApplet();
        app.setClass(entryPoint.getQualifiedName());

        log.debug("Compiling into {}.cap", entryPoint.getSimpleName());
        p.executeTarget(p.getDefaultTarget());
    }
}
