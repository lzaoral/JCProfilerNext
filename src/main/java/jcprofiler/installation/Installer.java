package jcprofiler.installation;

import apdu4j.BIBO;
import apdu4j.CardBIBO;
import apdu4j.TerminalManager;
import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import cz.muni.fi.crocs.rcard.client.RunConfig;
import cz.muni.fi.crocs.rcard.client.Util;
import javacard.framework.Applet;
import jcprofiler.args.Args;
import pro.javacard.gp.GPTool;
import pro.javacard.gp.ISO7816;
import spoon.reflect.declaration.CtClass;

import javax.smartcardio.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Installer {
    // static class
    private Installer() {}

    public static CardManager install(final Args args, final CtClass<?> entryPoint) {
        return args.use_simulator ? configureSimulator(args, entryPoint)
                                  : installOnCard(args, entryPoint);
    }

    private static CardManager configureSimulator(final Args args, final CtClass<?> entryPoint) {
        final Path jarPath = Paths.get(args.outputDir, "bin", entryPoint.getPackage().getSimpleName() + ".jar");
        final CardManager cardManager = new CardManager(false, Util.hexStringToByteArray("123456789001"));

        try {
            final URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()});
            final Class<? extends Applet> cls = classLoader.loadClass(entryPoint.getQualifiedName())
                    .asSubclass(Applet.class);
            final RunConfig runCfg = RunConfig.getDefaultConfig()
                    .setTestCardType(CardType.JCARDSIMLOCAL)
                    .setAppletToSimulate(cls)
                    .setbReuploadApplet(true)
                    .setInstallData(new byte[8]);

            // TODO: simulator may print unrelated messages to stdout
            if (!cardManager.connect(runCfg))
                throw new RuntimeException("Setting-up the simulator failed");

            return cardManager;
        } catch (ClassNotFoundException | CardException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CardManager installOnCard(final Args args, final CtClass<?> entryPoint) {
        final Path capPath = Paths.get(args.outputDir, entryPoint.getSimpleName() + ".cap");
        final CardManager cardManager = new CardManager(false, Util.hexStringToByteArray("123456789001"));
        // we do it manually later after we have successfully installed the applet
        cardManager.setDoSelect(false);

        try {
            final Card card = connectToCard(cardManager);

            final GPTool gp = new GPTool();
            final BIBO bibo = CardBIBO.wrap(card);

            // TODO: be very careful to not destroy the card!!!
            int ret;
            if ((ret = gp.run(bibo, new String[]{"-v", "--force", "--install", capPath.toString()})) != 0)
                throw new RuntimeException(String.format("GlobalPlatformPro exited with non-zero code: %d", ret));

            System.out.print("Selecting installed applet on card...");
            ResponseAPDU out = cardManager.selectApplet();
            if (out.getSW() != ISO7816.SW_NO_ERROR)
                throw new CardException("Applet could not se selected. SW: " + out.getSW());
            System.out.println("Done");

            return cardManager;
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }

    private static Card connectToCard(final CardManager cardManager) throws CardException {
        // for better portability across different platforms
        TerminalManager.fixPlatformPaths();
        final CardTerminals terminals = TerminalManager.getTerminalFactory().terminals();
        System.out.println("Waiting for terminal with a card ...");

        List<CardTerminal> terminalList;
        do {
            terminals.waitForChange();
            terminalList = terminals.list(CardTerminals.State.CARD_PRESENT);
        } while (terminalList.isEmpty());

        int terminalIdx = 0;
        if (terminalList.size() > 2)
            terminalIdx = selectTerminal(terminalList);

        final CardTerminal terminal = terminalList.get(terminalIdx);
        System.out.println("Connecting card in terminal " + terminal.getName());

        final Card card = cardManager.connectTerminal(terminal).getCard();
        System.out.println("Connected!");
        return card;
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
