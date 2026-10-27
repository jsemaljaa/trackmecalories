"""
Builds the bundled base-food list (raw and basic cooked foods with portion sizes) from two
open food composition databases:

- Fineli, the Finnish national food composition database (THL), CC BY 4.0. fineli.fi is behind
  a bot check, so the CSVs are read from the theel0ja/fineli-data mirror of its open-data
  "basic package 2" (release 18). Its foodaddunit table has small/medium/large piece weights.
- USDA FoodData Central, SR Legacy (public domain): fills in foods Fineli lacks and portion
  sizes (e.g. small/medium/large egg) where Fineli has none.

The curated list is tools/base_foods.txt: which source food each entry uses, its portions, and
its English, Russian and Czech names. Run from the repo root:

    python tools/build_base_foods.py            # -> app/src/main/assets/base_foods.json
    python tools/build_base_foods.py --check    # print every entry's source food for review

Downloads (~8 MB) go to a temp directory unless --fineli/--usda point at extracted copies.
Stdlib only.
"""

import argparse
import collections
import csv
import io
import json
import os
import re
import sys
import tempfile
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIST = os.path.join(ROOT, "tools", "base_foods.txt")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "base_foods.json")

FINELI_MIRROR = "https://raw.githubusercontent.com/theel0ja/fineli-data/master/basic-package-2/"
FINELI_FILES = ["foodname_EN.csv", "component_value.csv", "foodaddunit.csv"]
USDA_ZIP = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip"
USDA_FILES = ["food.csv", "food_nutrient.csv", "food_portion.csv"]

# Portion keys the app knows how to label (strings.xml: portion_<key>).
PORTION_KEYS = ["small", "medium", "large", "extra_large", "piece", "slice", "clove", "tbsp", "tsp", "glass", "cup"]
# Pre-selected portion when logging, most natural first.
DEFAULT_ORDER = ["medium", "piece", "slice", "clove", "glass", "cup", "large", "small", "extra_large", "tbsp", "tsp"]

FINELI_UNITS = {"KPL_S": "small", "KPL_M": "medium", "KPL_L": "large", "RKL": "tbsp", "TL": "tsp"}

# USDA portion modifiers ("large", "Potato medium (2-1/4\" ...)", "slice", "tbsp") -> our keys.
USDA_MODIFIERS = [
    (re.compile(r"^(\w+ )?extra large\b"), "extra_large"),
    (re.compile(r"^(\w+ )?large\b"), "large"),
    (re.compile(r"^(\w+ )?medium\b"), "medium"),
    (re.compile(r"^(\w+ )?small\b"), "small"),
    (re.compile(r"^slice\b"), "slice"),
    (re.compile(r"^clove\b"), "clove"),
    (re.compile(r"^tbsp\b"), "tbsp"),
    (re.compile(r"^tsp\b"), "tsp"),
]


