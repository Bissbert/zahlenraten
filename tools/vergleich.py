# -*- coding: utf-8 -*-
"""Spielt alle 100 Zahlen in EINEM Prozess durch - fuer beide Sprachfassungen.
   Plays all 100 numbers in a SINGLE process - for both language editions."""
import os, subprocess, re, sys, time

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src")

SPRACHEN = {
    "de": dict(start="Denke dir", frage=re.compile(r"Ist es die (\d+)\?"),
               nochmal="Spielen wir nochmal?", ja="j", nein="n"),
    "en": dict(start="Think of",  frage=re.compile(r"Is it (\d+)\?"),
               nochmal="Shall we play again?", ja="y", nein="n"),
}

PROGRAMME = ["Rater.java", "NeuralGuesser.java", "NG.java",
             "Verflucht.java", "ILoveMyTeacher.java"]


def durchlauf(lang, src, ziele, timeout=600):
    t = SPRACHEN[lang]
    p = subprocess.Popen(["java", os.path.join(SRC, lang, src)],
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=1)
    counts, runde, n = [], 0, 0
    t0 = time.time()

    def sende(s):
        try:
            p.stdin.write(s + "\n"); p.stdin.flush()
        except (BrokenPipeError, ValueError):
            pass

    while True:
        line = p.stdout.readline()
        if not line:
            break
        line = line.rstrip("\n")
        if line.startswith(t["start"]):
            sende("")
        elif t["frage"].search(line):
            tipp = int(t["frage"].search(line).group(1)); n += 1
            ziel = ziele[runde]
            sende("1" if tipp == ziel else "2" if tipp > ziel else "3")
        elif line.startswith(t["nochmal"]):
            counts.append(n); n = 0; runde += 1
            sende(t["ja"] if runde < len(ziele) else t["nein"])
    try:
        p.stdin.close()
    except Exception:
        pass
    return p.wait(timeout=timeout), counts, time.time() - t0


ZIELE = list(range(100))
gut = True
print("%-24s %5s %6s %6s %9s %6s %7s" % ("Programm", "Spr.", "exit", "min", "schnitt", "max", "Sek."))
print("-" * 70)
for src in PROGRAMME:
    for lang in ("de", "en"):
        rc, c, dt = durchlauf(lang, src, ZIELE)
        ok = rc == 0 and len(c) == 100
        gut &= ok
        print("%-24s %5s %6s %6d %9.2f %6d %7.1f %s" % (
            src, lang, rc, min(c), sum(c) / len(c), max(c), dt,
            "" if ok else "  <-- nur %d Runden!" % len(c)), flush=True)
sys.exit(0 if gut else 1)
