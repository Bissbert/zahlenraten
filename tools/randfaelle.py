# -*- coding: utf-8 -*-
import os
import subprocess, re, sys
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIR = os.path.join(REPO, "src") + os.sep
FRAGE = re.compile(r"Ist es die (\d+)\?")

def spiele(src, ziele, vorlauf=(), timeout=300):
    p = subprocess.Popen(["java", DIR + src], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=1)
    log, runde, offen, tipp = [], 0, list(vorlauf), 0
    def sende(s):
        try: p.stdin.write(s + "\n"); p.stdin.flush()
        except Exception: pass
    def antworte():
        if offen: sende(offen.pop(0)); return
        z = ziele[min(runde, len(ziele) - 1)]
        sende("2" if z == "tief" else "3" if z == "hoch"
              else "1" if tipp == z else "2" if tipp > z else "3")
    while True:
        line = p.stdout.readline()
        if not line: break
        line = line.rstrip("\n"); log.append(line)
        if line.startswith("Denke dir"): sende("")
        elif FRAGE.search(line):
            tipp = int(FRAGE.search(line).group(1)); antworte()
        elif line.startswith("Bitte 1"): antworte()
        elif line.startswith("Spielen wir nochmal?"):
            runde += 1; sende("j" if runde < len(ziele) else "n")
    try: p.stdin.close()
    except Exception: pass
    return p.wait(timeout=timeout), "\n".join(log)

def eof(src, eingabe, timeout=300):
    p = subprocess.run(["java", DIR + src], input=eingabe, capture_output=True,
                       text=True, encoding="utf-8", timeout=timeout)
    return p.returncode, p.stdout

ALLE = ["Rater.java", "NeuralGuesser.java", "NG.java", "Verflucht.java", "ILoveMyTeacher.java"]
gut = True
for src in ALLE:
    r = []
    rc, t = spiele(src, ["tief"]); r.append(("schummeln-tief", rc == 0 and "geschummelt" in t))
    rc, t = spiele(src, ["hoch"]); r.append(("schummeln-hoch", rc == 0 and "geschummelt" in t))
    rc, t = spiele(src, [48], vorlauf=["x", "banane", "4", ""])
    r.append(("Muell", rc == 0 and t.count("Bitte 1") == 4 and "Versuch" in t))
    rc, t = spiele(src, [48, 0, 99]); r.append(("3-Runden", rc == 0 and t.count("Versuch") >= 3))
    rc, t = eof(src, ""); r.append(("EOF-sofort", rc == 0))
    rc, t = eof(src, "\n2\n3\n"); r.append(("EOF-mitten", rc == 0))
    ok = all(v for _, v in r); gut &= ok
    print(("OK   " if ok else "FAIL ") + "%-22s " % src + "  ".join(
        "%s:%s" % (n, "ok" if v else "FAIL") for n, v in r), flush=True)
sys.exit(0 if gut else 1)
