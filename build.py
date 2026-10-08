"""
build.py — Takasha Resource Pack Builder
=========================================
Membuat Takasha-{MAJOR}.{MINOR}.{PATCH}.zip dari sakura-resourcepack/
Versi dibaca dari file VERSION di root project.

Usage:
  python build.py              # build dengan versi saat ini
  python build.py --patch      # build lalu bump PATCH  (1.0.0 -> 1.0.1)
  python build.py --minor      # build lalu bump MINOR  (1.0.1 -> 1.1.0)
  python build.py --major      # build lalu bump MAJOR  (1.1.0 -> 2.0.0)
"""

import zipfile
import pathlib
import sys
import datetime

ROOT    = pathlib.Path(__file__).parent
SRC     = ROOT / 'sakura-resourcepack'
VER_F   = ROOT / 'VERSION'

# ── helpers ──────────────────────────────────────────────────────────────────

def read_version() -> tuple[int, int, int]:
    raw = VER_F.read_text(encoding='utf-8').strip()
    parts = raw.split('.')
    if len(parts) != 3 or not all(p.isdigit() for p in parts):
        raise ValueError(f"VERSION harus berformat MAJOR.MINOR.PATCH, ditemukan: '{raw}'")
    return int(parts[0]), int(parts[1]), int(parts[2])

def write_version(major: int, minor: int, patch: int):
    VER_F.write_text(f"{major}.{minor}.{patch}\n", encoding='utf-8')

def bump(major: int, minor: int, patch: int, kind: str) -> tuple[int, int, int]:
    if kind == 'major': return major + 1, 0, 0
    if kind == 'minor': return major, minor + 1, 0
    if kind == 'patch': return major, minor, patch + 1
    raise ValueError(f"bump kind tidak dikenal: {kind}")

def build_zip(major: int, minor: int, patch: int) -> pathlib.Path:
    ver_str = f"{major}.{minor}.{patch}"
    out_dir = ROOT / 'Hasil RP'
    out_dir.mkdir(parents=True, exist_ok=True)
    out     = out_dir / f"Takasha-{ver_str}.zip"

    print(f"[build] Membuat {out.name} ...")
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for f in sorted(SRC.rglob('*')):
            if f.is_file():
                zf.write(f, f.relative_to(SRC))

    size_kb = out.stat().st_size // 1024
    ts      = datetime.datetime.now().strftime('%Y-%m-%d %H:%M')
    print(f"[build] OK  {out.name}  ({size_kb} KB)  [{ts}]")
    return out

# ── main ─────────────────────────────────────────────────────────────────────

def main():
    args     = sys.argv[1:]
    bump_arg = None

    for a in args:
        if a in ('--patch', '--minor', '--major'):
            bump_arg = a.lstrip('-')
        else:
            print(f"[build] Argumen tidak dikenal: {a}")
            print(__doc__)
            sys.exit(1)

    major, minor, patch = read_version()
    print(f"[build] Versi saat ini : {major}.{minor}.{patch}")

    build_zip(major, minor, patch)

    if bump_arg:
        new_major, new_minor, new_patch = bump(major, minor, patch, bump_arg)
        write_version(new_major, new_minor, new_patch)
        print(f"[build] Versi berikutnya: {new_major}.{new_minor}.{new_patch}  (disimpan ke VERSION)")

if __name__ == '__main__':
    main()
