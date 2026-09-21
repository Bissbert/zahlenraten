import java.io.*;
import java.util.*;

/**
 * Verflucht.java - "Der Computer erraet deine Zahl", so unheilvoll wie moeglich.
 *
 * Spielregeln unveraendert: du denkst dir eine Zahl von 0 bis 99, antwortest mit
 * 1 = richtig, 2 = kleiner, 3 = groesser. Das Programm findet sie per Binaersuche.
 *
 * Bedingung: JEDE Fiesheit hier ist durch die Java Language Specification exakt
 * festgelegt. Kein undefiniertes Verhalten, keine JVM-Eigenheit, keine Reflection,
 * keine Race Condition. Alles laeuft auf jedem konformen Compiler gleich - es ist
 * nur zutiefst unanstaendig. Die einzelnen Fluechte sind unten nummeriert.
 *
 * Start:  java Verflucht.java        (Java 11+)
 */
public class Verflucht {

    /* ------------------------------------------------------------------
     * FLUCH 1 - Vorwaertsreferenz ueber den qualifizierten Namen.
     * Bei der Initialisierung von UNTEN ist OBEN noch nicht dran und traegt
     * seinen Default 0 (JLS 4.12.5 + 12.4.2). Der einfache Name "OBEN" waere
     * hier ein Compilerfehler, "Verflucht.OBEN" ist erlaubt (JLS 8.3.3).
     * Diese geliehene Null ist unsere Untergrenze.
     * ------------------------------------------------------------------ */
    static int UNTEN = Verflucht.OBEN;
    static int OBEN  = 99;

    /* FLUCH 2 - Integer-Cache. JLS 5.1.7 garantiert, dass Boxing fuer -128..127
     * immer dasselbe Objekt liefert. Deshalb - und NUR deshalb - darf man die
     * Antwortziffern unten mit == vergleichen. In jedem Code-Review waere das
     * ein sofortiger Ablehnungsgrund. Hier ist es beweisbar korrekt. */
    static final Integer EINS = 1, ZWEI = 2, DREI = 3;

    /* FLUCH 3 - Die Grenzen leben in einem short[]. Warum short? Weil (short)(0-1)
     * sauber -1 ergibt und Schummeln damit erkennbar bleibt. Mit dem naheliegenden
     * char[] waere daraus 65535 geworden und das Spiel haette ewig weitergeraten.
     * Der Typ ist also nicht Willkuer, sondern die Pointe. */
    static final short[] B = new short[2];

    /* FLUCH 4 - Im Quelltext steht kein einziges deutsches Wort. Alle Ausgaben
     * liegen als Hexdump da und werden erst zur Laufzeit zu Text. */
    static final String H =
        "44656e6b65206469722065696e65205a61686c207a7769736368656e203020756e6420393920756e64206472fc636b652064"
        + "616e6e20456e7465722e004f6b2e2049636820626567696e6e65206a65747a74207a7520726174656e2e00416e74776f7274"
        + "65206d6972206d697420312066fc7220726963687469672c20322066fc72206b6c65696e657220756e6420332066fc722067"
        + "72f6737365722e004973742065732064696520003f0044617320776172206a612065696e666163682100476566756e64656e"
        + "21004765736368616666742e005566662c2064617320776172207363687769657269672e0020566572737563686529002028"
        + "00486d6d2c206465696e6520416e74776f7274656e2070617373656e206e69636874207a7573616d6d656e2e204861737420"
        + "6475206765736368756d6d656c743f00537069656c656e20776972206e6f63686d616c3f20286a2f6e2900426973207a756d"
        + "206ee463687374656e204d616c210042697474652031202872696368746967292c203220286b6c65696e657229206f646572"
        + "203320286772f673736572292065696e676562656e2e003d3d3d20564552464c554348544552205241544552203d3d3d2076"
        + "6f6c6c20646566696e696572746573204a6176612c2074726f747a64656d2065696e65205a756d7574756e67002056657273"
        + "75636829"
;

    static PrintStream P;
    static String[] D;
    static Map<Integer, String> LOB;