def fetch(url):
    print(f"  downloading {url}", file=sys.stderr)
    req = urllib.request.Request(url, headers={"User-Agent": "TrackMeCalories build script"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def download(tmp):
    fineli = os.path.join(tmp, "fineli")
    usda = os.path.join(tmp, "usda")
    os.makedirs(fineli, exist_ok=True)
    os.makedirs(usda, exist_ok=True)
    for name in FINELI_FILES:
        with open(os.path.join(fineli, name), "wb") as f:
            f.write(fetch(FINELI_MIRROR + name))
    with zipfile.ZipFile(io.BytesIO(fetch(USDA_ZIP))) as z:
        for member in z.namelist():
            if os.path.basename(member) in USDA_FILES:
                with open(os.path.join(usda, os.path.basename(member)), "wb") as f:
                    f.write(z.read(member))
    return fineli, usda


def num(s):
    return float(s.replace(",", ".")) if s and s.strip() else 0.0


class Fineli:
    def __init__(self, path):
        def rows(name):
            with open(os.path.join(path, name), encoding="latin-1", newline="") as f:
                return list(csv.DictReader(f, delimiter=";"))

        self.names = {r["FOODID"]: r["FOODNAME"] for r in rows("foodname_EN.csv")}
        self.values = collections.defaultdict(dict)
        for r in rows("component_value.csv"):
            self.values[r["FOODID"]][r["EUFDNAME"]] = num(r["BESTLOC"])
        self.units = collections.defaultdict(dict)
        for r in rows("foodaddunit.csv"):
            self.units[r["FOODID"]][r["FOODUNIT"]] = num(r["MASS"])

    def name(self, fid):
        return self.names[fid]

    def nutrients(self, fid):
        v = self.values[fid]
        return {
            "kcal": v.get("ENERC", 0) / 4.184,  # kJ
            "protein": v.get("PROT", 0),
            "carbs": v.get("CHOAVL", 0),  # available carbohydrate, as on EU labels
            "fat": v.get("FAT", 0),
            "fiber": v.get("FIBC", 0),
            "salt": v.get("NACL", 0) / 1000,  # mg
            "sugar": v.get("SUGAR"),
            "satFat": v.get("FASAT"),
        }

    def portions(self, fid):
        units = self.units[fid]
        out = {FINELI_UNITS[u]: g for u, g in units.items() if u in FINELI_UNITS and g > 0}
        # A lone "medium-sized piece" is just "a piece" (an egg, a garlic clove).
        if "medium" in out and "small" not in out and "large" not in out:
            out["piece"] = out.pop("medium")
        return out


class Usda:
    def __init__(self, path):
        def rows(name):
            with open(os.path.join(path, name), encoding="utf-8", newline="") as f:
                return list(csv.DictReader(f))

        self.names = {r["fdc_id"]: r["description"] for r in rows("food.csv")}
        wanted = {"1008": "kcal", "1003": "protein", "1005": "carbs", "1004": "fat",
                  "1079": "fiber", "1093": "sodium", "2000": "sugar", "1258": "satFat"}
        self.values = collections.defaultdict(dict)
        for r in rows("food_nutrient.csv"):
            key = wanted.get(r["nutrient_id"])
            if key:
                self.values[r["fdc_id"]][key] = num(r["amount"])
        self.raw_portions = collections.defaultdict(list)
        for r in rows("food_portion.csv"):
            self.raw_portions[r["fdc_id"]].append((num(r["amount"]), r["modifier"].strip().lower(), num(r["gram_weight"])))

    def name(self, fid):
        return self.names[fid]

    def nutrients(self, fid):
        v = self.values[fid]
        fiber = v.get("fiber", 0)
        return {
            "kcal": v.get("kcal", 0),
            "protein": v.get("protein", 0),
            # USDA carbs are "by difference" and include fiber; EU labels (and the rest of the
            # app's data) don't.
            "carbs": max(v.get("carbs", 0) - fiber, 0),
            "fat": v.get("fat", 0),
            "fiber": fiber,
            "salt": v.get("sodium", 0) * 2.5 / 1000,
            "sugar": v.get("sugar"),
            "satFat": v.get("satFat"),
        }

    def portions(self, fid):
        out = {}
        for amount, modifier, grams in self.raw_portions[fid]:
            if amount <= 0 or grams <= 0:
                continue
            for rx, key in USDA_MODIFIERS:
                if rx.search(modifier) and key not in out:
                    out[key] = grams / amount
                    break
        return out

    def describe_portions(self, fid):
        return "; ".join(f"{a:g} {m}={g:g}" for a, m, g in self.raw_portions[fid])


def source(ref, fineli, usda):
    """'F300' -> (fineli, '300'), 'U171287' -> (usda, '171287')."""
    db = {"F": fineli, "U": usda}.get(ref[:1])
    if db is None or ref[1:] not in db.names:
        raise SystemExit(f"Unknown source food {ref!r}")
    return db, ref[1:]


# Nutri-Score 2023 food groups the app knows (data/NutritionRating.kt, FoodCategory.key).
CATEGORIES = {"general", "beverage", "water", "fat", "cheese", "red_meat", "alcohol"}


def parse_nutri(spec, line_no, base=None):
    """'cat=fat fvl=100' -> {"cat": "fat", "fvl": 100.0}, on top of [base]."""
    nutri = dict(base or {"cat": "general", "fvl": 0.0})
    for token in spec.split():
        name, _, value = token.partition("=")
        if name == "cat" and value in CATEGORIES:
            nutri["cat"] = value
        elif name == "fvl":
            nutri["fvl"] = float(value)
        else:
            raise SystemExit(f"base_foods.txt:{line_no}: bad nutri token {token!r}")
    return nutri


def read_list():
    entries = []
    nutri = parse_nutri("", 0)
    with open(LIST, encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if line.startswith("@"):
                # "@ cat=fat fvl=100": the Nutri-Score group for the lines below.
                nutri = parse_nutri(line[1:], line_no)
                continue
            if not line.strip() or line.startswith("#"):
                continue
            cols = line.split("|")
            if len(cols) < 6:
                raise SystemExit(f"base_foods.txt:{line_no}: expected 6-8 |-separated columns, got {len(cols)}")
            key, src, portions, en, ru, cs = (c.strip() for c in cols[:6])
            aliases = cols[6].strip() if len(cols) > 6 else ""
            own = parse_nutri(cols[7], line_no, nutri) if len(cols) > 7 else nutri
            entries.append(dict(line=line_no, key=key, src=src, portions=portions, en=en, ru=ru, cs=cs, aliases=aliases,
                                cat=own["cat"], fvl=own["fvl"]))
    keys = [e["key"] for e in entries]
    dupes = {k for k in keys if keys.count(k) > 1}
    if dupes:
        raise SystemExit(f"Duplicate keys in base_foods.txt: {sorted(dupes)}")
    return entries


def resolve_portions(spec, src_db, src_id, fineli, usda):
    """
    Portions column: empty = the source food's own; "-" = none; otherwise space-separated
    tokens applied in order: a food reference (F123 / U123) takes that food's portions, and
    key=grams sets one explicitly.
    """
    if spec == "-":
        return {}
    if not spec:
        return src_db.portions(src_id)
    out = {}
    for token in spec.split():
        if "=" in token:
            key, grams = token.split("=", 1)
            if key not in PORTION_KEYS:
                raise SystemExit(f"Unknown portion key {key!r}")
            out[key] = float(grams)
        else:
            db, fid = source(token, fineli, usda)
            out.update(db.portions(fid))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--fineli", help="directory with Fineli basic package 2 CSVs")
    ap.add_argument("--usda", help="directory with extracted SR Legacy CSVs")
    ap.add_argument("--check", action="store_true", help="print each entry's sources instead of writing JSON")
    args = ap.parse_args()

    fineli_dir, usda_dir = args.fineli, args.usda
    if not (fineli_dir and usda_dir):
        fineli_dir, usda_dir = download(tempfile.mkdtemp(prefix="base_foods_"))
    fineli, usda = Fineli(fineli_dir), Usda(usda_dir)

    foods = []
    for e in read_list():
        db, fid = source(e["src"], fineli, usda)
        n = db.nutrients(fid)
        portions = resolve_portions(e["portions"], db, fid, fineli, usda)
        unknown = set(portions) - set(PORTION_KEYS)
        if unknown:
            raise SystemExit(f"{e['key']}: unknown portion keys {unknown}")
        ordered = {k: round(portions[k], 1) for k in PORTION_KEYS if k in portions}
        default = next((k for k in DEFAULT_ORDER if k in ordered), None)
        if args.check:
            extra = f"   [usda portions: {usda.describe_portions(fid)}]" if db is usda and usda.raw_portions[fid] else ""
            print(f"{e['key']:<24} {n['kcal']:5.0f} kcal  P{n['protein']:5.1f} C{n['carbs']:5.1f} F{n['fat']:5.1f}  "
                  f"{e['src']} {db.name(fid)}  ->  {ordered}{extra}")
            continue
        food = {
            "key": e["key"],
            "en": e["en"],
            "ru": e["ru"],
            "cs": e["cs"],
            "unit": "ml" if "glass" in ordered or "cup" in ordered else "g",
            "kcal": round(n["kcal"], 1),
            "protein": round(n["protein"], 2),
            "carbs": round(n["carbs"], 2),
            "fat": round(n["fat"], 2),
            "fiber": round(n["fiber"], 2),
            "salt": round(n["salt"], 3),
            "source": ("fineli:" if db is fineli else "usda:") + fid,
        }
        if n["sugar"] is not None:
            food["sugar"] = round(n["sugar"], 2)
        if n["satFat"] is not None:
            food["satFat"] = round(n["satFat"], 2)
        if e["aliases"]:
            food["aliases"] = e["aliases"]
        if e["cat"] != "general":
            food["cat"] = e["cat"]
        if e["fvl"]:
            food["fvl"] = e["fvl"]
        if ordered:
            food["portions"] = ordered
            food["defaultPortion"] = default
        foods.append(food)

    if args.check:
        return
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({
            "attribution": "Fineli, Finnish Institute for Health and Welfare (THL), CC BY 4.0; "
                           "USDA FoodData Central, SR Legacy (public domain)",
            "foods": foods,
        }, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Wrote {len(foods)} foods to {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
