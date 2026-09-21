import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*
 *  ILoveMyTeacher.java
 *  ------------------------------------------------------------------------
 *  Loest die Zusatzaufgabe: der Computer erraet die Zahl, die sich der
 *  Mensch gedacht hat (0 bis 99), Antworten 1 = richtig, 2 = kleiner,
 *  3 = groesser.
 *
 *  Start:   java ILoveMyTeacher.java
 *
 *  Jede einzelne Konstruktion in dieser Datei ist durch die Java Language
 *  Specification bzw. die API-Dokumentation vollstaendig festgelegt. Es gibt
 *  hier kein undefiniertes Verhalten, keine JVM-Eigenheit, keine Reflexion in
 *  JDK-Interna, keine Abhaengigkeit von Zeit, Threads oder Plattform-Encoding.
 *  Das Programm verhaelt sich auf jeder konformen Java-Implementierung exakt
 *  gleich. Es ist trotzdem eine Zumutung.
 *
 *  Die Datei ist reines ASCII und enthaelt kein einziges deutsches Wort in
 *  der Ausgabe-Sprache - und implementiert auch keine Suche.
 * ------------------------------------------------------------------------
 */
public class ILoveMyTeacher {

    /* FLUCH 1 --------------------------------------------------------------
     * Der Dateiname ist der Entschluesselungsschluessel. Class.getSimpleName()
     * ist fuer eine Top-Level-Klasse exakt festgelegt. Wer die Datei umbenennt,
     * bekommt Kauderwelsch statt Deutsch.
     */
    static final String SCHLUESSEL = ILoveMyTeacher.class.getSimpleName();

    /* FLUCH 2 --------------------------------------------------------------
     * Untergrenze des Spiels. Math.abs(Integer.MIN_VALUE) IST laut javadoc
     * Integer.MIN_VALUE, und MIN_VALUE + MIN_VALUE laeuft nach JLS 15.18.2 im
     * Zweierkomplement garantiert auf exakt 0 ueber.
     */
    static final int UNTEN = Math.abs(Integer.MIN_VALUE) + Integer.MIN_VALUE;

    /* FLUCH 3 --------------------------------------------------------------
     * Obergrenze des Spiels. String.hashCode ist in der API exakt definiert,
     * und "Aa" und "BB" haben beide den Wert 2112 - die Differenz ist auf
     * jeder Implementierung garantiert 0. 0143 ist ein Oktalliteral (JLS
     * 3.10.1) und bedeutet 99.
     */
    static final int OBEN = "Aa".hashCode() - "BB".hashCode() + 0143;

    /* FLUCH 4 --------------------------------------------------------------
     * Eine eigene Klasse namens System verdeckt java.lang.System (JLS 6.4.1).
     * Ab hier ist jedes "System.out.println" in dieser Datei ein Aufruf von
     * etwas voellig anderem - und java.lang.System nur noch qualifiziert
     * erreichbar.
     */
    static final class System {
        static final Aus out = new Aus();
        static final BufferedReader in = new BufferedReader(
            new InputStreamReader(java.lang.System.in, StandardCharsets.UTF_8));
        static final class Aus {
            private final PrintStream s = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
            void println()         { s.println(); }
            void println(Object o) { s.println(o); }
        }
    }

    /* FLUCH 5 --------------------------------------------------------------
     * Die gesamte Textausgabe steckt als Hexdump in einem Textblock (JLS
     * 3.10.6), XOR-verschluesselt mit FLUCH 1. Im Quelltext steht kein
     * einziges Wort der Ausgabe.
     */
    static final String GEHEIM = """
            0d29011d006d1d3d174106010b1769160e1e096d03230c120000001c697c4f030b29596d5c41160601522d3e93150e28
            5930040f0d48201c3d291d586502127a4528000045102c2b06180b28593e0015191c45083c6c1d171128177a65200d1c
            121d3b380a5608240b740808174854522fb01d5617241a3c110804444540692a9304452615310c0f061a450727284f45
            452b85264506119e16012c3e41762c3e0d740012430c0c17694c5076212c0a74120011480f1369290618032c1a3c4461
            240d1611212d09101163790103074f4801133a6c1817176d0a370d160a0d171b2e626f564d4d59020013101d061a2c65
            6f3e0820557401040a06005208221b010a3f0d310b41130916012c224f180c2e1120451b161b041f2429015845051827
            1141071d45152c3f0c1e1020143109155c68271b3d380a56546d51260c020b1c0c1560604f444565123800080d0d175b
            69230b13176d4a744d06119e16012c3e465600241733000306064b721a3c061309281774120811480b1d2a2402170972
            597c0f4e0d416530203f4f0c1020593a81020b1b1117276c2217096c7969585c432d0c1c691e0e02003d0b3b02130205
            085e69280e05452310370d15104816072a241b5a453e163a0104110645143b2d0802457044696525021b45153b29010c
            116d0a370d0e0d4803133a384f170b6d12a80b1217040c1121294f3f0b391c380908040d0b08674c2813163d10310915
            0648370727280a185f6d791d0b12040d161324384f11003e0d31090d170d45343b2d08130b7759544537061a16072a24
            46
            """;

