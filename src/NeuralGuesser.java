import java.io.*;
import java.util.*;

/**
 * NeuralGuesser - Zusatzaufgabe "Der Computer errät deine Zahl".
 *
 * Besonderheit: die Rate-Strategie ist NICHT einprogrammiert. Das Programm
 * enthält ein komplett selbst geschriebenes neuronales Netz (Multi-Layer
 * Perceptron mit tanh/Sigmoid, Backpropagation, Adam-Optimizer), das per
 * Reinforcement Learning (REINFORCE / Policy Gradient) im Selbstspiel lernt,
 * welchen Anteil der noch möglichen Spanne es raten soll.
 *
 * Eingabe des Netzes : [ low/99, high/99, (high-low)/99 ]
 * Ausgabe des Netzes : mu in (0,1) -- der Bruchteil der Spanne
 * Tipp               = low + round(mu * (high - low))
 * Belohnung          = -1 pro Rateversuch  (also: möglichst wenige Versuche)
 *
 * Das Netz entdeckt dabei von selbst, dass mu ~ 0.5 optimal ist -- es lernt
 * also die binäre Suche, ohne sie je gezeigt bekommen zu haben.
 *
 * Nur Standard-Java, keine externen Bibliotheken.
 *
 * Jeder Start trainiert ein frisches Netz im Speicher (ein paar Sekunden)
 * und spielt danach gegen den Benutzer. Keine Optionen, keine Dateien.
 *
 * Start:  java NeuralGuesser.java          (Java 11+, ohne Compile-Schritt)
 * oder :  javac -encoding UTF-8 NeuralGuesser.java && java NeuralGuesser
 *
 * Eine auf minimale Dateigrösse zusammengedrängte Variante mit identischem
 * Verhalten liegt daneben in NG.java.
 */
public class NeuralGuesser {

    // ===================== Spiel-Konfiguration =====================
    static final int MIN = 0;
    static final int MAX = 99;
    static final int MAX_STEPS = 110;          // Sicherheitsnetz gegen Endlosspiele

    // ===================== Trainings-Konfiguration =====================
    static final int[] LAYERS     = {3, 24, 24, 1};
    static final int   ITERATIONS = 3000;
    static final int   BATCH      = 64;        // Spiele pro Gradienten-Schritt
    static final double LR        = 0.01;      // wird während des Trainings abgesenkt
    static final double SIGMA_START = 0.30;    // Explorationsrauschen am Anfang
    static final double SIGMA_END   = 0.06;    // ... und am Ende (nicht auf 0: sonst
                                               //     wird der Gradient reines Rauschen)
    static final int   EVAL_EVERY   = 125;     // wie oft das beste Netz gesichert wird

    /** Kein fester Seed: jeder Programmstart trainiert ein wirklich neues Netz. */
    static final Random RNG = new Random();

    static PrintStream out;
    static BufferedReader in;

    // ============================================================
    //  main
    // ============================================================
    public static void main(String[] args) throws Exception {
        out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");
        in  = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        banner();

        // 1. Frisches, zufällig initialisiertes Netz - kann noch gar nichts.
        MLP net = new MLP(LAYERS);
        double before = evaluate(net, 3000);
        out.printf(Locale.ROOT, "Frisch initialisiertes Netz:  %5.2f Versuche pro Spiel%n%n", before);

        // 2. Im Selbstspiel trainieren (dauert ein paar Sekunden).
        train(net);

        // 3. Ergebnis zeigen.
        double after = evaluate(net, 3000);
        out.println();
        out.printf(Locale.ROOT, "Vor dem Training :  %5.2f Versuche pro Spiel%n", before);
        out.printf(Locale.ROOT, "Selbst gelernt   :  %5.2f Versuche pro Spiel%n", after);
        out.printf(Locale.ROOT, "Binäre Suche     :  %5.2f Versuche pro Spiel (theoretisches Optimum)%n", binarySearchAverage());
        out.println("Das grenzt schon fast an künstliche Intelligenz.");
        out.println();
        report(net);
        out.println();

        // 4. Mit dem soeben trainierten Netz gegen den Menschen spielen.
        out.println("=========================================================");
        out.println();
        playLoop(net);
    }

