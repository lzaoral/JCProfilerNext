import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleClass {
    public int foo(int a) {
        a = 1;
        // commentary

        if (a % 2 == 0)
            a++;
        else
            a--;

        if (a % 2 == 0)
            a++;

        while (a > 0)
            a++;

        for (int i = 0; i != 1; i++)
            a--;

        do
            a--;
        while (a > 0);

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

        {
        }

        try {
            a++;
        } catch (RuntimeException e) {
            a--;
        } catch (Exception e) {
            a--;
        } finally {
            a = 1000;
        }

        int[] arr = new int[]{1, 2, 3};
        for (int b : arr) {
            b++;
        }

        for (int i = 0; i != 1; i++) {
            i++;
            break;
        }

        for (int i = 0; i != 1; i++)
            continue;

        for (int i = 0; i != 1; i++);

        for (int i = 0; i != 1; i++) {}

        if (a != 1);

        if (a != 1) {}

        while (a != 1);

        while (a != 1) {}

        if (a != 1) {}
        else;

        if (a != 1) {}
        else {}

        {
            a++;
        }

        return a;
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
}
