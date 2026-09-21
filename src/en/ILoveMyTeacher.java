import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*
 *  ILoveMyTeacher.java
 *  ------------------------------------------------------------------------
 *  Solves the bonus exercise: the computer guesses the number the human is
 *  thinking of (0 to 99), answers 1 = correct, 2 = smaller, 3 = larger.
 *
 *  Start:   java ILoveMyTeacher.java
 *
 *  Every single construction in this file is completely pinned down by the
 *  Java Language Specification or by the API documentation. There is no
 *  undefined behaviour here, no JVM quirk, no reflection into JDK internals,
 *  no dependence on time, threads or the platform encoding. The program
 *  behaves exactly the same on every conforming Java implementation. It is
 *  an imposition regardless.
 *
 *  The file is pure ASCII, contains not one word of the output language -
 *  and implements no search either.
 * ------------------------------------------------------------------------
 */
public class ILoveMyTeacher {

    /* CURSE 1 --------------------------------------------------------------
     * The file name is the decryption key. Class.getSimpleName() is exactly
     * specified for a top-level class. Rename the file and you get gibberish
     * instead of English.
     */
    static final String KEY = ILoveMyTeacher.class.getSimpleName();

    /* CURSE 2 --------------------------------------------------------------
     * Lower bound of the game. Math.abs(Integer.MIN_VALUE) IS, per javadoc,
     * Integer.MIN_VALUE, and MIN_VALUE + MIN_VALUE is guaranteed by JLS
     * 15.18.2 to wrap in two's complement to exactly 0.
     */
    static final int LOW = Math.abs(Integer.MIN_VALUE) + Integer.MIN_VALUE;

    /* CURSE 3 --------------------------------------------------------------
     * Upper bound of the game. String.hashCode is exactly defined in the API,
     * and "Aa" and "BB" both come to 2112 - the difference is guaranteed to be
     * 0 on every implementation. 0143 is an octal literal (JLS 3.10.1) and
     * means 99.
     */
    static final int HIGH = "Aa".hashCode() - "BB".hashCode() + 0143;

    /* CURSE 4 --------------------------------------------------------------
     * A nested class named System shadows java.lang.System (JLS 6.4.1). From
     * here on every "System.out.println" in this file is a call into something
     * else entirely - and java.lang.System is only reachable qualified.
     */
    static final class System {
        static final Out out = new Out();
        static final BufferedReader in = new BufferedReader(
            new InputStreamReader(java.lang.System.in, StandardCharsets.UTF_8));
        static final class Out {
            private final PrintStream s = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
            void println()         { s.println(); }
            void println(Object o) { s.println(o); }
        }
    }

    /* CURSE 5 --------------------------------------------------------------
     * The entire text output sits as a hex dump inside a text block (JLS
     * 3.10.6), XOR-encrypted with CURSE 1. Not one word of the output appears
     * in the source.
     */
    static final String SECRET = """
            1d2406180e6d163245004306101f2b291d5607280d2300040d48555228220b565c7455741109060645023b291c054508
            172000134d682a19676c2651092159271100111c45153c291c050c231e740b0e14466533273f1813176d143145160a1c
            0d52786c0919176d1a3b1713060b115e697e4f100a3f592708000f040000692d0112457e59320a13430404002e291d58
            65040a740c1543685a721e29031a496d0d3c0415431f040169290e051c6c79130a154301115c491c0713126159200d00
            174812133a6c1b040c2e122d4b61434065522e390a0516280a7d65290e05495230231a04452c17271204111b45162622
            4802452c1d3045141346453620284f0f0a3859370d04021c5a7219200a17162859310b15061a454369640c19173f1c37
            11484f485752613f021709211c264c410c1a454169640317172a1c264c4f633b0d1325204f01006d0938041843090213
            202250564d34563a4c61300d005230231a560b28012045150a0500534971524b450c59331004101b0c1c2e6c1f040a2a
            0b3508411700040669280013166d173b1141100d04002a2443560c395935160a1048584f744c3b1e04395e2745000f05
            0a013d6c0e0411241f3d06080204451b27380a1a09241e310b020646652026390112166d09380418060c5f52491d1a13
            1639103b0b12430916192c284f1f0b6d0d3b11000f524572692b1a13163e50
            """;