    static void banner() {
        out.println("=========================================================");
        out.println(" NeuralGuesser - das Netz lernt raten (Selbstspiel)");
        out.println("=========================================================");
        out.println("Das Netz kennt keine binäre Suche. Es bekommt nur:");
        out.println("  Eingabe    : die noch mögliche Spanne [low, high]");
        out.println("  Ausgabe    : welchen Anteil der Spanne es raten soll");
        out.println("  Belohnung  : -1 für jeden Rateversuch");
        out.println("Alles Weitere entdeckt es selbst - bei jedem Start neu.");
        out.println();
    }

    // ============================================================
    //  Zustand / Aktion
    // ============================================================
    static double[] state(int low, int high) {
        double span = MAX - MIN;
        return new double[]{ (low - MIN) / span, (high - MIN) / span, (high - low) / span };
    }

    /** Tipp ohne Zufall - so spielt das Netz gegen einen Menschen. */
    static int greedyGuess(MLP net, int low, int high) {
        double mu = net.forward(state(low, high))[0];
        int g = low + (int) Math.round(mu * (high - low));
        if (g < low)  g = low;
        if (g > high) g = high;
        return g;
    }

    static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    // ============================================================
    //  Training: REINFORCE mit Baseline
    // ============================================================
    static final class Step {
        double[] s;     // Zustand
        double   eps;   // gezogenes Rauschen (für den Log-Prob-Gradienten)
        int      width; // Spannenbreite -> Schlüssel für die Baseline
    }

