# -*- coding: utf-8 -*-
KEY = "ILoveMyTeacher"

MSG = [
 "Denke dir eine Zahl zwischen 0 und 99 und drücke dann Enter.",   # 0
 "Ok. Ich beginne jetzt zu raten.",                                     # 1
 "Antworte mir mit 1 für richtig, 2 für kleiner und 3 für grösser.", # 2
 "Ist es die ",                                                         # 3
 "?",                                                                   # 4
 "Das war ja einfach!",                                                 # 5
 "Geschafft.",                                                          # 6
 "Uff, das war schwierig.",                                             # 7
 " (",                                                                  # 8
 " Versuche)",                                                          # 9
 "Hmm, deine Antworten passen nicht zusammen. Hast du geschummelt?",    # 10
 "Bitte 1 (richtig), 2 (kleiner) oder 3 (grösser) eingeben.",      # 11
 "Spielen wir nochmal? (j/n)",                                          # 12
 "Bis zum nächsten Mal!",                                          # 13
 "=== Ein Rateprogramm, das nichts sucht, sondern fragt ===",           # 14
 "Das grenzt schon fast an künstliche Intelligenz.",               # 15
 "Gespielte Runden: ",                                                  # 16
 "Insgesamt gestellte Fragen: ",                                        # 17
 " Versuch)",                                        # 17
]

plain = "\u0000".join(MSG)
assert all(ord(c) <= 0xFF for c in plain), "nur Latin-1 erlaubt"
enc = "".join("%02x" % (ord(c) ^ ord(KEY[i % len(KEY)])) for i, c in enumerate(plain))
assert "\\u" not in enc

lines = [enc[i:i+96] for i in range(0, len(enc), 96)]
block = "\n".join("            " + l for l in lines)
open("payload.txt", "w").write(block)
print("Zeichen:", len(plain), " Hex:", len(enc), " Zeilen:", len(lines))