    static final String[] D = entschluessle(GEHEIM);

    static String[] entschluessle(String h) {
        String x = h.replaceAll("\\s", "");
        StringBuilder b = new StringBuilder();
        for (int i = 0, k = 0; i < x.length(); i += 2, k++)
            b.append((char) (Integer.parseInt(x.substring(i, i + 2), 16)
                             ^ SCHLUESSEL.charAt(k % SCHLUESSEL.length())));
        return b.toString().split("\u0000", -1);
    }

    /* FLUCH 6 --------------------------------------------------------------
     * Der naechste Blockkommentar ist keiner. Ein Unicode-Escape fuer U+002A
     * gefolgt von U+002F wird nach JLS 3.3 VOR dem Lexen uebersetzt und
     * beendet den Kommentar an Ort und Stelle. Die Fehlerklasse unten steht
     * scheinbar in einem Kommentar und ist trotzdem compilierter Code.
     */
    /* Ab hier steht nur noch Prosa, ganz bestimmt kein Code mehr. \u002a\u002f
    static final class Ende extends Error {
        private static final long serialVersionUID = 1L;
        Ende() { super(null, null, false, false); }
    }
    /* So, und jetzt wieder Prosa. */

    /* FLUCH 7 --------------------------------------------------------------
     * Homoglyphen. Nach der Uebersetzung der Unicode-Escapes sehen die beiden
     * folgenden Bezeichner identisch aus - beide rendern als "a". Es sind aber
     * zwei verschiedene Variablen: lateinisch U+0061 und kyrillisch U+0430.
     * Beides sind gueltige Java-Buchstaben (JLS 3.8).
     */
    static short a = 0;                 // Fragen in dieser Runde
    static int \u0430 = 0;              // Fragen insgesamt
    static int runden = 0;

    /* FLUCH 8 --------------------------------------------------------------
     * Die Antworten als enum mit abstrakter Methode. Die Deklarationsreihen-
     * folge IST das Protokoll vom Aufgabenblatt: RICHTIG = 1, KLEINER = 2,
     * GROESSER = 3. Der Rueckgabewert ist zugleich schon das Vergleichs-
     * ergebnis fuer FLUCH 10.
     */
    enum Antwort {
        RICHTIG  { int wert() { return  0; } },
        KLEINER  { int wert() { return  1; } },
        GROESSER { int wert() { return -1; } };
        abstract int wert();
    }

    /* FLUCH 9 --------------------------------------------------------------
     * Eine Liste ohne Inhalt: get(i) rechnet, size() rechnet. Hundert Zahlen,
     * null Bytes Nutzdaten. RandomAccess sorgt dafuer, dass Collections den
     * indizierten Weg nimmt.
     */
    static final class Weltall extends AbstractList<Integer> implements RandomAccess {
        public Integer get(int i) { return i; }
        public int size()         { return OBEN - UNTEN + 1; }
    }

    /* FLUCH 10 -------------------------------------------------------------
     * Der eigentliche Fluch: dieses Programm implementiert keine Suche.
     * Es ruft Collections.binarySearch auf einer Liste ohne Daten auf, sucht
     * dort den Schluessel null - und der Comparator fragt bei jedem einzelnen
     * Vergleich den Menschen. Der Mensch ist die Datenstruktur.
     *
     * Der dokumentierte Vertrag der Methode liefert den Index der Zahl, wenn
     * die Antworten konsistent sind, und sonst einen negativen Einfuegepunkt.
     * Die Schummelerkennung faellt damit gratis ab.
     */
    static final Comparator<Integer> ORAKEL = (tipp, niemand) -> {
        a += 1;                         // FLUCH 11: short += int, implizite Verengung (JLS 15.26.2)
        \u0430++;
        System.out.println(D[3] + tipp + D[4]);
        return frage().wert();
    };

    /* FLUCH 12 -------------------------------------------------------------
     * Statischer Aufruf ueber eine null-Referenz. Der Qualifizierer wird
     * ausgewertet und das Ergebnis verworfen; eine NullPointerException gibt
     * es garantiert nicht (JLS 15.12.4.1).
     */
    static final ILoveMyTeacher NIEMAND = null;

    /* FLUCH 13 -------------------------------------------------------------
     * Der naechste Zeilenkommentar endet mitten in der Zeile, weil ein
     * Unicode-Escape fuer U+000A nach JLS 3.3 ein echter Zeilenumbruch wird -
     * schon bevor der Lexer den Kommentar ueberhaupt sieht. Alles nach dem
     * Escape ist Code.
     */
    // Hier folgt kein Code mehr, nur noch eine Anmerkung des Autors. \u000a    static void titel() { System.out.println(D[14]); System.out.println(); }