    static void train(MLP net) {
        // Baseline je Spannenbreite: exponentiell gleitender Mittelwert des Returns.
        double[] baseSum = new double[MAX - MIN + 1];
        double[] baseCnt = new double[MAX - MIN + 1];

        List<Step>   steps   = new ArrayList<>();
        List<Double> returns = new ArrayList<>();

        double[][][] bestW = null;
        double[][]   bestB = null;
        double       bestScore = Double.MAX_VALUE;

        out.println("Training läuft (" + ITERATIONS + " Iterationen à " + BATCH + " Spiele) ...");
        out.println();
        out.println("   Iter |  sigma |   lr   | Versuche/Spiel | mu bei [0,99] | bestes Netz");
        out.println("  ------+--------+--------+----------------+---------------+------------");

        for (int iter = 1; iter <= ITERATIONS; iter++) {
            double prog  = (iter - 1.0) / (ITERATIONS - 1.0);
            double sigma = SIGMA_START + (SIGMA_END - SIGMA_START) * prog;
            // Lernrate absenken (Cosine-Decay): gegen Ende soll die Strategie
            // nicht mehr durch das Gradientenrauschen weggeschoben werden.
            double lr    = LR * 0.5 * (1.0 + Math.cos(Math.PI * prog));

            steps.clear();
            returns.clear();
            int totalTries = 0;

            // ---- Selbstspiel: BATCH Episoden sammeln ----
            for (int e = 0; e < BATCH; e++) {
                int secret = MIN + RNG.nextInt(MAX - MIN + 1);
                int low = MIN, high = MAX;
                int start = steps.size();

                while (true) {
                    double[] s  = state(low, high);
                    double   mu = net.forward(s)[0];
                    double   eps = RNG.nextGaussian();
                    double   a   = clamp01(mu + sigma * eps);

                    int guess = low + (int) Math.round(a * (high - low));
                    if (guess < low)  guess = low;
                    if (guess > high) guess = high;

                    Step st = new Step();
                    st.s = s; st.eps = eps; st.width = high - low;
                    steps.add(st);
                    returns.add(0.0);                       // Platzhalter

                    if (guess == secret) break;
                    if (secret < guess) high = guess - 1; else low = guess + 1;
                    if (steps.size() - start >= MAX_STEPS) break;
                }

                int n = steps.size() - start;
                totalTries += n;
                // Reward -1 pro Schritt  =>  Return ab Schritt i ist -(n - i)
                for (int i = 0; i < n; i++) returns.set(start + i, (double) -(n - i));
            }

            // ---- Vorteile (Advantages) berechnen ----
            int m = steps.size();
            double[] adv = new double[m];
            for (int i = 0; i < m; i++) {
                Step st = steps.get(i);
                double g = returns.get(i);
                double b = baseCnt[st.width] > 0 ? baseSum[st.width] / baseCnt[st.width] : 0.0;
                adv[i] = g - b;
                baseSum[st.width] = baseSum[st.width] * 0.995 + g;
                baseCnt[st.width] = baseCnt[st.width] * 0.995 + 1.0;
            }
            double mean = 0;
            for (double v : adv) mean += v;
            mean /= m;
            double var = 0;
            for (double v : adv) var += (v - mean) * (v - mean);
            double sd = Math.sqrt(var / m) + 1e-8;
            for (int i = 0; i < m; i++) adv[i] = (adv[i] - mean) / sd;

            // ---- Policy-Gradient anwenden ----
            // log p(a|mu) für a ~ N(mu, sigma):  d/dmu = (a - mu)/sigma^2 = eps/sigma
            // Verlust = -advantage * log p  =>  dL/dmu = -advantage * eps/sigma
            for (int i = 0; i < m; i++) {
                Step st = steps.get(i);
                net.forward(st.s);
                double dLdMu = -adv[i] * st.eps / sigma;
                if (dLdMu >  20) dLdMu =  20;
                if (dLdMu < -20) dLdMu = -20;
                net.backward(new double[]{ dLdMu });
            }
            net.step(lr, m);

            // ---- bestes Netz merken (Early Stopping ohne Abbruch) ----
            boolean improved = false;
            if (iter % EVAL_EVERY == 0 || iter == ITERATIONS) {
                double score = evaluate(net, 800);
                if (score < bestScore) {
                    bestScore = score;
                    bestW = copy3(net.w);
                    bestB = copy2(net.b);
                    improved = true;
                }
            }

            if (iter == 1 || iter % 250 == 0) {
                double muFull = net.forward(state(MIN, MAX))[0];
                out.printf(Locale.ROOT, "   %4d | %.4f | %.4f |         %6.2f |        %.4f | %s%n",
                           iter, sigma, lr, totalTries / (double) BATCH, muFull,
                           bestScore == Double.MAX_VALUE ? "-"
                                 : String.format(Locale.ROOT, "%.2f%s", bestScore, improved ? " *" : ""));
            }
        }

        // bestes gefundenes Netz zurückspielen
        if (bestW != null) {
            for (int l = 0; l < net.L; l++) {
                for (int o = 0; o < net.sizes[l + 1]; o++) {
                    net.b[l][o] = bestB[l][o];
                    System.arraycopy(bestW[l][o], 0, net.w[l][o], 0, net.sizes[l]);
                }
            }
            out.println();
            out.printf(Locale.ROOT, "Bestes während des Trainings gefundenes Netz wiederhergestellt (%.2f).%n", bestScore);
        }
    }

    static double[][][] copy3(double[][][] a) {
        double[][][] c = new double[a.length][][];
        for (int i = 0; i < a.length; i++) c[i] = copy2(a[i]);
        return c;
    }

    static double[][] copy2(double[][] a) {
        double[][] c = new double[a.length][];
        for (int i = 0; i < a.length; i++) c[i] = a[i].clone();
        return c;
    }

    // ============================================================
    //  Auswertung
    // ============================================================
    static double evaluate(MLP net, int games) {
        Random r = new Random(4711);
        long total = 0;
        for (int g = 0; g < games; g++) {
            int secret = MIN + r.nextInt(MAX - MIN + 1);
            int low = MIN, high = MAX, n = 0;
            while (true) {
                int guess = greedyGuess(net, low, high);
                n++;
                if (guess == secret) break;
                if (secret < guess) high = guess - 1; else low = guess + 1;
                if (n >= MAX_STEPS) break;
            }
            total += n;
        }
        return total / (double) games;
    }

    static double binarySearchAverage() {
        long total = 0;
        for (int secret = MIN; secret <= MAX; secret++) {
            int low = MIN, high = MAX, n = 0;
            while (true) {
                int guess = (low + high) / 2;
                n++;
                if (guess == secret) break;
                if (secret < guess) high = guess - 1; else low = guess + 1;
            }
            total += n;
        }
        return total / (double) (MAX - MIN + 1);
    }

