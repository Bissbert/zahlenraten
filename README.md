# Reverse Number Guessing

**Five Java solutions to the same school exercise.** From "this is how you should
write it", through "a neural network discovers the strategy on its own", all the
way to "this program does not search at all".

> **Bonus exercise:** Write a program that does the opposite of the usual guessing
> game — **the computer guesses the number the human is thinking of.** Range 0 to
> 99. The human answers `1` for correct, `2` for smaller and `3` for larger.
>
> *"That's almost artificial intelligence!"*

![One round](media/spiel.gif)

All five programs behave **identically from the outside**: same prompts, same
flow, same cheat detection, same replay loop. All five need **nothing but the
JDK** — no library, no Maven, no build step. Only the road there differs.

> The programs speak German, because the exercise does. This documentation is in
> English.

## Quick start

```sh
java src/Rater.java              # the normal solution
java src/NeuralGuesser.java      # neural network, retrained on every launch
java src/NG.java                 # same thing, minified
java src/Verflucht.java          # cursed, but fully defined
java src/ILoveMyTeacher.java     # more cursed
```

Requires **Java 11+**; `ILoveMyTeacher.java` needs **Java 15+** for text blocks.
Tested on OpenJDK 21.0.2.

## The five versions

```mermaid
graph LR
    A["Bonus exercise<br/>0 to 99, answers 1/2/3"]
    A --> B["Rater.java<br/><i>binary search, written out</i>"]
    A --> C["NeuralGuesser.java<br/><i>network learns the strategy</i>"]
    A --> E["Verflucht.java<br/><i>binary search, 15 curses</i>"]
    C --> D["NG.java<br/><i>minified, −68 %</i>"]
    E --> F["ILoveMyTeacher.java<br/><i>no search at all, 22 curses</i>"]

    style B fill:#1f6feb,stroke:#58a6ff,color:#fff
    style C fill:#238636,stroke:#3fb950,color:#fff
    style D fill:#238636,stroke:#3fb950,color:#fff
    style E fill:#9e6a03,stroke:#d29922,color:#fff
    style F fill:#8250df,stroke:#bc8cff,color:#fff
```

| File | Point | Method | Bytes | Lines | Write-up |
|---|---|---|---:|---:|---|
| **`Rater.java`** | How you should write it | Binary search, spelled out | 5,349 | 162 | [→](docs/01-rater.md) |
| **`NeuralGuesser.java`** | "Solve it with AI", readably | Neural network, learns by itself | 21,312 | 487 | [→](docs/02-neuralguesser.md) |
| **`NG.java`** | As smol as possible | Identical, minified | 6,706 | 102 | [→](docs/03-ng.md) |
| **`Verflucht.java`** | As perverse as possible | Binary search, 15 curses | 10,349 | 229 | [→](docs/04-verflucht.md) |
| **`ILoveMyTeacher.java`** | Worse | *No search at all*, 22 curses | 14,096 | 303 | [→](docs/05-ilovemyteacher.md) |

## All five play equally well

Driven automatically through **all 100 possible numbers**:

| File | Found all 100 | min | mean | max | Runtime | `javac -Xlint:all` |
|---|:---:|---:|---:|---:|---:|---:|
| `Rater.java` | ✅ | 1 | **5.80** | 7 | 0.4 s | 0 warnings |
| `NeuralGuesser.java` | ✅ | 1 | **5.80** | 7 | 4.5 s | 0 warnings |
| `NG.java` | ✅ | 1 | **5.80** | 7 | 4.5 s | 0 warnings |
| `Verflucht.java` | ✅ | 1 | **5.80** | 7 | 0.4 s | 4 warnings |
| `ILoveMyTeacher.java` | ✅ | 1 | **5.80** | 7 | 0.4 s | 3 warnings |

5.80 guesses on average and never more than 7 is the **theoretical optimum** for
100 numbers (2⁷ = 128 > 100). The extra runtime of the ML versions is pure
start-up training. The warnings on the cursed versions are deliberate — each one
points straight at a curse.

→ [How this was measured](docs/measurement.md)

## The underlying idea: binary search

The computer remembers the interval the target can still be in and always guesses
its midpoint. Every wrong answer halves the interval.

![Binary search](media/intervall.gif)

When the interval becomes **empty**, no number is consistent with all the answers
— which is where cheat detection comes from, for free, with no extra code.

## The network finds the midpoint by itself

`NeuralGuesser.java` is **not** taught binary search. It only gets the interval as
input, emits a fraction between 0 and 1, and is penalised −1 per guess. It works
out the rest alone.

![Learning curve](media/lernkurve.gif)

*Real measurements from one run.* `mu` is the fraction of the span the network
guesses at. It drifts from a random 0.43 to ≈ 0.50 — the midpoint. The network
rediscovered binary search.

## And one of them stops searching entirely

`ILoveMyTeacher.java` builds an `AbstractList` that **contains no data at all** and
calls `Collections.binarySearch` on it with the search key `null`. The comparator
asks the human on every comparison.

![The oracle](media/orakel.gif)

**The human is the data structure being searched.** The documented contract of the
method hands back the index of the number — and, for contradictory answers, a
negative insertion point. Cheat detection, again for free.

## Which one should I use?

| Situation | File |
|---|---|
| Understand the exercise, hand in a solution | [`Rater.java`](src/Rater.java) |
| Show that a network can find the strategy itself | [`NeuralGuesser.java`](src/NeuralGuesser.java) |
| Show how small it goes | [`NG.java`](src/NG.java) |
| Show how much the Java spec actually permits | [`Verflucht.java`](src/Verflucht.java) |
| Get back at your teacher | [`ILoveMyTeacher.java`](src/ILoveMyTeacher.java) |

## Repository layout

```
src/      the five programs
docs/     a detailed write-up per version
media/    the animations (built by tools/media_bauen.py)
tools/    test drivers and measurement scripts
```

## Known limitations

- For `ILoveMyTeacher.java`, the contract of `Collections.binarySearch` guarantees
  the **result** (index, or negative insertion point), not the particular sequence
  of questions. The measured worst case of 7 questions holds for OpenJDK; the
  program is *correct* on any conforming implementation.
- The ML versions retrain on every launch with no fixed seed, so results wobble
  slightly — but every run so far has landed on the optimum.
- `Rater.java`, `NeuralGuesser.java` and `NG.java` contain umlauts and are UTF-8.
  Java 18+ defaults to UTF-8; on older JDKs pass `-encoding UTF-8` when compiling.