    // ================= Aufbau =================
    static {
        try {
            P = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        D = entschluessle(H);

        /* FLUCH 5 - Unicode-Escapes werden laut JLS 3.3 ersetzt, BEVOR der Compiler
         * ueberhaupt Kommentare erkennt. Die naechste Zeile sieht aus wie ein
         * Kommentar, enthaelt aber ein Escape fuer den Zeilenumbruch (U+000A).
         * Nach der Ersetzung endet der Kommentar mittendrin und der Rest der
         * Zeile ist echter, ausfuehrbarer Code. Voll spezifiziert. Voll boese. */
        // ein voellig harmloser Kommentar, versprochen \u000a P.println(D[15]); P.println();

        /* FLUCH 6 - Double-Brace-Initialisierung: eine anonyme HashMap-Unterklasse,
         * deren Instanz-Initialisierer die Eintraege setzt. Erzeugt eine komplette
         * Extraklasse, nur um sich drei Zeilen Code zu sparen. */
        LOB = new HashMap<Integer, String>() {{
            put(1, D[5]); put(2, D[5]); put(3, D[5]);
            put(4, D[6]); put(5, D[6]);
            put(6, D[7]);
        }};

        /* FLUCH 7 - Der Abschiedsgruss kommt aus einem Shutdown-Hook. Das Programm
         * verabschiedet sich also technisch gesehen nach seinem eigenen Ende. */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> P.println(D[13])));
    }

    // ================= Das Spiel - komplett im Static-Initializer =================
    static {
        haupt:
        for (;;) {
            B[0] = (short) UNTEN;
            B[1] = (short) OBEN;

            P.println(D[0]);
            if (zeile() == null) break haupt;
            P.println();
            P.println(D[1]);
            P.println(D[2]);
            P.println();

            try {
                /* FLUCH 8 - Keine Schleife, keine benannte Methode, keine Rekursion
                 * im ueblichen Sinn: die Funktion bekommt sich selbst als Argument
                 * uebergeben (Y-Kombinator von Hand). Jeder Rateversuch ist ein
                 * eigener Stack-Frame. */
                Schleife s = (ich, egal) -> {
                    if (B[0] > B[1]) throw new Geschummelt();
                    int tipp = mitte(B[0], B[1]);
                    P.println(D[3] + tipp + D[4]);
                    try {
                        Antwort.werte(lies()).tu(tipp);   // RICHTIG wirft Gewonnen(0)
                        ich.los(ich, 0);
                    } catch (Gewonnen g) {
                        /* FLUCH 9 - Gezaehlt wird beim Abwickeln des Stacks: jede
                         * Ebene faengt den Sieg, erhoeht den Zaehler um eins und
                         * wirft ihn weiter nach oben. Die Anzahl der Versuche ist
                         * damit buchstaeblich die Aufrufstacktiefe. */
                        throw new Gewonnen(g.n + 1);
                    }
                };
                s.los(s, 0);
            } catch (Gewonnen g) {
                P.println(LOB.getOrDefault(g.n, D[8]) + D[10] + g.n + (g.n == 1 ? D[16] : D[9]));
            } catch (Geschummelt g) {
                P.println(D[11]);
            } catch (Ende e) {
                break haupt;
            }

            P.println();
            P.println(D[12]);
            String w = zeile();
            if (w == null) break haupt;
            w = w.trim().toLowerCase(Locale.ROOT);
            if (!(w.isEmpty() || w.startsWith("j") || w.startsWith("y"))) break haupt;
            P.println();
        }
    }

    /* FLUCH 10 - main ist leer. Die Klasseninitialisierung laeuft vor dem Aufruf
     * von main (JLS 12.4.1), das Spiel ist hier also laengst vorbei. */
    public static void main(String[] args) { }

    /* ------------------------------------------------------------------
     * FLUCH 11 - return im finally verschluckt das return im try. JLS 14.20.2
     * sagt das voellig klar, javac warnt trotzdem, und jeder Leser stolpert.
     * FLUCH 12 - Addiert wird durch Aneinanderreihen von Leerzeichen und
     * anschliessendes Messen der Laenge. Bis zu 198 Zeichen Muell pro Tipp,
     * nur um u + o auszurechnen.
     * ------------------------------------------------------------------ */
    static int mitte(int u, int o) {
        try {
            return Integer.MIN_VALUE;
        } finally {
            return (" ".repeat(u) + " ".repeat(o)).length() >>> 1;
        }
    }

    /* FLUCH 13 - Der enum ist die Verzweigung. Statt if/switch ueberschreibt
     * jede Konstante das Verhalten selbst, und "richtig" ist kein Rueckgabewert,
     * sondern ein geworfener Error. */
    enum Antwort {
        RICHTIG  { void tu(int t) { throw new Gewonnen(0); } },
        KLEINER  { void tu(int t) { B[1] = (short) (t - 1); } },
        GROESSER { void tu(int t) { B[0] = (short) (t + 1); } };

        abstract void tu(int tipp);

        static Antwort werte(Integer a) {
            return a == EINS ? RICHTIG : a == ZWEI ? KLEINER : GROESSER;   // siehe FLUCH 2
        }
    }

    interface Schleife { void los(Schleife ich, int egal); }

    /* FLUCH 14 - Steuerfluss als Error-Hierarchie, damit die Lambdas oben keine
     * checked exceptions deklarieren muessen. */
    static final class Gewonnen extends Error {
        final int n;
        Gewonnen(int n) { super(null, null, false, false); this.n = n; }
    }
    static final class Geschummelt extends Error {
        Geschummelt() { super(null, null, false, false); }
    }
    static final class Ende extends Error {
        Ende() { super(null, null, false, false); }
    }

    // ================= Eingabe =================
    static Integer lies() {
        /* FLUCH 15 - ein Label, das auf die Schleife zeigt, in der es steht, und
         * ein continue darauf als allerletzte Anweisung. Vollkommen wirkungslos
         * und trotzdem exakt definiert. */
        nochmal:
        for (;;) {
            String z = zeile();
            if (z == null) throw new Ende();
            z = z.trim();
            if (z.length() == 1) {
                Integer a = z.charAt(0) - '0';
                if (a == EINS || a == ZWEI || a == DREI) return a;
            }
            P.println(D[14]);
            continue nochmal;
        }
    }

    static String zeile() {
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

    static String[] entschluessle(String h) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < h.length(); i += 2)
            b.append((char) Integer.parseInt(h.substring(i, i + 2), 16));
        return b.toString().split("\u0000", -1);
    }
}