    static void report(MLP net) {
        out.println("Was das Netz gelernt hat");
        out.println("(mu = Anteil der Spanne, den es als Tipp wählt; 0.5 = Mitte)");
        out.println();
        out.println("    Spanne     |   mu   | Tipp");
        out.println("  -------------+--------+------");
        int[][] ranges = { {0,99}, {0,49}, {50,99}, {20,80}, {37,43}, {0,9}, {90,99}, {44,44} };
        for (int[] r : ranges) {
            double mu = net.forward(state(r[0], r[1]))[0];
            out.printf(Locale.ROOT, "   [%2d, %2d]    | %.4f |  %2d%n",
                       r[0], r[1], mu, greedyGuess(net, r[0], r[1]));
        }
        out.println();
        out.printf(Locale.ROOT, "Schnitt über 3000 Spiele: %.2f Versuche (binäre Suche: %.2f)%n",
                   evaluate(net, 3000), binarySearchAverage());
    }

    // ============================================================
    //  Das eigentliche Spiel
    // ============================================================
    static void playLoop(MLP net) throws IOException {
        while (true) {
            playOne(net);
            out.println();
            out.println("Spielen wir nochmal? (j/n)");
            String line = in.readLine();
            if (line == null) break;
            line = line.trim().toLowerCase(Locale.ROOT);
            if (!(line.isEmpty() || line.startsWith("j") || line.startsWith("y"))) break;
            out.println();
        }
        out.println("Bis zum nächsten Mal!");
    }

    static void playOne(MLP net) throws IOException {
        out.println("Denke dir eine Zahl zwischen " + MIN + " und " + MAX + " und drücke dann Enter.");
        if (in.readLine() == null) return;
        out.println();
        out.println("Ok. Ich beginne jetzt zu raten.");
        out.println("Antworte mir mit 1 für richtig, 2 für kleiner und 3 für grösser.");
        out.println();

        int low = MIN, high = MAX, tries = 0;
        while (true) {
            if (low > high) {
                out.println("Hmm, deine Antworten passen nicht zusammen. Hast du geschummelt?");
                return;
            }
            int guess = greedyGuess(net, low, high);
            tries++;
            out.println("Ist es die " + guess + "?");

            int answer = readAnswer();
            if (answer < 0) return;                       // Eingabe-Ende
            if (answer == 1) {
                out.println(comment(tries) + " (" + tries + " Versuche)");
                return;
            } else if (answer == 2) {
                high = guess - 1;                         // gesuchte Zahl ist kleiner
            } else {
                low = guess + 1;                          // gesuchte Zahl ist grösser
            }
        }
    }

    static int readAnswer() throws IOException {
        while (true) {
            String line = in.readLine();
            if (line == null) return -1;
            line = line.trim();
            if (line.equals("1") || line.equals("2") || line.equals("3")) return Integer.parseInt(line);
            out.println("Bitte 1 (richtig), 2 (kleiner) oder 3 (grösser) eingeben.");
        }
    }

    static String comment(int tries) {
        if (tries <= 3) return "Das war ja einfach!";
        if (tries <= 5) return "Gefunden!";
        if (tries <= 6) return "Geschafft.";
        return "Uff, das war schwierig.";
    }

    // ============================================================
    //  Neuronales Netz (MLP + Backprop + Adam) - alles von Hand
    // ============================================================
    static final class MLP {
        final int[] sizes;
        final int   L;                     // Anzahl Gewichtsschichten
        final double[][][] w, gw, mw, vw;  // Gewichte, Gradienten, Adam-Momente
        final double[][]   b, gb, mb, vb;  // Bias dito
        final double[][]   act;            // act[0] = Eingabe, act[l+1] = Ausgabe Schicht l
        int t = 0;                         // Adam-Schrittzähler

