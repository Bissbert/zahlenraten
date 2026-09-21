# -*- coding: utf-8 -*-
"""Erzeugt alle animierten GIFs in media/.  Aufruf:  python3 tools/media_bauen.py"""
import os
from PIL import Image, ImageDraw, ImageFont

HIER  = os.path.dirname(os.path.abspath(__file__))
MEDIA = os.path.join(HIER, "..", "media")

BG, PANEL, RAHMEN = "#0d1117", "#161b22", "#30363d"
TEXT, GRAU, GRUEN, GELB, BLAU, ROT, LILA = (
    "#c9d1d9", "#8b949e", "#3fb950", "#d29922", "#58a6ff", "#f85149", "#bc8cff")

def font(px, fett=False):
    return ImageFont.truetype("/System/Library/Fonts/Menlo.ttc", px, index=1 if fett else 0)

F13, F15, F17, F15B, F11 = font(13), font(15), font(17), font(15, True), font(11)

def speichern(name, frames, dauern):
    os.makedirs(MEDIA, exist_ok=True)
    p = os.path.join(MEDIA, name)
    pal = [f.convert("P", palette=Image.ADAPTIVE, colors=64) for f in frames]
    pal[0].save(p, save_all=True, append_images=pal[1:], duration=dauern, loop=0, optimize=True)
    print("  %-22s %3d Frames  %6.1f kB" % (name, len(frames), os.path.getsize(p) / 1024))

def fenster(b, h, titel):
    img = Image.new("RGB", (b, h), BG)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([8, 8, b - 9, h - 9], 8, fill=PANEL, outline=RAHMEN)
    d.rounded_rectangle([8, 8, b - 9, 40], 8, fill="#21262d", outline=RAHMEN)
    d.rectangle([9, 32, b - 10, 41], fill="#21262d")
    d.line([9, 41, b - 10, 41], fill=RAHMEN)
    for i, c in enumerate(("#ff5f56", "#ffbd2e", "#27c93f")):
        d.ellipse([24 + i * 18, 18, 34 + i * 18, 28], fill=c)
        
    d.text((92, 17), titel, font=F13, fill=GRAU)
    return img, d

# --------------------------------------------------------------------------
# 1) spiel.gif - Terminalmitschnitt einer echten Partie (Rater, Zahl 48)
# --------------------------------------------------------------------------
def spiel():
    Z = [("Denke dir eine Zahl zwischen 0 und 99 und drücke dann Enter.", TEXT, 900),
         ("", TEXT, 250), ("Ok. Ich beginne jetzt zu raten.", TEXT, 500),
         ("Antworte mir mit 1 für richtig, 2 für kleiner und 3 für grösser.", TEXT, 900),
         ("", TEXT, 200)]
    for tipp, ant, wort in [(49, "2", "kleiner"), (24, "3", "grösser"), (36, "3", "grösser"),
                            (42, "3", "grösser"), (45, "3", "grösser"), (47, "3", "grösser"),
                            (48, "1", "richtig")]:
        Z.append(("Ist es die %d?" % tipp, TEXT, 550))
        Z.append(("%s          « %s" % (ant, wort), GELB, 650))
    Z += [("", TEXT, 200), ("Uff, das war schwierig. (7 Versuche)", GRUEN, 700),
          ("Das grenzt schon fast an künstliche Intelligenz.", GRUEN, 2600)]

    B, H, Y0, DY = 790, 566, 58, 21
    frames, dauern = [], []
    for n in range(len(Z) + 1):
        img, d = fenster(B, H, "java src/Rater.java")
        d.rounded_rectangle([B - 196, 52, B - 22, 76], 5, fill="#1f2937", outline=BLAU)
        d.text((B - 186, 58), "gedachte Zahl: 48", font=F13, fill=BLAU)
        for i, (t, c, _) in enumerate(Z[:n]):
            d.text((26, Y0 + i * DY), t, font=F15, fill=c)
        if n < len(Z):
            d.rectangle([26, Y0 + n * DY + 2, 34, Y0 + n * DY + 17], fill=GRAU)
        frames.append(img)
        dauern.append(400 if n == 0 else Z[n - 1][2])
    speichern("spiel.gif", frames, dauern)

