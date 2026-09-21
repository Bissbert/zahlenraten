import java.io.*;
import java.util.*;

/**
 * NeuralGuesser - bonus exercise "the computer guesses your number".
 *
 * The point: the guessing strategy is NOT programmed in. The program contains
 * a neural network written entirely from scratch (multi-layer perceptron with
 * tanh/sigmoid, backpropagation, Adam optimiser) that learns, by reinforcement
 * learning (REINFORCE / policy gradient) in self-play, what fraction of the
 * still-possible span it should guess at.
 *
 * Network input  : [ low/99, high/99, (high-low)/99 ]
 * Network output : mu in (0,1) -- the fraction of the span
 * Guess          = low + round(mu * (high - low))
 * Reward         = -1 per guess  (i.e. ask as few questions as possible)
 *
 * The network works out by itself that mu ~ 0.5 is optimal -- it learns binary
 * search without ever having been shown it.
 *
 * Standard Java only, no external libraries.
 *
 * Every launch trains a fresh network in memory (a few seconds) and then plays
 * against the user. No options, no files.
 *
 * Start:  java NeuralGuesser.java          (Java 11+, no compile step)
 * or   :  javac -encoding UTF-8 NeuralGuesser.java && java NeuralGuesser
 *
 * A variant squeezed down to minimal file size, with identical behaviour,
 * sits next to it in NG.java.
 */
public class NeuralGuesser {

    // ===================== game configuration =====================
    static final int MIN = 0;
    static final int MAX = 99;
    static final int MAX_STEPS = 110;          // safety net against endless games

    // ===================== training configuration =====================
    static final int[] LAYERS     = {3, 24, 24, 1};
    static final int   ITERATIONS = 3000;
    static final int   BATCH      = 64;        // games per gradient step
    static final double LR        = 0.01;      // decayed over the course of training
    static final double SIGMA_START = 0.30;    // exploration noise at the start
    static final double SIGMA_END   = 0.06;    // ... and at the end (not 0: otherwise
                                               //     the gradient becomes pure noise)
    static final int   EVAL_EVERY   = 125;     // how often the best network is saved

    /** No fixed seed: every launch really does train a brand new network. */
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

        // 1. Fresh, randomly initialised network - knows nothing yet.
        MLP net = new MLP(LAYERS);
        double before = evaluate(net, 3000);
        out.printf(Locale.ROOT, "Freshly initialised network:  %5.2f guesses per game%n%n", before);

        // 2. Train in self-play (takes a few seconds).
        train(net);

        // 3. Show the result.
        double after = evaluate(net, 3000);
        out.println();
        out.printf(Locale.ROOT, "Before training :  %5.2f guesses per game%n", before);
        out.printf(Locale.ROOT, "Self-taught     :  %5.2f guesses per game%n", after);
        out.printf(Locale.ROOT, "Binary search   :  %5.2f guesses per game (theoretical optimum)%n", binarySearchAverage());
        out.println("That's almost artificial intelligence.");
        out.println();
        report(net);
        out.println();

