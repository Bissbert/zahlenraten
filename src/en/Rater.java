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
 * Bonus exercise: reverse number guessing.
 *
 * The human thinks of a number between 0 and 99 and the computer guesses it.
 * The human answers with 1 (correct), 2 (smaller) or 3 (larger).
 *
 * Strategy: binary search. The computer remembers the interval the target can
 * still be in and always guesses its midpoint. Every wrong answer halves the
 * interval, so it never needs more than 7 guesses (2^7 = 128 > 100).
 *
 * Start:  java Rater.java
 */
public class Rater {

    /** Smallest number the human is allowed to think of. */
    private static final int MIN = 0;
    /** Largest number the human is allowed to think of. */
    private static final int MAX = 99;

    private static final int CORRECT = 1;
    private static final int SMALLER = 2;
    private static final int LARGER  = 3;

    /** Input and output pinned to UTF-8, so the text is right everywhere. */
    private static final PrintStream OUT = new PrintStream(
            new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
    private static final BufferedReader IN = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public static void main(String[] args) throws IOException {
        OUT.println("Think of a number between " + MIN + " and " + MAX
                + ", then press Enter.");

        if (IN.readLine() == null) {   // input ended straight away
            sayGoodbye();
            return;
        }

        OUT.println();
        OUT.println("Ok. I'll start guessing now.");
        OUT.println("Answer me with 1 for correct, 2 for smaller and 3 for larger.");

        try {
            do {
                OUT.println();
                playRound();
            } while (again());
        } catch (EOFException e) {
            // The human closed the input: just stop cleanly.
        }

        sayGoodbye();
    }

    /**
     * Plays one round. The interval [low, high] holds every number that is
     * still possible given the answers so far.
     */
    private static void playRound() throws IOException {
        int low  = MIN;
        int high = MAX;
        int guesses = 0;

        while (low <= high) {
            int guess = low + (high - low) / 2;   // midpoint, without overflow
            guesses++;
            OUT.println("Is it " + guess + "?");

            switch (readAnswer()) {
                case CORRECT:
                    report(guesses);
                    return;
                case SMALLER:
                    high = guess - 1;   // the number is smaller than the guess
                    break;
                default:
                    low = guess + 1;    // the number is larger than the guess
                    break;
            }
        }

        // The interval ran empty: no number fits all the answers.
        OUT.println();
        OUT.println("Hmm, your answers don't add up. Did you cheat?");
    }

    /** Prints the success message together with the number of guesses. */
    private static void report(int guesses) {
        OUT.println();
        OUT.println(rating(guesses) + " (" + guesses
                + (guesses == 1 ? " guess)" : " guesses)"));
        OUT.println("That's almost artificial intelligence.");
    }

    private static String rating(int guesses) {
        if (guesses <= 5) {
            return "Well, that was easy!";
        }
        if (guesses <= 6) {
            return "Got it.";
        }
        return "Phew, that was tricky.";
    }

    /**
     * Keeps reading lines until one of them contains 1, 2 or 3.
     *
     * @throws EOFException when the input has ended
     */
    private static int readAnswer() throws IOException {
        while (true) {
            String line = IN.readLine();
            if (line == null) {
                throw new EOFException();
            }
            switch (line.trim()) {
                case "1": return CORRECT;
                case "2": return SMALLER;
                case "3": return LARGER;
                default:
                    OUT.println("Please enter 1 (correct), 2 (smaller) or 3 (larger).");
            }
        }
    }

    /** Asks for another round. An empty line means yes. */
    private static boolean again() throws IOException {
        OUT.println();
        OUT.println("Shall we play again? (y/n)");

        String line = IN.readLine();
        if (line == null) {
            return false;
        }
        switch (line.trim().toLowerCase(Locale.ROOT)) {
            case "":
            case "y":
            case "yes":
            case "j":
            case "ja":
                return true;
            default:
                return false;
        }
    }

    private static void sayGoodbye() {
        OUT.println();
        OUT.println("See you next time!");
    }
}
