import java.io.*;
import java.util.*;

/**
 * Verflucht.java ("cursed") - "the computer guesses your number", as ominous
 * as it gets.
 *
 * The rules are unchanged: you think of a number from 0 to 99 and answer with
 * 1 = correct, 2 = smaller, 3 = larger. The program finds it by binary search.
 *
 * The constraint: EVERY nastiness in here is exactly pinned down by the Java
 * Language Specification. No undefined behaviour, no JVM quirk, no reflection,
 * no race condition. All of it behaves the same on every conforming compiler -
 * it is merely deeply indecent. The individual curses are numbered below.
 *
 * Start:  java Verflucht.java        (Java 11+)
 */
public class Verflucht {

    /* ------------------------------------------------------------------
     * CURSE 1 - Forward reference through the qualified name.
     * While LOW is being initialised, HIGH has not had its turn yet and still
     * carries its default 0 (JLS 4.12.5 + 12.4.2). The simple name "HIGH"
     * would be a compile error here; "Verflucht.HIGH" is allowed (JLS 8.3.3).
     * That borrowed zero is our lower bound.
     * ------------------------------------------------------------------ */
    static int LOW  = Verflucht.HIGH;
    static int HIGH = 99;

    /* CURSE 2 - The Integer cache. JLS 5.1.7 guarantees that boxing -128..127
     * always yields the same object. That - and ONLY that - is why the answer
     * digits below may be compared with ==. In any code review this would be
     * an instant rejection. Here it is provably correct. */
    static final Integer ONE = 1, TWO = 2, THREE = 3;

    /* CURSE 3 - The bounds live in a short[]. Why short? Because (short)(0-1)
     * cleanly gives -1, which keeps cheating detectable. With the obvious
     * char[] it would have become 65535 and the game would have gone on
     * guessing forever. The type is not arbitrary, it is the punchline. */
    static final short[] B = new short[2];

    /* CURSE 4 - There is not one English word in this source file. Every piece
     * of output sits here as a hex dump and only becomes text at runtime. */
    static final String H =
        "5468696e6b206f662061206e756d626572206265747765656e203020616e642039392c207468656e20707265737320456e74"
        + "65722e004f6b2e2049276c6c207374617274206775657373696e67206e6f772e00416e73776572206d652077697468203120"
        + "666f7220636f72726563742c203220666f7220736d616c6c657220616e64203320666f72206c61726765722e004973206974"
        + "20003f0057656c6c2c20746861742077617320656173792100466f756e642069742100476f742069742e00506865772c2074"
        + "6861742077617320747269636b792e0020677565737365732900202800486d6d2c20796f757220616e737765727320646f6e"
        + "2774206164642075702e2044696420796f752063686561743f005368616c6c20776520706c617920616761696e3f2028792f"
        + "6e290053656520796f75206e6578742074696d652100506c6561736520656e74657220312028636f7272656374292c203220"
        + "28736d616c6c657229206f72203320286c6172676572292e003d3d3d204355525345442047554553534552203d3d3d206675"
        + "6c6c7920646566696e6564204a6176612c20616e64207374696c6c20616e20696d706f736974696f6e0020677565737329";

    static PrintStream P;
    static String[] D;
    static Map<Integer, String> PRAISE;

