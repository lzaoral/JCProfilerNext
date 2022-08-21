package jcprofiler.installation;

import apdu4j.BIBO;
import apdu4j.CardBIBO;
import apdu4j.TerminalManager;
import pro.javacard.gp.GPTool;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import cz.muni.fi.crocs.rcard.client.RunConfig;
import cz.muni.fi.crocs.rcard.client.Util;

import javacard.framework.Applet;

import jcprofiler.args.Args;
import jcprofiler.util.JCProfilerUtil;
import jcprofiler.util.Stage;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.declaration.CtClass;

import javax.smartcardio.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

public class Installer {
    private static final Logger log = LoggerFactory.getLogger(Installer.class);
    private static final byte[] APPLET_AID = Util.hexStringToByteArray(JCProfilerUtil.PACKAGE_AID);

    // static class
    private Installer() {}

    public static CardManager installOnCard(final Args args, final CtClass<?> entryPoint) {
        if (args.useSimulator)
            throw new RuntimeException("Installation on a simulator is not possible");

        final CardManager cardManager = connectToCard();

        final GPTool gp = new GPTool();
        final BIBO bibo = CardBIBO.wrap(cardManager.getChannel().getCard());
        final Path capPath = JCProfilerUtil.getAppletOutputDirectory(args.workDir)
                .resolve(entryPoint.getSimpleName() + ".cap");
        JCProfilerUtil.checkFile(capPath, Stage.compilation);

        String[] gpArgv = new String[]{"--verbose", "--force", "--install", capPath.toString()};
        if (args.debug)
            gpArgv = ArrayUtils.add(gpArgv, "--debug");

        // TODO: be very careful to not destroy the card!!!
        log.info("Executing GlobalPlatformPro to install {}.", capPath);
        log.debug("GlobalPlatformPro argv: {}", Arrays.toString(gpArgv));
        int ret = gp.run(bibo, gpArgv);
        if (ret != 0)
            throw new RuntimeException(String.format("GlobalPlatformPro exited with non-zero code: %d", ret));

        try {
            log.info("Selecting installed applet on card.");
            ResponseAPDU out = cardManager.selectApplet();
            if (out.getSW() != JCProfilerUtil.SW_NO_ERROR)
                throw new CardException("Applet could not se selected. SW: " + out.getSW());
        } catch (CardException e) {
            throw new RuntimeException(e);
        }

        return cardManager;
    }

    public static CardManager connect(final Args args, final CtClass<?> entryPoint) {
        return args.useSimulator ? configureSimulator(args, entryPoint)
                                 : connectToCard();
    }

    private static CardManager configureSimulator(final Args args, final CtClass<?> entryPoint) {
        log.info("Configuring jCardSim simulator.");

        final Path jarPath = JCProfilerUtil.getAppletOutputDirectory(args.workDir)
                        .resolve(entryPoint.getPackage().getSimpleName() + ".jar");
        JCProfilerUtil.checkFile(jarPath, Stage.compilation);
        final CardManager cardManager = new CardManager(true, APPLET_AID);

        try {
            log.debug("Loading {} from {}.", entryPoint.getQualifiedName(), jarPath);

            // FIXME: this leak is intentional so that the simulator can access every class in the loaded JAR
            final URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()});
            final Class<? extends Applet> cls = classLoader.loadClass(entryPoint.getQualifiedName())
                    .asSubclass(Applet.class);
            final RunConfig runCfg = RunConfig.getDefaultConfig()
                    .setTestCardType(CardType.JCARDSIMLOCAL)
                    .setAppletToSimulate(cls)
                    .setbReuploadApplet(true)
                    .setInstallData(new byte[8]);

            // Simulator may print unrelated messages to stdout during initialization (happens with JCMathLib)
            PrintStream stdout = System.out;
            System.setOut(new PrintStream(NULL_OUTPUT_STREAM));

            log.debug("Connecting to jCardSim simulator.");
            if (!cardManager.connect(runCfg))
                throw new RuntimeException("Setting-up the simulator failed");

            System.setOut(stdout);
            return cardManager;
        } catch (ClassNotFoundException | CardException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CardManager connectToCard() {
        log.info("Connecting to a physical card reader.");
        final CardManager cardManager = new CardManager(true, APPLET_AID);

        // for better portability across different platforms
        TerminalManager.fixPlatformPaths();
        final CardTerminals terminals = TerminalManager.getTerminalFactory().terminals();
        log.info("Looking for terminal with a card.");

        try {
            List<CardTerminal> terminalList = terminals.list(CardTerminals.State.CARD_PRESENT);
            while (terminalList.isEmpty()) {
                log.warn("No connected terminals with a card found!");
                log.info("Waiting for a terminal with a card.");
                terminals.waitForChange();
                terminalList = terminals.list(CardTerminals.State.CARD_PRESENT);
            }

            int terminalIdx = 0;
            if (terminalList.size() > 1)
                terminalIdx = selectTerminal(terminalList);

            final CardTerminal terminal = terminalList.get(terminalIdx);
            log.info("Connecting to a card in terminal {}.", terminal.getName());
            cardManager.connectTerminal(terminal);
            log.info("Successfully connected.");
            log.info("Card ATR: {}", Util.bytesToHex(cardManager.getChannel().getCard().getATR().getBytes()));

            return cardManager;
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }

    private static int selectTerminal(final List<CardTerminal> terminalList) {
        while (true) {
            System.out.println("More card terminals found. Please, select one:");

            int i = 0;
            for (final CardTerminal ct : terminalList)
                System.out.printf("%d. %s%n", i++, ct.getName());

            Scanner in = new Scanner(System.in);
            try {
                int idx = in.nextInt();
                if (0 <= idx && idx < terminalList.size())
                    return idx;
                System.err.println("Invalid index entered!");
            } catch (InputMismatchException e) {
                System.err.println("Invalid input format!");
            }
        }
    }
}
