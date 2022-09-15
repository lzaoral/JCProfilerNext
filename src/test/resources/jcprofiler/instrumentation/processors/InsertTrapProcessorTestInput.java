import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleClass {
    public void assignment(int a) {
        a = 1;
    }

    public void lineCommentary(int a) {
        // line commentary
        a = 1;
    }

    public void lineCommentaryOnly() {
        // line commentary only
    }

    public void lineCommentaryTwice(int a) {
        // line commentary 1
        // line commentary 2
        a = 1;
    }

    public void lineCommentaryOnlyTwice() {
        // line commentary 1
        // line commentary 2
    }

    public void blockCommentary(int a) {
        /* block
           commentary
         */
        a = 1;
    }

    public void blockCommentaryOnly() {
        /* block
           commentary
         */
    }

    public void blockCommentaryTwice(int a) {
        /* block
           commentary 1
        */
        /* block
           commentary 2
        */
        a = 1;
    }

    public void blockCommentaryOnlyTwice() {
        /* block
           commentary 1
         */
        /* block
           commentary 2
         */
    }

    public void javadocCommentary(int a) {
        /**
         * javadoc
         * commentary
         */
        a = 1;
    }

    public void javadocCommentaryOnly() {
        /**
         * javadoc
         * commentary
         */
    }

    public void javadocCommentaryTwice(int a) {
        /**
         * javadoc
         * commentary 1
         */
        /**
         * javadoc
         * commentary 2
         */
        a = 1;
    }

    public void javadocCommentaryOnlyTwice() {
        /**
         * javadoc
         * commentary 1
         */
        /**
         * javadoc
         * commentary 2
         */
    }

    public void ifElseStatement(int a) {
        if (a % 2 == 0)
            a++;
        else
            a--;
    }

    public void ifStatement(int a) {
        if (a % 2 == 0)
            a++;
    }

    public void whileStatement(int a) {
        while (a > 0)
            a++;
    }

    public void forStatement(int a) {
        for (int i = 0; i != 1; i++)
            a--;
    }

    public void doWhileStatement(int a) {
        do
            a--;
        while (a > 0);
    }

    public int switchStatement(int a) {
        switch (a) {
            case 1:
                a--;
            case 2:
                a++;
                break;
            default:
                a = 0;
                return a;
        }
        return a;
    }

    public void emptyBlock(int a) {
        {
        }
        a = 1;
    }

    public void multipleEmptyBlocks(int a) {
        {
            {
                {
                }
            }
        }
        a = 1;
    }

    public void emptyBlockWithCommentary(int a) {
        {
            // commentary
        }
        a = 1;
    }

    public void stetementWithEmptyBlockWithCommentary(int a) {
        if (a == 1) {
            {
                // commentary
            }
            a = 1;
        }
    }

    public void tryStatement(int a) {
        try {
            a++;
        } catch (RuntimeException e) {
            a--;
        } catch (Exception e) {
            a--;
        } finally {
            a = 1000;
        }
    }

    public void foreachStatement() {
        int[] arr = new int[]{1, 2, 3};
        for (int b : arr) {
            b++;
        }
    }

    public void forBreak() {
        for (int i = 0; i != 1; i++)
            break;
    }

    public void forContinue() {
        for (int i = 0; i != 1; i++)
            continue;
    }

    public void forNoBody() {
        for (int i = 0; i != 1; i++);
    }

    public void forEmptyBody() {
        for (int i = 0; i != 1; i++) {}
    }

    public void ifNoBody(int a) {
        if (a != 1);
    }

    public void ifEmptyBody(int a) {
        if (a != 1) {}
    }

    public void whileNoBody(int a) {
        while (a != 1);
    }

    public void whileEmptyBody(int a) {
        while (a != 1) {}
    }

    public void ifElseNoBody(int a) {
        if (a != 1) {}
        else;
    }

    public void ifElseEmptyBody1(int a) {
        if (a != 1) {}
        else {}
    }

    public void ifElseEmptyBody2(int a) {
        if (a != 1) {
            a++
        } else {}
    }

    public void nonEmptyBlock(int a) {
        {
            a++;
        }
    }

    public void empty() {}

    public void emptyWithEmptyBlocks() {
        {
            {
                {
                }
                {
                    // commentary
                }
            }
        }
    }

    public int terminator1(int a) {
        if (a == 0)
            return a;
        else
            return a;
    }

    public int terminator2(int a) {
        switch (a) {
            case 0:
                return a;
            case 1:
                return a;
            default:
                return a;
        }
    }

    public int terminator3(int a) {
        {
            return a;
        }
    }

    public int terminator4(int a) {
        for (int i = 0; i != 1; i++)
            return a;
    }

    public int terminator5(int a) {
        while (true)
            return a;
    }

    public void terminator6(int a) {
        ISOException.throwIt(ISO7816.SW_NO_ERROR);
    }

    public int terminator7(int a) {
        try {
            return a;
        } catch (Exception) {
            return a;
        }
    }

    public int terminator8(int a) {
        if (a == 0)
            return a;
        return a;
    }

    public static void terminator9(int a) {
        terminator8(a);
    }

    public void terminator10(int a) {
        terminator9(a);
    }

    public int terminator11(int a) {
        switch (a) {
            case 0:
                break;
            case 1:
                return a;
            default:
                return a;
        }
        return a;
    }

    public int terminator12(int a) {
        switch (a) {
            case 0:
                return a;
            default:
                if (a == 0)
                    return a;
                else
                    return a;
        }
    }

    public static class Nest {
        void nest(int a) {
            a++;
        }
    }
}