    static final String[] D = decode(SECRET);

    static String[] decode(String h) {
        String x = h.replaceAll("\\s", "");
        StringBuilder b = new StringBuilder();
        for (int i = 0, k = 0; i < x.length(); i += 2, k++)
            b.append((char) (Integer.parseInt(x.substring(i, i + 2), 16)
                             ^ KEY.charAt(k % KEY.length())));
        return b.toString().split("\u0000", -1);
    }

    /* CURSE 6 --------------------------------------------------------------
     * The next block comment is not one. A Unicode escape for U+002A followed
     * by U+002F is translated per JLS 3.3 BEFORE lexing and terminates the
     * comment right there. The error class below appears to sit inside a
     * comment and is compiled code all the same.
     */
    /* From here on it is only prose, definitely no more code. */
    static final class End extends Error {
        private static final long serialVersionUID = 1L;
        End() { super(null, null, false, false); }
    }
    /* Right, and now back to prose. */

    /* CURSE 7 --------------------------------------------------------------
     * Homoglyphs. Once the Unicode escapes have been translated, the two
     * identifiers below look identical - both render as "a". They are two
     * different variables all the same: Latin U+0061 and Cyrillic U+0430.
     * Both are valid Java letters (JLS 3.8).
     */
    static short a = 0;                 // questions in this round
    static int а = 0;              // questions in total
    static int rounds = 0;

    /* CURSE 8 --------------------------------------------------------------
     * The answers as an enum with an abstract method. The declaration order IS
     * the protocol from the exercise sheet: CORRECT = 1, SMALLER = 2,
     * LARGER = 3. The return value is at the same time the comparison result
     * for CURSE 10.
     */
    enum Answer {
        CORRECT { int value() { return  0; } },
        SMALLER { int value() { return  1; } },
        LARGER  { int value() { return -1; } };
        abstract int value();
    }

    /* CURSE 9 --------------------------------------------------------------
     * A list with no contents: get(i) computes, size() computes. A hundred
     * numbers, zero bytes of payload. RandomAccess makes sure Collections
     * takes the indexed route.
     */
    static final class Universe extends AbstractList<Integer> implements RandomAccess {
        public Integer get(int i) { return i; }
        public int size()         { return HIGH - LOW + 1; }
    }

    /* CURSE 10 -------------------------------------------------------------
     * The curse proper: this program implements no search. It calls
     * Collections.binarySearch on a list without data, looking for the key
     * null - and the comparator asks the human on every single comparison.
     * The human is the data structure.
     *
     * The documented contract of the method returns the index of the number
     * when the answers are consistent, and otherwise a negative insertion
     * point. Cheat detection thus falls out for free.
     */
    static final Comparator<Integer> ORACLE = (guess, nobody) -> {
        a += 1;                         // CURSE 11: short += int, implicit narrowing (JLS 15.26.2)
        а++;
        System.out.println(D[3] + guess + D[4]);
        return ask().value();
    };

    /* CURSE 12 -------------------------------------------------------------
     * A static call through a null reference. The qualifier is evaluated and
     * the result discarded; a NullPointerException is guaranteed not to happen
     * (JLS 15.12.4.1).
     */
    static final ILoveMyTeacher NOBODY = null;

    /* CURSE 13 -------------------------------------------------------------
     * The next line comment ends in the middle of the line, because a Unicode
     * escape for U+000A becomes a real line break per JLS 3.3 - before the
     * lexer even sees the comment. Everything after the escape is code.
     */
    // No more code follows here, only a remark by the author. \u000a    static void title() { System.out.println(D[14]); System.out.println(); }

