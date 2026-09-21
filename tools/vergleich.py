# -*- coding: utf-8 -*-
"""Spielt alle 100 Zahlen in EINEM Prozess (Wiederholungsschleife) durch."""
import os
import subprocess, re, sys, time

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIR = os.path.join(REPO, "src") + os.sep
FRAGE = re.compile(r"Ist es die (\d+)\?")

def durchlauf(src, ziele, timeout=600):
    p = subprocess.Popen(["java", DIR + src], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=1)
    counts, runde, n, tail = [], 0, 0, []
    t0 = time.time()
    def sende(s):
        try: p.stdin.write(s + "\n"); p.stdin.flush()
        except (BrokenPipeError, ValueError): pass
    while True:
        line = p.stdout.readline()
        if not line: break
        line = line.rstrip("\n"); tail.append(line); tail[:] = tail[-6:]
        if line.startswith("Denke dir"):
            sende("")
        elif FRAGE.search(line):
            tipp = int(FRAGE.search(line).group(1)); n += 1
            ziel = ziele[runde]
            sende("1" if tipp == ziel else "2" if tipp > ziel else "3")
        elif line.startswith("Spielen wir nochmal?"):
            counts.append(n); n = 0; runde += 1
            sende("j" if runde < len(ziele) else "n")
    try: p.stdin.close()
    except Exception: pass
    rc = p.wait(timeout=timeout)
    return rc, counts, time.time() - t0

ZIELE = list(range(100))
print("%-22s %6s %8s %8s %8s %7s" % ("Programm", "exit", "min", "schnitt", "max", "Sek."))
print("-" * 66)
for src in ["Rater.java", "NeuralGuesser.java", "NG.java", "Verflucht.java", "ILoveMyTeacher.java"]:
    rc, c, dt = durchlauf(src, ZIELE)
    ok = (rc == 0 and len(c) == 100)
    print("%-22s %6s %8d %8.2f %8d %7.1f %s" % (
        src, rc, min(c), sum(c)/len(c), max(c), dt, "" if ok else "  <-- %d Runden!" % len(c)))
