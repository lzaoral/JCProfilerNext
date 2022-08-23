import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleClass {
    public void assignment(int a) {
        a = 1;
    }

    public int commentary(int a) {
        // commentary
        a = 1;
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
    }

    public int emptyBlock(int a) {
        {
        }
        a = 1;
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

    public void ifElseEmptyBody(int a) {
        if (a != 1) {}
        else {}
    }

    public void nonEmptyBlock(int a) {
        {
            a++;
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
        while (a)
            return a;
    }

    public int terminator6(int a) {
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
    }

    public static int terminator9(int a) {
        terminator8(a);
    }

    public int terminator10(int a) {
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
    }

    public static class Nest {
        int nest(int a) {
            a++;
        }
    }
}
