import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleClass {
    public void assignment(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_assignment_argb_int_arge_1);
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_assignment_argb_int_arge_2);
    }

    public void lineCommentary(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_lineCommentary_argb_int_arge_1);
        // line commentary
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_lineCommentary_argb_int_arge_2);
    }

    public void lineCommentaryOnly() {
        PM.check(PMC.TRAP_SimpleClass_hash_lineCommentaryOnly_argb_arge_1);
        // line commentary only
    }

    public void lineCommentaryTwice(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_lineCommentaryTwice_argb_int_arge_1);
        // line commentary 1
        // line commentary 2
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_lineCommentaryTwice_argb_int_arge_2);
    }

    public void lineCommentaryOnlyTwice() {
        PM.check(PMC.TRAP_SimpleClass_hash_lineCommentaryOnlyTwice_argb_arge_1);
        // line commentary 1
        // line commentary 2
    }

    public void blockCommentary(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_blockCommentary_argb_int_arge_1);
        /* block
           commentary
         */
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_blockCommentary_argb_int_arge_2);
    }

    public void blockCommentaryOnly() {
        PM.check(PMC.TRAP_SimpleClass_hash_blockCommentaryOnly_argb_arge_1);
        /* block
           commentary
         */
    }

    public void blockCommentaryTwice(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_blockCommentaryTwice_argb_int_arge_1);
        /* block
           commentary 1
        */
        /* block
           commentary 2
        */
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_blockCommentaryTwice_argb_int_arge_2);
    }

    public void blockCommentaryOnlyTwice() {
        PM.check(PMC.TRAP_SimpleClass_hash_blockCommentaryOnlyTwice_argb_arge_1);
        /* block
           commentary 1
         */
        /* block
           commentary 2
         */
    }

    public void javadocCommentary(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_javadocCommentary_argb_int_arge_1);
        /**
         * javadoc
         * commentary
         */
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_javadocCommentary_argb_int_arge_2);
    }

    public void javadocCommentaryOnly() {
        PM.check(PMC.TRAP_SimpleClass_hash_javadocCommentaryOnly_argb_arge_1);
        /**
         * javadoc
         * commentary
         */
    }

    public void javadocCommentaryTwice(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_javadocCommentaryTwice_argb_int_arge_1);
        /**
         * javadoc
         * commentary 1
         */
        /**
         * javadoc
         * commentary 2
         */
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_javadocCommentaryTwice_argb_int_arge_2);
    }

    public void javadocCommentaryOnlyTwice() {
        PM.check(PMC.TRAP_SimpleClass_hash_javadocCommentaryOnlyTwice_argb_arge_1);
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
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseStatement_argb_int_arge_1);
        if (a % 2 == 0) {
            PM.check(PMC.TRAP_SimpleClass_hash_ifElseStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_hash_ifElseStatement_argb_int_arge_3);
        } else {
            PM.check(PMC.TRAP_SimpleClass_hash_ifElseStatement_argb_int_arge_4);
            a--;
            PM.check(PMC.TRAP_SimpleClass_hash_ifElseStatement_argb_int_arge_5);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseStatement_argb_int_arge_6);
    }

    public void ifStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_ifStatement_argb_int_arge_1);
        if (a % 2 == 0) {
            PM.check(PMC.TRAP_SimpleClass_hash_ifStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_hash_ifStatement_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_ifStatement_argb_int_arge_4);
    }

    public void whileStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_whileStatement_argb_int_arge_1);
        while (a > 0) {
            PM.check(PMC.TRAP_SimpleClass_hash_whileStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_hash_whileStatement_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_whileStatement_argb_int_arge_4);
    }

    public void forStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_forStatement_argb_int_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_hash_forStatement_argb_int_arge_2);
            a--;
            PM.check(PMC.TRAP_SimpleClass_hash_forStatement_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_forStatement_argb_int_arge_4);
    }

    public void doWhileStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_doWhileStatement_argb_int_arge_1);
        do {
            PM.check(PMC.TRAP_SimpleClass_hash_doWhileStatement_argb_int_arge_2);
            a--;
            PM.check(PMC.TRAP_SimpleClass_hash_doWhileStatement_argb_int_arge_3);
        } while (a > 0);
        PM.check(PMC.TRAP_SimpleClass_hash_doWhileStatement_argb_int_arge_4);
    }

    public int switchStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_1;
        switch (a) {
            case 1:
                PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_2);
                a--;
                PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_3);
            case 2:
                PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_4);
                a++;
                PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_5);
                break;
            default:
                PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_6);
                a = 0;
                PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_7);
                return a;
        }
        PM.check(PMC.TRAP_SimpleClass_hash_switchStatement_argb_int_arge_8);
        return a;
    }

    public void emptyBlock(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_emptyBlock_argb_int_arge_1);
        {
        }
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_emptyBlock_argb_int_arge_2);
    }

    public void multipleEmptyBlocks(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_multipleEmptyBlocks_argb_int_arge_1);
        {
            {
                {
                }
            }
        }
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_multipleEmptyBlocks_argb_int_arge_2);
    }

    public void emptyBlockWithCommentary(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_emptyBlockWithCommentary_argb_int_arge_1);
        {
            // commentary
        }
        a = 1;
        PM.check(PMC.TRAP_SimpleClass_hash_emptyBlockWithCommentary_argb_int_arge_2);
    }

    public void stetementWithEmptyBlockWithCommentary(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_stetementWithEmptyBlockWithCommentary_argb_int_arge_1);
        if (a == 1) {
            PM.check(PMC.TRAP_SimpleClass_hash_stetementWithEmptyBlockWithCommentary_argb_int_arge_2);
            {
                // commentary
            }
            a = 1;
            PM.check(PMC.TRAP_SimpleClass_hash_stetementWithEmptyBlockWithCommentary_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_stetementWithEmptyBlockWithCommentary_argb_int_arge_4);
    }

    public void tryStatement(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_1)
        try {
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_3);
        } catch (RuntimeException e) {
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_4);
            a--;
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_5);
        } catch (Exception e) {
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_6);
            a--;
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_7);
        } finally {
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_8);
            a = 1000;
            PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_9);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_tryStatement_argb_int_arge_10);
    }

    public void foreachStatement() {
        PM.check(PMC.TRAP_SimpleClass_hash_foreachStatement_argb_arge_1);
        int[] arr = new int[]{1, 2, 3};
        PM.check(PMC.TRAP_SimpleClass_hash_foreachStatement_argb_arge_2);
        for (int b : arr) {
            PM.check(PMC.TRAP_SimpleClass_hash_foreachStatement_argb_arge_3);
            b++;
            PM.check(PMC.TRAP_SimpleClass_hash_foreachStatement_argb_arge_4);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_foreachStatement_argb_arge_5);
    }

    public void forBreak() {
        PM.check(PMC.TRAP_SimpleClass_hash_forBreak_argb_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_hash_forBreak_argb_arge_2);
            break;
        }
        PM.check(PMC.TRAP_SimpleClass_hash_forBreak_argb_arge_3);
    }

    public void forContinue() {
        PM.check(PMC.TRAP_SimpleClass_hash_forContinue_argb_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_hash_forContinue_argb_arge_2);
            continue;
        }
        PM.check(PMC.TRAP_SimpleClass_hash_forContinue_argb_arge_3);
    }

    public void forNoBody() {
        PM.check(PMC.TRAP_SimpleClass_hash_forNoBody_argb_arge_1);
        for (int i = 0; i != 1; i++);
        PM.check(PMC.TRAP_SimpleClass_hash_forNoBody_argb_arge_2);
    }

    public void forEmptyBody() {
        PM.check(PMC.TRAP_SimpleClass_hash_forEmptyBody_argb_arge_1);
        for (int i = 0; i != 1; i++) {}
        PM.check(PMC.TRAP_SimpleClass_hash_forEmptyBody_argb_arge_2);
    }

    public void ifNoBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_ifNoBody_argb_int_arge_1);
        if (a != 1);
        PM.check(PMC.TRAP_SimpleClass_hash_ifNoBody_argb_int_arge_2);
    }

    public void ifEmptyBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_ifEmptyBody_argb_int_arge_1);
        if (a != 1) {}
        PM.check(PMC.TRAP_SimpleClass_hash_ifEmptyBody_argb_int_arge_2);
    }

    public void whileNoBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_whileNoBody_argb_int_arge_1);
        while (a != 1);
        PM.check(PMC.TRAP_SimpleClass_hash_whileNoBody_argb_int_arge_2);
    }

    public void whileEmptyBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_whileEmptyBody_argb_int_arge_1);
        while (a != 1) {}
        PM.check(PMC.TRAP_SimpleClass_hash_whileEmptyBody_argb_int_arge_2);
    }

    public void ifElseNoBody(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseNoBody_argb_int_arge_1);
        if (a != 1) {}
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseNoBody_argb_int_arge_2);
    }

    public void ifElseEmptyBody1(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseEmptyBody1_argb_int_arge_1);
        if (a != 1) {}
        else {}
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseEmptyBody1_argb_int_arge_2);
    }

    public void ifElseEmptyBody2(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseEmptyBody2_argb_int_arge_1);
        if (a != 1) {
            PM.check(PMC.TRAP_SimpleClass_hash_ifElseEmptyBody2_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_hash_ifElseEmptyBody2_argb_int_arge_3);
        }
        else {}
        PM.check(PMC.TRAP_SimpleClass_hash_ifElseEmptyBody2_argb_int_arge_4);
    }

    public void nonEmptyBlock(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_nonEmptyBlock_argb_int_arge_1);
        {
            PM.check(PMC.TRAP_SimpleClass_hash_nonEmptyBlock_argb_int_arge_2);
            a++;
            PM.check(PMC.TRAP_SimpleClass_hash_nonEmptyBlock_argb_int_arge_3);
        }
        PM.check(PMC.TRAP_SimpleClass_hash_nonEmptyBlock_argb_int_arge_4);
    }

    public void empty() {
        PM.check(PMC.TRAP_SimpleClass_hash_empty_argb_arge_1);
    }

    public void emptyWithEmptyBlocks() {
        PM.check(PMC.TRAP_SimpleClass_hash_emptyWithEmptyBlocks_argb_arge_1);
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
        PM.check(PMC.TRAP_SimpleClass_hash_terminator1_argb_int_arge_1);
        if (a == 0) {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator1_argb_int_arge_2);
            return a;
        } else {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator1_argb_int_arge_3);
            return a;
        }
    }

    public int terminator2(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator2_argb_int_arge_1);
        switch (a) {
            case 0:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator2_argb_int_arge_2);
                return a;
            case 1:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator2_argb_int_arge_3);
                return a;
            default:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator2_argb_int_arge_4);
                return a;
        }
    }

    public int terminator3(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator3_argb_int_arge_1);
        {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator3_argb_int_arge_2);
            return a;
        }
    }

    public int terminator4(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator4_argb_int_arge_1);
        for (int i = 0; i != 1; i++) {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator4_argb_int_arge_2);
            return a;
        }
    }

    public int terminator5(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator5_argb_int_arge_1);
        while (true) {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator5_argb_int_arge_2);
            return a;
        }
    }

    public void terminator6(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator6_argb_int_arge_1);
        ISOException.throwIt(ISO7816.SW_NO_ERROR);
    }

    public int terminator7(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator7_argb_int_arge_1);
        try {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator7_argb_int_arge_2);
            return a;
        } catch (Exception) {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator7_argb_int_arge_3);
            return a;
        }
    }

    public int terminator8(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator8_argb_int_arge_1);
        if (a == 0) {
            PM.check(PMC.TRAP_SimpleClass_hash_terminator8_argb_int_arge_2);
            return a;
        }
        PM.check(PMC.TRAP_SimpleClass_hash_terminator8_argb_int_arge_3);
        return a;
    }

    public static void terminator9(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator9_argb_int_arge_1);
        terminator8(a);
        PM.check(PMC.TRAP_SimpleClass_hash_terminator9_argb_int_arge_2);
    }

    public void terminator10(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator10_argb_int_arge_1);
        terminator9(a);
        PM.check(PMC.TRAP_SimpleClass_hash_terminator10_argb_int_arge_2);
    }

    public int terminator11(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator11_argb_int_arge_1);
        switch (a) {
            case 0:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator11_argb_int_arge_2);
                break;
            case 1:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator11_argb_int_arge_3);
                return a;
            default:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator11_argb_int_arge_4);
                return a;
        }
        PM.check(PMC.TRAP_SimpleClass_hash_terminator11_argb_int_arge_5);
        return a;
    }

    public int terminator12(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator12_argb_int_arge_1);
        switch (a) {
            case 0:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator12_argb_int_arge_2);
                return a;
            default:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator12_argb_int_arge_3);
                if (a == 0) {
                    PM.check(PMC.TRAP_SimpleClass_hash_terminator12_argb_int_arge_4);
                    return a;
                } else {
                    PM.check(PMC.TRAP_SimpleClass_hash_terminator12_argb_int_arge_5);
                    return a;
                }
        }
    }

    public void terminator13(int a) {
        PM.check(PMC.TRAP_SimpleClass_hash_terminator13_argb_int_arge_1);
        switch (a) {
            case 1:
                PM.check(PMC.TRAP_SimpleClass_hash_terminator13_argb_int_arge_2);
                {
                    PM.check(PMC.TRAP_SimpleClass_hash_terminator13_argb_int_arge_3);
                    a++;
                    PM.check(PMC.TRAP_SimpleClass_hash_terminator13_argb_int_arge_4);
                    break;
                }
        }
        PM.check(PMC.TRAP_SimpleClass_hash_terminator13_argb_int_arge_5);
    }

    public static class Nest {
        void nest(int a) {
            PM.check(PMC.TRAP_SimpleClass_dol_Nest_hash_nest_argb_int_arge_1);
            a++;
            PM.check(PMC.TRAP_SimpleClass_dol_Nest_hash_nest_argb_int_arge_2);
        }
    }
}