    // ================= setup =================
    static {
        try {
            P = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        D = decode(H);

        /* CURSE 5 - Per JLS 3.3, Unicode escapes are replaced BEFORE the compiler
         * even recognises comments. The next line looks like a comment, but it
         * contains an escape for a line break (U+000A). After the replacement the
         * comment ends halfway through and the rest of the line is real,
         * executable code. Fully specified. Fully evil. */
        // a completely harmless comment, promise \u000a P.println(D[15]); P.println();

        /* CURSE 6 - Double-brace initialisation: an anonymous HashMap subclass
         * whose instance initialiser fills in the entries. Creates an entire
         * extra class just to save three lines of code. */
        PRAISE = new HashMap<Integer, String>() {{
            put(1, D[5]); put(2, D[5]); put(3, D[5]);
            put(4, D[6]); put(5, D[6]);
            put(6, D[7]);
        }};

        /* CURSE 7 - The farewell comes out of a shutdown hook. Technically, the
         * program says goodbye after its own end. */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> P.println(D[13])));
    }

    // ================= the game - entirely inside a static initialiser =================
    static {
        game:
        for (;;) {
            B[0] = (short) LOW;
            B[1] = (short) HIGH;

            P.println(D[0]);
            if (line() == null) break game;
            P.println();
            P.println(D[1]);
            P.println(D[2]);
            P.println();

            try {
                /* CURSE 8 - No loop, no named method, no recursion in the usual
                 * sense: the function is handed itself as an argument (a
                 * hand-rolled Y combinator). Every guess is its own stack frame. */
                Loop s = (self, ignored) -> {
                    if (B[0] > B[1]) throw new Cheated();
                    int guess = middle(B[0], B[1]);
                    P.println(D[3] + guess + D[4]);
                    try {
                        Answer.of(read()).act(guess);   // CORRECT throws Won(0)
                        self.go(self, 0);
                    } catch (Won g) {
                        /* CURSE 9 - The counting happens while the stack unwinds:
                         * every level catches the win, adds one and throws it
                         * further up. The number of guesses is literally the
                         * depth of the call stack. */
                        throw new Won(g.n + 1);
                    }
                };
                s.go(s, 0);
            } catch (Won g) {
                P.println(PRAISE.getOrDefault(g.n, D[8]) + D[10] + g.n + (g.n == 1 ? D[16] : D[9]));
            } catch (Cheated g) {
                P.println(D[11]);
            } catch (End e) {
                break game;
            }

            P.println();
            P.println(D[12]);
            String w = line();
            if (w == null) break game;
            w = w.trim().toLowerCase(Locale.ROOT);
            if (!(w.isEmpty() || w.startsWith("y") || w.startsWith("j"))) break game;
            P.println();
        }
    }

    /* CURSE 10 - main is empty. Class initialisation runs before main is called
     * (JLS 12.4.1), so by this point the game is long over. */
    public static void main(String[] args) { }

    /* ------------------------------------------------------------------
     * CURSE 11 - A return inside finally swallows the return in try. JLS 14.20.2
     * says so perfectly clearly, javac warns about it anyway, and every reader
     * trips over it.
     * CURSE 12 - Addition is performed by concatenating spaces and then
     * measuring the length. Up to 198 characters of garbage per guess, purely
     * to work out u + o.
     * ------------------------------------------------------------------ */
    static int middle(int u, int o) {
        try {
            return Integer.MIN_VALUE;
        } finally {
            return (" ".repeat(u) + " ".repeat(o)).length() >>> 1;
        }
    }

    /* CURSE 13 - The enum IS the branch. Instead of if/switch, each constant
     * overrides the behaviour itself, and "correct" is not a return value but a
     * thrown Error. */
    enum Answer {
        CORRECT { void act(int t) { throw new Won(0); } },
        SMALLER { void act(int t) { B[1] = (short) (t - 1); } },
        LARGER  { void act(int t) { B[0] = (short) (t + 1); } };

        abstract void act(int guess);

        static Answer of(Integer a) {
            return a == ONE ? CORRECT : a == TWO ? SMALLER : LARGER;   // see CURSE 2
        }
    }

    interface Loop { void go(Loop self, int ignored); }

    /* CURSE 14 - Control flow as an Error hierarchy, so that the lambdas above
     * need not declare any checked exceptions. */
    static final class Won extends Error {
        final int n;
        Won(int n) { super(null, null, false, false); this.n = n; }
    }
    static final class Cheated extends Error {
        Cheated() { super(null, null, false, false); }
    }
    static final class End extends Error {
        End() { super(null, null, false, false); }
    }

    // ================= input =================
    static Integer read() {
        /* CURSE 15 - a label pointing at the very loop it sits in, and a continue
         * to it as the very last statement. Completely without effect and
         * nevertheless exactly defined. */
        retry:
        for (;;) {
            String z = line();
            if (z == null) throw new End();
            z = z.trim();
            if (z.length() == 1) {
                Integer a = z.charAt(0) - '0';
                if (a == ONE || a == TWO || a == THREE) return a;
            }
            P.println(D[14]);
            continue retry;
        }
    }

    static String line() {
        try {
            StringBuilder b = new StringBuilder();
            for (int c; (c = System.in.read()) != -1; ) {
                if (c == '\n') return b.toString();
                if (c != '\r') b.append((char) c);
            }
            return b.length() == 0 ? null : b.toString();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    static String[] decode(String h) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < h.length(); i += 2)
            b.append((char) Integer.parseInt(h.substring(i, i + 2), 16));
        return b.toString().split("\u0000", -1);
    }
}