    /* CURSE 14 -------------------------------------------------------------
     * Overload resolution as a prize ceremony. praise(a) with a short a is
     * forced in phase 1 (neither boxing nor varargs, JLS 15.12.2.2) to pick
     * the long variant; the Integer variant is only reachable through explicit
     * boxing, the varargs variant only through an explicit array cast. Which
     * praise you get is therefore decided not by the value but by the
     * resolution phase of the language.
     */
    static String praise(long n)      { return D[5]; }
    static String praise(Integer n)   { return D[6]; }
    static String praise(Object... n) { return D[7]; }

    /* CURSE 15 -------------------------------------------------------------
     * A label on a loop that does not need one, plus a "continue" to that
     * label which changes exactly nothing.
     */
    static Answer ask() {
        retry:
        for (;;) {
            String z = line();
            if (z == null) throw new End();
            z = z.trim();
            if (z.length() == 1 && z.charAt(0) >= '1' && z.charAt(0) <= '3')
                return Answer.values()[digit(z) - 1];
            System.out.println(D[11]);
            continue retry;
        }
    }

    /* CURSE 16 -------------------------------------------------------------
     * A return inside finally discards the in-flight exception completely
     * (JLS 14.20.2). The AssertionError never arrives anywhere.
     */
    static int digit(String z) {
        try { throw new AssertionError(D[10]); }
        finally { return z.charAt(0) - '0'; }
    }

    static String line() {
        try { return System.in.readLine(); } catch (IOException e) { return null; }
    }

    static void round() {
        a = 0;
        /* CURSE 17 ----------------------------------------------------------
         * "var" is not a keyword but a reserved type name (JLS 3.9) - as a
         * variable name it is allowed.
         */
        int var = Collections.binarySearch(new Universe(), (Integer) null, ORACLE);
        System.out.println();
        if (var < LOW) { System.out.println(D[10]); return; }
        System.out.println(
            (a <= 5 ? praise(a)
                    : a <= 6 ? praise(Integer.valueOf(a))
                             : praise((Object[]) new Object[]{ a }))
            + D[8] + a + (a == 1 ? D[18] : D[9]));
        System.out.println(D[15]);
    }

    /* CURSE 18 -------------------------------------------------------------
     * switch over a String is, by specification, hashCode-based with a
     * subsequent equals check. "Aa" and "BB" have the same hashCode - both may
     * nevertheless appear in the same switch and each works correctly. Two
     * secret words for yes.
     */
    static boolean again() {
        System.out.println();
        System.out.println(D[12]);
        String z = line();
        if (z == null) throw new End();
        switch (z.trim()) {
            case "": case "y": case "Y": case "yes": case "Yes":
            case "j": case "J": case "ja": case "Aa": case "BB":
                return true;
            default:
                return false;
        }
    }

    /* CURSE 19 -------------------------------------------------------------
     * main is empty. The whole game runs in the static initialiser, which per
     * JLS 12.4.1 is executed before main.
     *
     * CURSE 20: a labelled block that is left with break - Java's only legal
     * goto.
     *
     * CURSE 21: the farewell hangs in a shutdown hook, and the thanks to the
     * teacher is assembled at runtime out of the class's own name.
     *
     * CURSE 22: if(false) is expressly allowed (conditional compilation, JLS
     * 14.21) - while(false) would be an error because of unreachable code. The
     * line is compiled, would fail if it ran, and is guaranteed never to run.
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println(D[16] + rounds);
            System.out.println(D[17] + а);
            System.out.println(D[13]);
            System.out.println(String.join(" ", KEY.split("(?=\\p{Lu})")).trim());
        }));

        run: {
            if (false) System.out.println(D[0].substring(HIGH * HIGH));

            NOBODY.title();
            System.out.println(D[0]);
            if (line() == null) break run;
            System.out.println();
            System.out.println(D[1]);
            System.out.println(D[2]);
            try {
                do {
                    rounds++;
                    System.out.println();
                    round();
                } while (again());
            } catch (End e) {
                break run;
            }
        }
    }

    public static void main(String[] args) { }
}