# --------------------------------------------------------------------------
# 2) intervall.gif - wie sich die binäre Suche den Bereich halbiert
# --------------------------------------------------------------------------
def intervall():
    SCHRITTE = [(0, 99, 49, "kleiner"), (0, 48, 24, "grösser"), (25, 48, 36, "grösser"),
                (37, 48, 42, "grösser"), (43, 48, 45, "grösser"), (46, 48, 47, "grösser"),
                (48, 48, 48, "richtig")]
    B, H, ZB, X0, YB = 790, 300, 7, 40, 150
    frames, dauern = [], []

    def zeichne(unten, oben, tipp, antwort, gezeigt):
        img, d = fenster(B, H, "binäre Suche — der Bereich halbiert sich")
        d.text((26, 58), "Der Computer merkt sich, welche Zahlen noch möglich sind.",
               font=F13, fill=GRAU)
        for z in range(100):
            x = X0 + z * ZB
            drin = unten <= z <= oben
            f = "#1f6feb" if drin else "#21262d"
            if gezeigt and z == tipp: f = GELB if antwort != "richtig" else GRUEN
            d.rectangle([x, YB, x + ZB - 2, YB + 26], fill=f)
        for z in (0, 25, 50, 75, 99):
            d.text((X0 + z * ZB - 4, YB + 32), str(z), font=F11, fill=GRAU)
        if gezeigt:
            x = X0 + tipp * ZB + ZB // 2
            d.polygon([(x - 6, YB - 14), (x + 6, YB - 14), (x, YB - 3)],
                      fill=GELB if antwort != "richtig" else GRUEN)
            d.text((26, 96), "Tipp: die Mitte, also %d" % tipp, font=F15B, fill=GELB)
            d.text((300, 96), "Antwort: %s" % antwort, font=F15B,
                   fill=GRUEN if antwort == "richtig" else BLAU)
        anz = oben - unten + 1
        d.text((26, YB + 62), "noch möglich: %3d Zahlen   [%d, %d]" % (anz, unten, oben),
               font=F15, fill=TEXT)
        d.text((26, YB + 86), "Versuche bisher: %d" % versuche, font=F15, fill=GRAU)
        return img

    versuche = 0
    for unten, oben, tipp, antwort in SCHRITTE:
        frames.append(zeichne(unten, oben, tipp, antwort, False)); dauern.append(550)
        versuche += 1
        frames.append(zeichne(unten, oben, tipp, antwort, True)); dauern.append(1100)
    img, d = fenster(B, H, "binäre Suche — gefunden")
    d.text((26, 58), "Sieben Fragen reichen für hundert Zahlen:  2^7 = 128 > 100.",
           font=F13, fill=GRAU)
    for z in range(100):
        x = X0 + z * ZB
        d.rectangle([x, YB, x + ZB - 2, YB + 26], fill=GRUEN if z == 48 else "#21262d")
    d.text((26, 96), "Gefunden: 48", font=F17, fill=GRUEN)
    d.text((26, YB + 62), "7 Versuche — das theoretische Optimum.", font=F15, fill=TEXT)
    frames.append(img); dauern.append(3000)
    speichern("intervall.gif", frames, dauern)

# --------------------------------------------------------------------------
# 3) lernkurve.gif - echte Messwerte eines NeuralGuesser-Laufs
# --------------------------------------------------------------------------
LAUF = [(1, .4281, 7.41), (250, .5386, 6.48), (500, .5026, 6.77), (750, .4977, 6.36),
        (1000, .5131, 6.47), (1250, .5158, 6.44), (1500, .4935, 5.77), (1750, .5128, 6.17),
        (2000, .5021, 6.23), (2250, .4999, 5.97), (2500, .5054, 5.94), (2750, .5034, 5.84),
        (3000, .5024, 5.44)]

