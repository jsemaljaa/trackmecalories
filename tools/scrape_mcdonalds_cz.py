"""
Scrapes official per-portion nutrition values from mcdonalds.cz product pages and writes
them to the app's bundled restaurant menu asset.

Usage (from the repo root):
    python tools/scrape_mcdonalds_cz.py

Only uses the standard library. Re-run whenever McDonald's changes its menu, then rebuild.
"""
import html
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

BASE = "https://www.mcdonalds.cz"
CATEGORIES = [
    "hovezi-burgery", "kureci-a-ostatni", "prilohy", "salaty", "snidane",
    "dezerty", "napoje", "mccafe", "omacky-a-zalivky", "aktualni-nabidka",
    "happy-meal", "grimace",
]
OUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/restaurants/mcdonalds_cz.json"

# Row label on the product page -> our field name.
LABELS = {
    "tuky": "fat",
    "sacharidy": "carbs",
    "vláknina": "fiber",
    "bílkoviny": "protein",
    "sůl": "salt",
}


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (TrackMeCalories menu sync)"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8")


def text(fragment: str) -> str:
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", fragment))).strip()


def number(cell: str) -> float | None:
    m = re.search(r"([\d]+(?:[.,]\d+)?)", cell.replace("\xa0", ""))
    return float(m.group(1).replace(",", ".")) if m else None


def parse_table(table_html: str) -> dict | None:
    values = {}
    for row in re.findall(r"<tr>(.*?)</tr>", table_html, re.S):
        cells = [text(c) for c in re.findall(r"<td[^>]*>(.*?)</td>", row, re.S)]
        if len(cells) < 3 or not cells[0]:
            continue
        label = cells[0].lower()
        per_portion, per_100 = number(cells[1]), number(cells[2])
        if label == "energie" and "kcal" in cells[1]:
            values["kcal"] = per_portion
            values["kcal100"] = per_100
        elif label in LABELS:
            values[LABELS[label]] = per_portion
    if values.get("kcal") is None:
        return None
    return values


def parse_product(url: str) -> list[dict]:
    page = fetch(url)
    title_m = re.search(r'<h1 class="productTitle[^"]*">(.*?)</h1>', page, re.S)
    if not title_m:
        return []
    title = text(title_m.group(1))

    # Size/variant picker; attributes are spread over several lines.
    variant_labels = {
        value: text(label)
        for value, label in re.findall(r'<option\s+value="([^"]+)"[^>]*>(.*?)</option>', page, re.S)
    }

    # Each size variant (or the single default one, keyed "0") is its own block with a table.
    blocks = list(re.finditer(r'<div class="row ([\w-]+)[^"]*w-full">', page))
    results = []
    for i, block in enumerate(blocks):
        end = blocks[i + 1].start() if i + 1 < len(blocks) else len(page)
        table_m = re.search(r"<table.*?</table>", page[block.end():end], re.S)
        if not table_m:
            continue
        values = parse_table(table_m.group(0))
        if not values:
            continue
        key = block.group(1)
        variant = variant_labels.get(key, key) if len(blocks) > 1 else None
        if not variant or variant.lower() == title.lower():
            name = title
        elif "poleva" in variant.lower() or "posypka" in variant.lower():
            # McFlurry toppings list only the topping's own values, not the dessert + topping.
            name = f"{variant[0].upper()}{variant[1:]} (McFlurry topping)"
        else:
            name = f"{title} ({variant.lower()})"
        kcal100 = values.pop("kcal100", None)
        portion = round(values["kcal"] / kcal100 * 100) if kcal100 else None
        results.append({
            "name": name,
            "kcal": values["kcal"],
            "protein": values.get("protein", 0.0),
            "carbs": values.get("carbs", 0.0),
            "fat": values.get("fat", 0.0),
            "fiber": values.get("fiber", 0.0),
            "salt": values.get("salt", 0.0),
            "portionGrams": portion,
            "url": url,
        })
    return results


def main() -> int:
    product_urls = []
    for category in CATEGORIES:
        listing = fetch(f"{BASE}/menu/{category}/")
        for path in re.findall(rf'href="(/menu/{category}/[\w-]+/)"', listing):
            if path not in product_urls:
                product_urls.append(path)
        time.sleep(0.3)

    items, seen = [], set()
    for path in product_urls:
        try:
            for item in parse_product(BASE + path):
                if item["name"].lower() not in seen:
                    seen.add(item["name"].lower())
                    items.append(item)
        except Exception as e:  # one broken page shouldn't sink the whole sync
            print(f"skip {path}: {e}", file=sys.stderr)
        time.sleep(0.3)

    items.sort(key=lambda it: it["name"].lower())
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({
        "chain": "McDonald's",
        "source": "mcdonalds.cz",
        "fetchedAt": time.strftime("%Y-%m-%d"),
        "items": items,
    }, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(items)} items from {len(product_urls)} products -> {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
