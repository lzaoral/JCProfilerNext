import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleClass {
    public void assignment(int a) {
        PM.check(PMC.TRAP_SimpleClass_assignment_argb_int_arge_1);
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_assignment_argb_int_arge_2);
    }

    public int commentary(int a) {
        PM.check(PMC.TRAP_SimpleClass_commentary_argb_int_arge_1);
        // commentary
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_commentary_argb_int_arge_2);
    }

    public void ifElseStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifElseStatement_argb_int_arge_1);
        if (a % 2 == 0) {
            PM.check(PMC.TRAP_SimpleClass_ifElseStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_ifElseStatement_argb_int_arge_3);
        } else {
            PM.check(PMC.TRAP_SimpleClass_ifElseStatement_argb_int_arge_4);
            a--;
            PM.check(PMC.TRAP_SimpleClass_ifElseStatement_argb_int_arge_5);
        }
        PM.check(PMC.TRAP_SimpleClass_ifElseStatement_argb_int_arge_6);
    }

    public void ifStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifStatement_argb_int_arge_1);
        if (a % 2 == 0) {
            PM.check(PMC.TRAP_SimpleClass_ifStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_ifStatement_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_ifStatement_argb_int_arge_4);
    }

    public void whileStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_whileStatement_argb_int_arge_1);
        while (a > 0) {
            PM.check(PMC.TRAP_SimpleClass_whileStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_whileStatement_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_whileStatement_argb_int_arge_4);
    }

    public void forStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_forStatement_argb_int_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_forStatement_argb_int_arge_2);
            a--;
            PM.check(PMC.TRAP_SimpleClass_forStatement_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_forStatement_argb_int_arge_4);
    }

    public void doWhileStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_doWhileStatement_argb_int_arge_1);
        do {
            PM.check(PMC.TRAP_SimpleClass_doWhileStatement_argb_int_arge_2);
            a--;
            PM.check(PMC.TRAP_SimpleClass_doWhileStatement_argb_int_arge_3);
        } while (a > 0);
        PM.check(PMC.TRAP_SimpleClass_doWhileStatement_argb_int_arge_4);
    }

    public int switchStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_1;
        switch (a) {
            case 1:
                PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_2);
                a--;
                PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_3);
            case 2:
                PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_4);
                a++;
                PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_5);
                break;
            default:
                PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_6);
                a = 0;
                PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_7);
                return a;
        }
        PM.check(PMC.TRAP_SimpleClass_switchStatement_argb_int_arge_8);
    }

    public void emptyBlock(int a) {
        {
        }
        PM.check(PMC.TRAP_SimpleClass_emptyBlock_argb_int_arge_1);
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_emptyBlock_argb_int_arge_2);
    }

    public void multipleEmptyBlocks(int a) {
        {
            {
                {
                }
            }
        }
        PM.check(PMC.TRAP_SimpleClass_multipleEmptyBlocks_argb_int_arge_1);
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_multipleEmptyBlocks_argb_int_arge_2);
    }

    public int emptyBlockWithCommentary(int a) {
        {
            // commentary
        }
        PM.check(PMC.TRAP_SimpleClass_emptyBlockWithCommentary_argb_int_arge_1);
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_emptyBlockWithCommentary_argb_int_arge_2);
    }

    public void tryStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_1)
        try {
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_3);
        } catch (RuntimeException e) {
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_4);
            a--;
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_5);
        } catch (Exception e) {
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_6);
            a--;
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_7);
        } finally {
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_8);
            a = 1000;
            PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_9);
        }
        PM.check(PMC.TRAP_SimpleClass_tryStatement_argb_int_arge_10);
    }

    public void foreachStatement() {
        PM.check(PMC.TRAP_SimpleClass_foreachStatement_argb_arge_1);
        int[] arr = new int[]{1, 2, 3};
        PM.check(PMC.TRAP_SimpleClass_foreachStatement_argb_arge_2);
        for (int b : arr) {
            PM.check(PMC.TRAP_SimpleClass_foreachStatement_argb_arge_3);
            b++;
            PM.check(PMC.TRAP_SimpleClass_foreachStatement_argb_arge_4);
        }
        PM.check(PMC.TRAP_SimpleClass_foreachStatement_argb_arge_5);
    }

    public void forBreak() {
        PM.check(PMC.TRAP_SimpleClass_forBreak_argb_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_forBreak_argb_arge_2);
            break;
        }
        PM.check(PMC.TRAP_SimpleClass_forBreak_argb_arge_3);
    }

    public void forContinue() {
        PM.check(PMC.TRAP_SimpleClass_forContinue_argb_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_forContinue_argb_arge_2);
            continue;
        }
        PM.check(PMC.TRAP_SimpleClass_forContinue_argb_arge_3);
    }

    public void forNoBody() {
        PM.check(PMC.TRAP_SimpleClass_forNoBody_argb_arge_1);
        for (int i = 0; i != 1; i++);
        PM.check(PMC.TRAP_SimpleClass_forNoBody_argb_arge_2);
    }

    public void forEmptyBody() {
        PM.check(PMC.TRAP_SimpleClass_forEmptyBody_argb_arge_1);
        for (int i = 0; i != 1; i++) {}
        PM.check(PMC.TRAP_SimpleClass_forEmptyBody_argb_arge_2);
    }

    public void ifNoBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifNoBody_argb_int_arge_1);
        if (a != 1);
        PM.check(PMC.TRAP_SimpleClass_ifNoBody_argb_int_arge_2);
    }

    public void ifEmptyBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifEmptyBody_argb_int_arge_1);
        if (a != 1) {}
        PM.check(PMC.TRAP_SimpleClass_ifEmptyBody_argb_int_arge_2);
    }

    public void whileNoBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_whileNoBody_argb_int_arge_1);
        while (a != 1);
        PM.check(PMC.TRAP_SimpleClass_whileNoBody_argb_int_arge_2);
    }

    public void whileEmptyBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_whileEmptyBody_argb_int_arge_1);
        while (a != 1) {}
        PM.check(PMC.TRAP_SimpleClass_whileEmptyBody_argb_int_arge_2);
    }

    public void ifElseNoBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifElseNoBody_argb_int_arge_1);
        if (a != 1) {}
        PM.check(PMC.TRAP_SimpleClass_ifElseNoBody_argb_int_arge_2);
    }

    public void ifElseEmptyBody1(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifElseEmptyBody1_argb_int_arge_1);
        if (a != 1) {}
        else {}
        PM.check(PMC.TRAP_SimpleClass_ifElseEmptyBody1_argb_int_arge_2);
    }

    public void ifElseEmptyBody2(int a) {
        PM.check(PMC.TRAP_SimpleClass_ifElseEmptyBody2_argb_int_arge_1);
        if (a != 1) {
            PM.check(PMC.TRAP_SimpleClass_ifElseEmptyBody2_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_ifElseEmptyBody2_argb_int_arge_3);
        }
        else {}
        PM.check(PMC.TRAP_SimpleClass_ifElseEmptyBody2_argb_int_arge_4);
    }

    public void nonEmptyBlock(int a) {
        PM.check(PMC.TRAP_SimpleClass_nonEmptyBlock_argb_int_arge_1);
        {
            PM.check(PMC.TRAP_SimpleClass_nonEmptyBlock_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_nonEmptyBlock_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_nonEmptyBlock_argb_int_arge_4);
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

    public static int terminator9(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator9_argb_int_arge_1);
        terminator8(a);
        PM.check(PMC.TRAP_SimpleClass_terminator9_argb_int_arge_2);
    }

    public int terminator10(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator10_argb_int_arge_1);
        terminator9(a);
        PM.check(PMC.TRAP_SimpleClass_terminator10_argb_int_arge_2);
    }

    public int terminator11(int a) {
        PM.check(PMC.TRAP_SimpleClass_terminator11_argb_int_arge_1);
        switch (a) {
            case 0:
                PM.check(PMC.TRAP_SimpleClass_terminator11_argb_int_arge_2);
                break;
            case 1:
                PM.check(PMC.TRAP_SimpleClass_terminator11_argb_int_arge_3);
                return a;
            default:
                PM.check(PMC.TRAP_SimpleClass_terminator11_argb_int_arge_4);
                return a;
        }
        PM.check(PMC.TRAP_SimpleClass_terminator11_argb_int_arge_5);
    }

    public static class Nest {
        int nest(int a) {
            PM.check(PMC.TRAP_SimpleClass_dol_Nest_nest_argb_int_arge_1);
            a++;
            PM.check(PMC.TRAP_SimpleClass_dol_Nest_nest_argb_int_arge_2);
        }
    }
}