def lernkurve():
    B, H = 790, 470
    PX, PB = 70, 570
    frames, dauern = [], []

    def achse(d, ty, oy, oh, lo, hi, ziel, zielbez, titel):
        d.text((26, ty), titel, font=F15B, fill=TEXT)
        d.rectangle([PX, oy, PX + PB, oy + oh], outline=RAHMEN)
        yz = oy + oh - (ziel - lo) / (hi - lo) * oh
        for x in range(PX, PX + PB, 12):
            d.line([x, yz, x + 6, yz], fill=LILA)
        d.text((PX + PB + 10, yz - 7), zielbez, font=F11, fill=LILA)
        for v in (lo, hi):
            y = oy + oh - (v - lo) / (hi - lo) * oh
            d.text((28, y - 7), ("%.2f" if hi < 2 else "%.1f") % v, font=F11, fill=GRAU)
        return lambda it, w: (PX + it / 3000 * PB, oy + oh - (w - lo) / (hi - lo) * oh)

    for n in range(1, len(LAUF) + 1):
        img, d = fenster(B, H, "NeuralGuesser — das Netz lernt die Mitte (echte Messwerte)")
        it, mu, vs = LAUF[n - 1]
        d.text((26, 56), "Iteration %4d von 3000    mu = %.4f    %.2f Versuche/Spiel"
               % (it, mu, vs), font=F15, fill=GELB)
        p1 = achse(d, 92, 116, 110, .40, .60, .50, "0.50 = Mitte",
                   "mu — welchen Anteil der Spanne das Netz tippt")
        p2 = achse(d, 252, 276, 110, 5.0, 8.0, 5.80, "5.80 = Optimum",
                   "Versuche pro Spiel")
        for p, idx, farbe in ((p1, 1, BLAU), (p2, 2, GRUEN)):
            pts = [p(r[0], r[idx]) for r in LAUF[:n]]
            if len(pts) > 1:
                d.line(pts, fill=farbe, width=2, joint="curve")
            for q in pts:
                d.ellipse([q[0] - 3, q[1] - 3, q[0] + 3, q[1] + 3], fill=farbe)
        d.text((26, H - 34), "Die binäre Suche steht nirgends im Code. Das Netz findet sie selbst.",
               font=F13, fill=GRAU)
        frames.append(img); dauern.append(900 if n < len(LAUF) else 3000)
    speichern("lernkurve.gif", frames, dauern)

# --------------------------------------------------------------------------
# 4) orakel.gif - ILoveMyTeacher: der Mensch IST die Datenstruktur
# --------------------------------------------------------------------------
def orakel():
    B, H = 790, 420
    SP = [(120, "Collections", ".binarySearch", BLAU), (395, "ORAKEL", "Comparator", LILA),
          (660, "Mensch", "denkt an 48", GELB)]
    SCHRITTE = [(0, 1, "compare(49, null)", BLAU), (1, 2, "»Ist es die 49?«", GELB),
                (2, 1, "»2« — kleiner", GELB), (1, 0, "return +1", LILA),
                (0, 1, "compare(24, null)", BLAU), (1, 2, "»Ist es die 24?«", GELB),
                (2, 1, "»3« — grösser", GELB), (1, 0, "return -1", LILA),
                (-1, -1, "… noch fünf Fragen …", GRAU),
                (0, 1, "compare(48, null)", BLAU), (1, 2, "»Ist es die 48?«", GELB),
                (2, 1, "»1« — richtig", GRUEN), (1, 0, "return 0", GRUEN),
                (-2, -2, "binarySearch liefert 48", GRUEN)]
    frames, dauern = [], []
    gruppe = 0
    for n, (a, b, txt, farbe) in enumerate(SCHRITTE):
        if a <= 0:      # jeder neue compare-Aufruf beginnt eine neue Gruppe
            gruppe = n
        img, d = fenster(B, H, "ILoveMyTeacher — das Programm sucht nicht, es fragt")
        d.text((26, 58), "Eine Liste ohne Daten. Der Suchschlüssel ist null.",
               font=F13, fill=GRAU)
        for x, t1, t2, c in SP:
            d.rounded_rectangle([x - 100, 90, x + 100, 138], 6, fill="#1f2937", outline=c)
            d.text((x - 95, 97), t1, font=F15B, fill=c)
            d.text((x - 95, 116), t2, font=F11, fill=GRAU)
            d.line([x, 138, x, 310], fill=RAHMEN)
        start = gruppe
        for i in range(start, n + 1):
            aa, bb, tt, ff = SCHRITTE[i]
            aktuell = (i == n)
            c = ff if aktuell else "#484f58"
            y = 176 + (i - start) * 32
            if aa >= 0:
                x1, x2 = SP[aa][0], SP[bb][0]
                r = 1 if x2 > x1 else -1
                d.line([x1, y, x2 - 10 * r, y], fill=c, width=2 if aktuell else 1)
                d.polygon([(x2, y), (x2 - 11 * r, y - 5), (x2 - 11 * r, y + 5)], fill=c)
                d.text(((x1 + x2) // 2 - len(tt) * 4, y - 19), tt, font=F13, fill=c)
            else:
                d.text((B // 2 - len(tt) * 4, y), tt, font=F15B, fill=c)
        d.text((26, H - 60), "Der Mensch ist die Datenstruktur, in der gesucht wird.",
               font=F15B, fill=TEXT)
        d.text((26, H - 38), "Widersprüchliche Antworten? Dann liefert binarySearch einen "
               "negativen Einfügepunkt.", font=F11, fill=GRAU)
        frames.append(img); dauern.append(1800 if a < 0 else 1100)
    speichern("orakel.gif", frames, dauern)

if __name__ == "__main__":
    spiel(); intervall(); lernkurve(); orakel()
