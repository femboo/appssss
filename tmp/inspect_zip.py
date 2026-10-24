import zipfile
import re

with zipfile.ZipFile("public/assets/aistudio/static_2493d0b0.zip") as z:
    for name in ["static/index.js", "static/app.js", "static/index.html"]:
        try:
            content = z.read(name).decode("utf-8", errors="ignore")
            print(f"=== {name} ===")
            eps = set(re.findall(r"[/\"'](api/[a-zA-Z0-9_\-\/]+)", content))
            for ep in sorted(eps):
                print("  ", ep)
            lyrics = [l for l in content.splitlines() if "lyric" in l.lower() or "текст" in l.lower()]
            print("Lyrics lines:", len(lyrics))
            for l in lyrics[:8]:
                print("   LYRIC:", l.strip()[:140])
        except Exception as e:
            print(e)