    /* FLUCH 14 -------------------------------------------------------------
     * Ueberladungsaufloesung als Preisverleihung. lob(a) mit short a waehlt
     * in Phase 1 (weder Boxing noch varargs, JLS 15.12.2.2) zwingend die
     * long-Variante; die Integer-Variante ist nur ueber explizites Boxen
     * erreichbar, die varargs-Variante nur ueber einen expliziten Array-Cast.
     * Welches Lob man bekommt, entscheidet also nicht der Wert, sondern die
     * Aufloesungsphase der Sprache.
     */
    static String lob(long n)      { return D[5]; }
    static String lob(Integer n)   { return D[6]; }
    static String lob(Object... n) { return D[7]; }

    /* FLUCH 15 -------------------------------------------------------------
     * Eine Marke an einer Schleife, die sie nicht braucht, plus ein
     * "continue" auf diese Marke, das exakt gar nichts aendert.
     */
    static Antwort frage() {
        nochmal:
        for (;;) {
            String z = zeile();
            if (z == null) throw new Ende();
            z = z.trim();
            if (z.length() == 1 && z.charAt(0) >= '1' && z.charAt(0) <= '3')
                return Antwort.values()[ziffer(z) - 1];
            System.out.println(D[11]);
            continue nochmal;
        }
    }

    /* FLUCH 16 -------------------------------------------------------------
     * Ein return im finally verwirft die fliegende Ausnahme restlos
     * (JLS 14.20.2). Der AssertionError kommt nie irgendwo an.
     */
    static int ziffer(String z) {
        try { throw new AssertionError(D[10]); }
        finally { return z.charAt(0) - '0'; }
    }

    static String zeile() {
        try { return System.in.readLine(); } catch (IOException e) { return null; }
    }

    static void runde() {
        a = 0;
        /* FLUCH 17 ----------------------------------------------------------
         * "var" ist kein Schluesselwort, sondern ein reservierter Typname
         * (JLS 3.9) - als Variablenname ist er erlaubt.
         */
        int var = Collections.binarySearch(new Weltall(), (Integer) null, ORAKEL);
        System.out.println();
        if (var < UNTEN) { System.out.println(D[10]); return; }
        System.out.println(
            (a <= 5 ? lob(a)
                    : a <= 6 ? lob(Integer.valueOf(a))
                             : lob((Object[]) new Object[]{ a }))
            + D[8] + a + (a == 1 ? D[18] : D[9]));
        System.out.println(D[15]);
    }

    /* FLUCH 18 -------------------------------------------------------------
     * switch ueber String ist per Spezifikation hashCode-basiert mit
     * anschliessender equals-Pruefung. "Aa" und "BB" haben denselben
     * hashCode - beide duerfen trotzdem im selben switch stehen und
     * funktionieren einzeln korrekt. Zwei geheime Ja-Woerter.
     */
    static boolean weiter() {
        System.out.println();
        System.out.println(D[12]);
        String z = zeile();
        if (z == null) throw new Ende();
        switch (z.trim()) {
            case "": case "j": case "J": case "ja": case "Ja":
            case "y": case "Y": case "yes": case "Aa": case "BB":
                return true;
            default:
                return false;
        }
    }

    /* FLUCH 19 -------------------------------------------------------------
     * main ist leer. Das ganze Spiel laeuft im statischen Initialisierer, der
     * nach JLS 12.4.1 vor main ausgefuehrt wird.
     *
     * FLUCH 20: ein markierter Block, aus dem mit break herausgesprungen wird
     * - Javas einziges legales goto.
     *
     * FLUCH 21: der Abschied haengt in einem Shutdown-Hook, und der Dank an
     * die Lehrperson wird zur Laufzeit aus dem eigenen Klassennamen
     * zusammengesetzt.
     *
     * FLUCH 22: if(false) ist ausdruecklich erlaubt (bedingte Kompilierung,
     * JLS 14.21) - while(false) waere ein Fehler wegen unerreichbaren Codes.
     * Die Zeile wird uebersetzt, wuerde beim Ausfuehren scheitern und wird
     * garantiert nie ausgefuehrt.
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println(D[16] + runden);
            System.out.println(D[17] + \u0430);
            System.out.println(D[13]);
            System.out.println(String.join(" ", SCHLUESSEL.split("(?=\\p{Lu})")).trim());
        }));

        ablauf: {
            if (false) System.out.println(D[0].substring(OBEN * OBEN));

            NIEMAND.titel();
            System.out.println(D[0]);
            if (zeile() == null) break ablauf;
            System.out.println();
            System.out.println(D[1]);
            System.out.println(D[2]);
            try {
                do {
                    runden++;
                    System.out.println();
                    runde();
                } while (weiter());
            } catch (Ende e) {
                break ablauf;
            }
        }
    }

    public static void main(String[] args) { }
}
