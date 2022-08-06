import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleClass {
    public int foo(int a) {
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_1);
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_2);
        // commentary

        if (a % 2 == 0) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_3);
            a++;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_4);
        } else {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_5);
            a--;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_6);
        }

        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_7);
        if (a % 2 == 0) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_8);
            a++;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_9);
        }

        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_10);
        while (a > 0) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_11);
            a++;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_12);
        }

        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_13);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_14);
            a--;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_15);
        }

        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_16);
        do {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_17);
            a--;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_18);
        } while (a > 0);

        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_19;
        switch (a) {
            case 1:
                PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_20);
                a--;
                PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_21);
            case 2:
                PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_22);
                a++;
                PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_23);
                break;
            default:
                PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_24);
                a = 0;
                PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_25);
                return a;
        }
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_26);

        {
        }

        try {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_27);
            a++;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_28);
        } catch (RuntimeException e) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_29);
            a--;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_30);
        } catch (Exception e) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_31);
            a--;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_32);
        } finally {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_33);
            a = 1000;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_34);
        }
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_35);

        int[] arr = new int[]{1, 2, 3};
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_36);
        for (int b : arr) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_37);
            b++;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_38);
        }
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_39);

        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_40);
            i++;
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_41);
            break;
        }
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_42);

        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_43);
            continue;
        }
        PM.check(PMC.TRAP_SimpleClass_foo_argb_int_arge_44);

        return a;
    }

    public int terminator1(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator1_argb_int_arge_1);
        if (a == 0) {
            PM.check(PMC.TRAP_SimpleClass_terminator1_argb_int_arge_2);
            return a;
        } else {
            PM.check(PMC.TRAP_SimpleClass_terminator1_argb_int_arge_3);
            return a;
        }
    }

    public int terminator2(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator2_argb_int_arge_1);
        switch (a) {
            case 0:
                PM.check(PMC.TRAP_SimpleClass_terminator2_argb_int_arge_2);
                return a;
            case 1:
                PM.check(PMC.TRAP_SimpleClass_terminator2_argb_int_arge_3);
                return a;
            default:
                PM.check(PMC.TRAP_SimpleClass_terminator2_argb_int_arge_4);
                return a;
        }
    }

    public int terminator3(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator3_argb_int_arge_1);
        {
            PM.check(PMC.TRAP_SimpleClass_terminator3_argb_int_arge_2);
            return a;
        }
    }

    public int terminator4(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator4_argb_int_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_terminator4_argb_int_arge_2);
            return a;
        }
    }

    public int terminator5(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator5_argb_int_arge_1);
        while (a) {
            PM.check(PMC.TRAP_SimpleClass_terminator5_argb_int_arge_2);
            return a;
        }
    }

    public int terminator6(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator6_argb_int_arge_1);
        ISOException.throwIt(ISO7816.SW_NO_ERROR);
    }

    public int terminator7(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator7_argb_int_arge_1);
        try {
            PM.check(PMC.TRAP_SimpleClass_terminator7_argb_int_arge_2);
            return a;
        } catch (Exception) {
            PM.check(PMC.TRAP_SimpleClass_terminator7_argb_int_arge_3);
            return a;
        }
    }

    public int terminator8(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator8_argb_int_arge_1);
        if (a == 0) {
            PM.check(PMC.TRAP_SimpleClass_terminator8_argb_int_arge_2);
            return a;
        }
        PM.check(PMC.TRAP_SimpleClass_terminator8_argb_int_arge_3);
    }
}
