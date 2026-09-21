import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Zusatzaufgabe: Umgekehrtes Zahlenraten.
 *
 * Der Mensch denkt sich eine Zahl zwischen 0 und 99, der Computer errät sie.
 * Der Mensch antwortet mit 1 (richtig), 2 (kleiner) oder 3 (grösser).
 *
 * Strategie: binäre Suche. Der Computer merkt sich das Intervall, in dem die
 * gesuchte Zahl noch liegen kann, und tippt jedes Mal auf dessen Mitte. Damit
 * halbiert sich das Intervall bei jeder falschen Antwort, und es sind nie mehr
 * als 7 Versuche nötig (2^7 = 128 > 100).
 *
 * Start:  java Rater.java
 */
public class Rater {

    /** Kleinste Zahl, die sich der Mensch denken darf. */
    private static final int MIN = 0;
    /** Grösste Zahl, die sich der Mensch denken darf. */
    private static final int MAX = 99;

    private static final int RICHTIG  = 1;
    private static final int KLEINER  = 2;
    private static final int GROESSER = 3;

    /** Ein- und Ausgabe fest auf UTF-8, damit die Umlaute überall stimmen. */
    private static final PrintStream OUT = new PrintStream(
            new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
    private static final BufferedReader IN = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public static void main(String[] args) throws IOException {
        OUT.println("Denke dir eine Zahl zwischen " + MIN + " und " + MAX
                + " und drücke dann Enter.");

        if (IN.readLine() == null) {   // Eingabe sofort zu Ende
            verabschieden();
            return;
        }

        OUT.println();
        OUT.println("Ok. Ich beginne jetzt zu raten.");
        OUT.println("Antworte mir mit 1 für richtig, 2 für kleiner und 3 für grösser.");

        try {
            do {
                OUT.println();
                spieleRunde();
            } while (nochmal());
        } catch (EOFException e) {
            // Der Mensch hat die Eingabe geschlossen: einfach sauber aufhören.
        }

        verabschieden();
    }

    /**
     * Spielt eine Runde. Das Intervall [unten, oben] enthält alle Zahlen, die
     * nach den bisherigen Antworten noch möglich sind.
     */
    private static void spieleRunde() throws IOException {
        int unten = MIN;
        int oben  = MAX;
        int versuche = 0;

        while (unten <= oben) {
            int tipp = unten + (oben - unten) / 2;   // Mitte, ohne Überlaufgefahr
            versuche++;
            OUT.println("Ist es die " + tipp + "?");

            switch (leseAntwort()) {
                case RICHTIG:
                    melde(versuche);
                    return;
                case KLEINER:
                    oben = tipp - 1;    // die Zahl ist kleiner als der Tipp
                    break;
                default:
                    unten = tipp + 1;   // die Zahl ist grösser als der Tipp
                    break;
            }
        }

        // Das Intervall ist leer geworden: keine Zahl passt zu allen Antworten.
        OUT.println();
        OUT.println("Hmm, deine Antworten passen nicht zusammen. Hast du geschummelt?");
    }

    /** Gibt die Erfolgsmeldung samt Anzahl Versuche aus. */
    private static void melde(int versuche) {
        OUT.println();
        OUT.println(bewertung(versuche) + " (" + versuche
                + (versuche == 1 ? " Versuch)" : " Versuche)"));
        OUT.println("Das grenzt schon fast an künstliche Intelligenz.");
    }

    private static String bewertung(int versuche) {
        if (versuche <= 5) {
            return "Das war ja einfach!";
        }
        if (versuche <= 6) {
            return "Geschafft.";
        }
        return "Uff, das war schwierig.";
    }

    /**
     * Liest so lange eine Zeile, bis sie 1, 2 oder 3 enthält.
     *
     * @throws EOFException wenn die Eingabe zu Ende ist
     */
    private static int leseAntwort() throws IOException {
        while (true) {
            String zeile = IN.readLine();
            if (zeile == null) {
                throw new EOFException();
            }
            switch (zeile.trim()) {
                case "1": return RICHTIG;
                case "2": return KLEINER;
                case "3": return GROESSER;
                default:
                    OUT.println("Bitte 1 (richtig), 2 (kleiner) oder 3 (grösser) eingeben.");
            }
        }
    }

    /** Fragt, ob noch eine Runde gespielt werden soll. Leere Eingabe heisst ja. */
    private static boolean nochmal() throws IOException {
        OUT.println();
        OUT.println("Spielen wir nochmal? (j/n)");

        String zeile = IN.readLine();
        if (zeile == null) {
            return false;
        }
        switch (zeile.trim().toLowerCase(Locale.ROOT)) {
            case "":
            case "j":
            case "ja":
            case "y":
            case "yes":
                return true;
            default:
                return false;
        }
    }

    private static void verabschieden() {
        OUT.println();
        OUT.println("Bis zum nächsten Mal!");
    }
}