        // 4. Play against the human with the network we just trained.
        out.println("=========================================================");
        out.println();
        playLoop(net);
    }

    static void banner() {
        out.println("=========================================================");
        out.println(" NeuralGuesser - the network learns to guess (self-play)");
        out.println("=========================================================");
        out.println("The network knows no binary search. All it gets is:");
        out.println("  input      : the still-possible span [low, high]");
        out.println("  output     : what fraction of the span to guess at");
        out.println("  reward     : -1 for every guess");
        out.println("Everything else it discovers itself - anew on every start.");
        out.println();
    }

    // ============================================================
    //  state / action
    // ============================================================
    static double[] state(int low, int high) {
        double span = MAX - MIN;
        return new double[]{ (low - MIN) / span, (high - MIN) / span, (high - low) / span };
    }

    /** Guess without noise - this is how the network plays against a human. */
    static int greedyGuess(MLP net, int low, int high) {
        double mu = net.forward(state(low, high))[0];
        int g = low + (int) Math.round(mu * (high - low));
        if (g < low)  g = low;
        if (g > high) g = high;
        return g;
    }

    static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    // ============================================================
    //  training: REINFORCE with a baseline
    // ============================================================
    static final class Step {
        double[] s;     // state
        double   eps;   // the noise that was drawn (for the log-prob gradient)
        int      width; // span width -> key for the baseline
    }

    static void train(MLP net) {
        // One baseline per span width: exponential moving average of the return.
        double[] baseSum = new double[MAX - MIN + 1];
        double[] baseCnt = new double[MAX - MIN + 1];

        List<Step>   steps   = new ArrayList<>();
        List<Double> returns = new ArrayList<>();

        double[][][] bestW = null;
        double[][]   bestB = null;
        double       bestScore = Double.MAX_VALUE;

        out.println("Training (" + ITERATIONS + " iterations of " + BATCH + " games) ...");
        out.println();
        out.println("   Iter |  sigma |   lr   |  guesses/game  |  mu at [0,99] | best net");
        out.println("  ------+--------+--------+----------------+---------------+------------");

        for (int iter = 1; iter <= ITERATIONS; iter++) {
            double prog  = (iter - 1.0) / (ITERATIONS - 1.0);
            double sigma = SIGMA_START + (SIGMA_END - SIGMA_START) * prog;
            // Decay the learning rate (cosine decay): towards the end the policy
            // should no longer be pushed around by gradient noise.
            double lr    = LR * 0.5 * (1.0 + Math.cos(Math.PI * prog));

            steps.clear();
            returns.clear();
            int totalTries = 0;

            // ---- self-play: collect BATCH episodes ----
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
                    returns.add(0.0);                       // placeholder

                    if (guess == secret) break;
                    if (secret < guess) high = guess - 1; else low = guess + 1;
                    if (steps.size() - start >= MAX_STEPS) break;
                }

                int n = steps.size() - start;
                totalTries += n;
                // Reward -1 per step  =>  the return from step i on is -(n - i)
                for (int i = 0; i < n; i++) returns.set(start + i, (double) -(n - i));
            }

            // ---- compute the advantages ----
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

            // ---- apply the policy gradient ----
            // log p(a|mu) for a ~ N(mu, sigma):  d/dmu = (a - mu)/sigma^2 = eps/sigma
            // loss = -advantage * log p  =>  dL/dmu = -advantage * eps/sigma
            for (int i = 0; i < m; i++) {
                Step st = steps.get(i);
                net.forward(st.s);
                double dLdMu = -adv[i] * st.eps / sigma;
                if (dLdMu >  20) dLdMu =  20;
                if (dLdMu < -20) dLdMu = -20;
                net.backward(new double[]{ dLdMu });
            }
            net.step(lr, m);

            // ---- remember the best network (early stopping without stopping) ----
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

        // restore the best network we found
        if (bestW != null) {
            for (int l = 0; l < net.L; l++) {
                for (int o = 0; o < net.sizes[l + 1]; o++) {
                    net.b[l][o] = bestB[l][o];
                    System.arraycopy(bestW[l][o], 0, net.w[l][o], 0, net.sizes[l]);
                }
            }
            out.println();
            out.printf(Locale.ROOT, "Restored the best network found during training (%.2f).%n", bestScore);
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
    //  evaluation
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
        out.println("What the network has learned");
        out.println("(mu = the fraction of the span it guesses at; 0.5 = midpoint)");
        out.println();
        out.println("     span      |   mu   | guess");
        out.println("  -------------+--------+------");
        int[][] ranges = { {0,99}, {0,49}, {50,99}, {20,80}, {37,43}, {0,9}, {90,99}, {44,44} };
        for (int[] r : ranges) {
            double mu = net.forward(state(r[0], r[1]))[0];
            out.printf(Locale.ROOT, "   [%2d, %2d]    | %.4f |  %2d%n",
                       r[0], r[1], mu, greedyGuess(net, r[0], r[1]));
        }
        out.println();
        out.printf(Locale.ROOT, "Mean over 3000 games: %.2f guesses (binary search: %.2f)%n",
                   evaluate(net, 3000), binarySearchAverage());
    }

    // ============================================================
    //  the actual game
    // ============================================================
    static void playLoop(MLP net) throws IOException {
        while (true) {
            playOne(net);
            out.println();
            out.println("Shall we play again? (y/n)");
            String line = in.readLine();
            if (line == null) break;
            line = line.trim().toLowerCase(Locale.ROOT);
            if (!(line.isEmpty() || line.startsWith("y") || line.startsWith("j"))) break;
            out.println();
        }
        out.println("See you next time!");
    }

    static void playOne(MLP net) throws IOException {
        out.println("Think of a number between " + MIN + " and " + MAX + ", then press Enter.");
        if (in.readLine() == null) return;
        out.println();
        out.println("Ok. I'll start guessing now.");
        out.println("Answer me with 1 for correct, 2 for smaller and 3 for larger.");
        out.println();

        int low = MIN, high = MAX, tries = 0;
        while (true) {
            if (low > high) {
                out.println("Hmm, your answers don't add up. Did you cheat?");
                return;
            }
            int guess = greedyGuess(net, low, high);
            tries++;
            out.println("Is it " + guess + "?");

            int answer = readAnswer();
            if (answer < 0) return;                       // end of input
            if (answer == 1) {
                out.println(comment(tries) + " (" + tries
                        + (tries == 1 ? " guess)" : " guesses)"));
                return;
            } else if (answer == 2) {
                high = guess - 1;                         // the target is smaller
            } else {
                low = guess + 1;                          // the target is larger
            }
        }
    }

    static int readAnswer() throws IOException {
        while (true) {
            String line = in.readLine();
            if (line == null) return -1;
            line = line.trim();
            if (line.equals("1") || line.equals("2") || line.equals("3")) return Integer.parseInt(line);
            out.println("Please enter 1 (correct), 2 (smaller) or 3 (larger).");
        }
    }

    static String comment(int tries) {
        if (tries <= 3) return "Well, that was easy!";
        if (tries <= 5) return "Found it!";
        if (tries <= 6) return "Got it.";
        return "Phew, that was tricky.";
    }

    // ============================================================
    //  neural network (MLP + backprop + Adam) - all by hand
    // ============================================================
    static final class MLP {
        final int[] sizes;
        final int   L;                     // number of weight layers
        final double[][][] w, gw, mw, vw;  // weights, gradients, Adam moments
        final double[][]   b, gb, mb, vb;  // biases likewise
        final double[][]   act;            // act[0] = input, act[l+1] = output of layer l
        int t = 0;                         // Adam step counter

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
                    // The output bias is deliberately offset at random, so the
                    // starting policy really is arbitrary and the learning is visible.
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

        /** Backprop; dLdOut = derivative of the loss w.r.t. the network output. */
        void backward(double[] dLdOut) {
            double[] delta = new double[sizes[L]];
            for (int o = 0; o < sizes[L]; o++) {
                double y = act[L][o];
                delta[o] = dLdOut[o] * y * (1.0 - y);          // sigmoid derivative
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
                        nd[i] = s * (1.0 - a * a);             // tanh derivative
                    }
                    delta = nd;
                }
            }
        }

        /** One Adam step over the averaged gradients; resets the gradients. */
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
