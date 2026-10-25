"""
Builds offline menus for popular Czech food chains from kaloricketabulky.cz and writes
them to app/src/main/assets/restaurants/kt_<chain>.json (one file per chain).

Bundling them lets the app search these menus instantly, offline, accent-insensitively and
by word prefix ("don pacin" -> Don Pačínko), which the live Kalorické tabulky search can't
do. Each item is resolved to its natural serving ("kus (215 g)", "porce (216 g)") where
one exists, otherwise per 100 g/ml.

Usage (from the repo root):
    python tools/scrape_kaloricke_chains.py

Only uses the standard library. Re-run occasionally to pick up menu changes.
"""
import json
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

BASE = "https://www.kaloricketabulky.cz"
OUT_DIR = Path(__file__).resolve().parent.parent / "app/src/main/assets/restaurants"

# Display name -> KT search query. Items must contain the chain name as whole words.
CHAINS = {
    "Don Pačínko": "don pačínko",
    "Bageterie Boulevard": "bageterie boulevard",
    "Subway": "subway",
    "KFC": "kfc",
    "Burger King": "burger king",
    "Ugo": "ugo",
    "Paul": "paul",
    "Starbucks": "starbucks",
    "Costa Coffee": "costa coffee",
    "Pizza Hut": "pizza hut",
    "Domino's": "domino's",
    "Lokál": "lokál",
    "Albert Fresh Bistro": "fresh bistro",
}

BASE_UNIT_OPTION = re.compile(r"^\s*1?0*\s*(g|ml)\s*$")


def norm(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = re.sub(r"[̀-ͯ]+", "", s).lower()
    return re.sub(r"[^a-z0-9]+", " ", s).strip()


def get_json(path: str, **params):
    url = f"{BASE}/{path}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (TrackMeCalories menu sync)"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception:
            if attempt == 2:
                raise
            time.sleep(1.5 * (attempt + 1))


def czech_number(value) -> float | None:
    if value is None:
        return None
    try:
        return float(re.sub(r"[\s  ]", "", str(value)).replace(",", "."))
    except ValueError:
        return None


def strip_chain(title: str, chain_query: str) -> str:
    """'Caesar bageta Bageterie Boulevard' -> 'Caesar bageta' (the chain is shown as the brand)."""
    chain_words = norm(chain_query).split()
    words = title.split()
    normed = [norm(w) for w in words]
    for i in range(len(words) - len(chain_words) + 1):
        if normed[i:i + len(chain_words)] == chain_words:
            words = words[:i] + words[i + len(chain_words):]
            break
    name = re.sub(r"\s+", " ", " ".join(words)).strip(" -–,·")
    return (name[0].upper() + name[1:]) if name else title


def resolve(hit: dict) -> dict | None:
    unit = "ml" if hit.get("unit") == "ml" else "g"
    detail = get_json(f"foodstuff/detail/{hit['id']}/100/{unit}", format="json")
    food = detail["foodstuff"]
    kcal100 = czech_number(food.get("energy"))
    if kcal100 is None:
        return None
    if str(detail.get("energyUnit", "")).lower() == "kj":
        kcal100 /= 4.184

    portion = None
    try:
        form = get_json(f"foodstuff/detail/form/{hit['id']}", format="json", default="true")
        for option in form.get("unitOptions") or []:
            if not BASE_UNIT_OPTION.match(option.get("title") or "") and (option.get("multiplier") or 0) > 0:
                portion = (option["title"], float(option["multiplier"]))
                break
    except Exception:
        pass

    scale = portion[1] / 100 if portion else 1.0
    value = lambda key: round((czech_number(food.get(key)) or 0.0) * scale, 2)
    return {
        "ktId": hit["id"],
        "kcal": round(kcal100 * scale, 1),
        "protein": value("protein"),
        "carbs": value("carbohydrate"),
        "fat": value("fat"),
        "fiber": value("fiber"),
        "salt": value("salt"),
        "unit": "pcs" if portion else unit,
        "referenceAmount": 1.0 if portion else 100.0,
        "portion": portion[0] if portion else None,
    }


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for chain, query in CHAINS.items():
        hits = get_json("autocomplete/foodstuff-activity-meal", query=query, limit=500)
        needle = f" {norm(query)} "
        seen, picked = set(), []
        for hit in hits:
            if hit.get("clazz") != "foodstuff":
                continue
            haystack = f" {norm(hit['title'] + ' ' + (hit.get('brandName') or ''))} "
            if needle not in haystack:
                continue
            name = strip_chain(hit["title"], query)
            key = norm(name)
            if key in seen:  # KT has community duplicates; the first is the most used one
                continue
            seen.add(key)
            picked.append((name, hit))

        with ThreadPoolExecutor(max_workers=6) as pool:
            resolved = list(pool.map(lambda nh: (nh[0], resolve_safe(nh[1])), picked))

        items = [{"name": name, **data} for name, data in resolved if data]
        items.sort(key=lambda it: it["name"].lower())
        slug = norm(chain).replace(" ", "_")
        out = OUT_DIR / f"kt_{slug}.json"
        out.write_text(json.dumps({
            "chain": chain,
            "source": "kaloricketabulky.cz",
            "fetchedAt": time.strftime("%Y-%m-%d"),
            "items": items,
        }, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"{chain}: {len(items)} items -> {out.name}")
    return 0


def resolve_safe(hit: dict) -> dict | None:
    try:
        return resolve(hit)
    except Exception as e:
        print(f"  skip {hit.get('title')}: {e}", file=sys.stderr)
        return None


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main())