        MLP(int[] sizes) {
            this.sizes = sizes.clone();
            this.L = sizes.length - 1;
            w  = new double[L][][]; gw = new double[L][][]; mw = new double[L][][]; vw = new double[L][][];
            b  = new double[L][];   gb = new double[L][];   mb = new double[L][];   vb = new double[L][];
            act = new double[L + 1][];
            act[0] = new double[sizes[0]];

            for (int l = 0; l < L; l++) {
                int nin = sizes[l], nout = sizes[l + 1];
                w[l]  = new double[nout][nin]; gw[l] = new double[nout][nin];
                mw[l] = new double[nout][nin]; vw[l] = new double[nout][nin];
                b[l]  = new double[nout]; gb[l] = new double[nout];
                mb[l] = new double[nout]; vb[l] = new double[nout];
                act[l + 1] = new double[nout];

                double scale = Math.sqrt(2.0 / (nin + nout));      // Xavier
                for (int o = 0; o < nout; o++) {
                    for (int i = 0; i < nin; i++) w[l][o][i] = RNG.nextGaussian() * scale;
                    // Ausgabe-Bias bewusst zufällig versetzt, damit die Startstrategie
                    // wirklich willkürlich ist und man das Lernen auch sieht.
                    b[l][o] = (l == L - 1) ? (RNG.nextDouble() * 4.0 - 2.0) : 0.0;
                }
            }
        }

        static double sigmoid(double x) { return 1.0 / (1.0 + Math.exp(-x)); }

        double[] forward(double[] x) {
            System.arraycopy(x, 0, act[0], 0, sizes[0]);
            for (int l = 0; l < L; l++) {
                double[] a = act[l];
                for (int o = 0; o < sizes[l + 1]; o++) {
                    double s = b[l][o];
                    double[] row = w[l][o];
                    for (int i = 0; i < sizes[l]; i++) s += row[i] * a[i];
                    act[l + 1][o] = (l == L - 1) ? sigmoid(s) : Math.tanh(s);
                }
            }
            return act[L];
        }

        /** Backprop; dLdOut = Ableitung des Verlusts nach der Netz-Ausgabe. */
        void backward(double[] dLdOut) {
            double[] delta = new double[sizes[L]];
            for (int o = 0; o < sizes[L]; o++) {
                double y = act[L][o];
                delta[o] = dLdOut[o] * y * (1.0 - y);          // Ableitung Sigmoid
            }
            for (int l = L - 1; l >= 0; l--) {
                for (int o = 0; o < sizes[l + 1]; o++) {
                    double d = delta[o];
                    gb[l][o] += d;
                    double[] grow = gw[l][o];
                    double[] a = act[l];
                    for (int i = 0; i < sizes[l]; i++) grow[i] += d * a[i];
                }
                if (l > 0) {
                    double[] nd = new double[sizes[l]];
                    for (int i = 0; i < sizes[l]; i++) {
                        double s = 0;
                        for (int o = 0; o < sizes[l + 1]; o++) s += w[l][o][i] * delta[o];
                        double a = act[l][i];
                        nd[i] = s * (1.0 - a * a);             // Ableitung tanh
                    }
                    delta = nd;
                }
            }
        }

        /** Adam-Schritt über die gemittelten Gradienten; setzt die Gradienten zurück. */
        void step(double lr, int n) {
            t++;
            final double b1 = 0.9, b2 = 0.999, eps = 1e-8;
            double c1 = 1.0 - Math.pow(b1, t), c2 = 1.0 - Math.pow(b2, t);
            for (int l = 0; l < L; l++) {
                for (int o = 0; o < sizes[l + 1]; o++) {
                    double g = gb[l][o] / n; gb[l][o] = 0;
                    mb[l][o] = b1 * mb[l][o] + (1 - b1) * g;
                    vb[l][o] = b2 * vb[l][o] + (1 - b2) * g * g;
                    b[l][o] -= lr * (mb[l][o] / c1) / (Math.sqrt(vb[l][o] / c2) + eps);
                    for (int i = 0; i < sizes[l]; i++) {
                        double gv = gw[l][o][i] / n; gw[l][o][i] = 0;
                        mw[l][o][i] = b1 * mw[l][o][i] + (1 - b1) * gv;
                        vw[l][o][i] = b2 * vw[l][o][i] + (1 - b2) * gv * gv;
                        w[l][o][i] -= lr * (mw[l][o][i] / c1) / (Math.sqrt(vw[l][o][i] / c2) + eps);
                    }
                }
            }
        }
    }
}
