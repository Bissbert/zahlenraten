# -*- coding: utf-8 -*-
import os
import subprocess, re, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Einzelprogramm-Testbatterie; Sprache ueber argv waehlbar: python3 tools/treiber.py en
LANG = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] in ("de", "en") else "de"
SRC = os.path.join(REPO, "src", LANG, "ILoveMyTeacher.java")
FRAGE = re.compile(r"^(?:Ist es die|Is it) (\d+)\?$")

def spiele(plan, vorlauf=None, timeout=30):
    """plan: Liste von Geheimzahlen (eine pro Runde); None-Eintrag = schummeln mit '2'."""
    p = subprocess.Popen(["java", SRC], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=1)
    log, fragen, runde = [], [], 0
    def sende(s):
        log.append(">>> " + s); p.stdin.write(s + "\n"); p.stdin.flush()
    try:
        gesendet_start = False
        n = 0
        while True:
            line = p.stdout.readline()
            if not line: break
            line = line.rstrip("\n"); log.append(line)
            if line.startswith("Denke dir"):
                sende(""); gesendet_start = True
            elif FRAGE.match(line):
                tipp = int(FRAGE.match(line).group(1)); n += 1
                ziel = plan[runde] if runde < len(plan) else 0
                if ziel is None:            sende("2")
                elif ziel == "hoch":        sende("3")
                elif tipp == ziel:          sende("1")
                elif tipp > ziel:           sende("2")
                else:                       sende("3")
            elif line.startswith("Spielen wir nochmal?"):
                fragen.append(n); n = 0; runde += 1
                sende("j" if runde < len(plan) else "n")
            elif line.startswith("Bitte 1"):
                pass
        p.stdin.close()
    except BrokenPipeError:
        pass
    rc = p.wait(timeout=timeout)
    return rc, log, fragen

def pruefe(name, plan, erwartet_max=None, muss_enthalten=()):
    rc, log, fragen = spiele(plan)
    text = "\n".join(log)
    ok = (rc == 0)
    grund = [] if ok else ["exit=%d" % rc]
    if erwartet_max is not None:
        for f in fragen:
            if f > erwartet_max: ok = False; grund.append("%d Fragen > %d" % (f, erwartet_max))
    for m in muss_enthalten:
        if m not in text: ok = False; grund.append("fehlt: %r" % m)
    print(("  OK  " if ok else "  FAIL") + "  %-34s Fragen=%s %s" % (name, fragen, " ".join(grund)))
    if not ok: print("\n".join("      " + l for l in log))
    return ok

alles = True
alles &= pruefe("Zahl 48", [48], 7, ["Versuch", "Das grenzt schon fast"])
alles &= pruefe("Zahl 0",  [0],  7, ["Versuch"])
alles &= pruefe("Zahl 99", [99], 7, ["Versuch"])
alles &= pruefe("Zahl 7",  [7],  7, ["Versuch"])
alles &= pruefe("drei Runden 48/0/99", [48, 0, 99], 7, ["Gespielte Runden: 3"])
alles &= pruefe("schummeln: immer kleiner", [None], None, ["geschummelt"])
alles &= pruefe("schummeln: immer groesser", ["hoch"], None, ["geschummelt"])

# alle 100 Zahlen
print("  ...  alle 100 Zahlen")
schlimmste, fehler = 0, []
for z in range(100):
    rc, log, fragen = spiele([z])
    if rc != 0 or not fragen: fehler.append(z); continue
    schlimmste = max(schlimmste, fragen[0])
    if "Versuch" not in "\n".join(log): fehler.append(z)
print(("  OK  " if not fehler else "  FAIL") + "  alle 100 Zahlen gefunden, schlimmster Fall = %d Fragen %s"
      % (schlimmste, fehler))
alles &= not fehler
sys.exit(0 if alles else 1)
