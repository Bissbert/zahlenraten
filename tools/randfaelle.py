# -*- coding: utf-8 -*-
"""Randfaelle fuer alle Programme in beiden Sprachen.
   Edge cases for every program in both languages."""
import os, subprocess, re, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src")

# "(3 Versuche)" / "(1 guess)" - klammert das Ergebnis, damit z.B. "guessing" nicht zaehlt
ERGEBNIS = re.compile(r"\(\d+ (?:Versuch|Versuche|guess|guesses)\)")

SPRACHEN = {
    "de": dict(start="Denke dir", frage=re.compile(r"Ist es die (\d+)\?"),
               nochmal="Spielen wir nochmal?", bitte="Bitte 1",
               ja="j", nein="n", schummeln="geschummelt"),
    "en": dict(start="Think of",  frage=re.compile(r"Is it (\d+)\?"),
               nochmal="Shall we play again?", bitte="Please enter",
               ja="y", nein="n", schummeln="cheat"),
}

PROGRAMME = ["Rater.java", "NeuralGuesser.java", "NG.java",
             "Verflucht.java", "ILoveMyTeacher.java"]


def spiele(lang, src, ziele, vorlauf=(), timeout=300):
    t = SPRACHEN[lang]
    p = subprocess.Popen(["java", os.path.join(SRC, lang, src)],
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=1)
    log, runde, offen, tipp = [], 0, list(vorlauf), 0

    def sende(s):
        try:
            p.stdin.write(s + "\n"); p.stdin.flush()
        except Exception:
            pass

    def antworte():
        if offen:
            sende(offen.pop(0)); return
        z = ziele[min(runde, len(ziele) - 1)]
        sende("2" if z == "tief" else "3" if z == "hoch"
              else "1" if tipp == z else "2" if tipp > z else "3")

    while True:
        line = p.stdout.readline()
        if not line:
            break
        line = line.rstrip("\n"); log.append(line)
        if line.startswith(t["start"]):
            sende("")
        elif t["frage"].search(line):
            tipp = int(t["frage"].search(line).group(1)); antworte()
        elif line.startswith(t["bitte"]):
            antworte()
        elif line.startswith(t["nochmal"]):
            runde += 1
            sende(t["ja"] if runde < len(ziele) else t["nein"])
    try:
        p.stdin.close()
    except Exception:
        pass
    return p.wait(timeout=timeout), "\n".join(log)


def eof(lang, src, eingabe, timeout=300):
    p = subprocess.run(["java", os.path.join(SRC, lang, src)], input=eingabe,
                       capture_output=True, text=True, encoding="utf-8", timeout=timeout)
    return p.returncode, p.stdout


gut = True
for src in PROGRAMME:
    for lang in ("de", "en"):
        t = SPRACHEN[lang]
        r = []
        rc, txt = spiele(lang, src, ["tief"])
        r.append(("schummeln-tief", rc == 0 and t["schummeln"] in txt))
        rc, txt = spiele(lang, src, ["hoch"])
        r.append(("schummeln-hoch", rc == 0 and t["schummeln"] in txt))
        rc, txt = spiele(lang, src, [48], vorlauf=["x", "banane", "4", ""])
        r.append(("Muell", rc == 0 and txt.count(t["bitte"]) == 4 and ERGEBNIS.search(txt) is not None))
        rc, txt = spiele(lang, src, [48, 0, 99])
        r.append(("3-Runden", rc == 0 and len(ERGEBNIS.findall(txt)) >= 3))
        rc, txt = eof(lang, src, "")
        r.append(("EOF-sofort", rc == 0))
        rc, txt = eof(lang, src, "\n2\n3\n")
        r.append(("EOF-mitten", rc == 0))
        ok = all(v for _, v in r); gut &= ok
        print(("OK   " if ok else "FAIL ") + "%-22s %s  " % (src, lang) + "  ".join(
            "%s:%s" % (n, "ok" if v else "FAIL") for n, v in r), flush=True)
sys.exit(0 if gut else 1)
