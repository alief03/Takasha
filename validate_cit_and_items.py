"""
validate_cit_and_items.py
==========================
Validator integritas aset multi-set untuk Takasha Resource Pack:
1. Menvalidasi setiap file item definition (assets/minecraft/items/*.json)
   agar semua referensi model (<set_id>:item/<model>) ada di filesystem.
2. Memeriksa sinkronisasi 100% antara folder optifine/cit/<set_id>/ dan
   citresewn/cit/<set_id>/ untuk seluruh set yang ada.
"""

import json
import pathlib

root = pathlib.Path('d:/Mod Minecraft/weapon set/sakura/sakura-resourcepack')
items_dir = root / 'assets/minecraft/items'
assets_dir = root / 'assets'
opt_base = root / 'assets/minecraft/optifine/cit'
cit_base = root / 'assets/minecraft/citresewn/cit'

errors = []

# 1. Check all items JSON (Dynamic Multi-Set Namespace Support)
print('1. Checking items/*.json against multi-set models ...')
json_count = 0
for f in sorted(items_dir.glob('*.json')):
    json_count += 1
    try:
        data = json.loads(f.read_text(encoding='utf-8'))
        cases = data.get('model', {}).get('cases', [])
        for c in cases:
            m = c.get('model', {}).get('model', '')
            if ':' in m:
                set_id, model_path = m.split(':', 1)
                if model_path.startswith('item/'):
                    rel_name = model_path[len('item/'):] + '.json'
                    target_model = assets_dir / set_id / 'models/item' / rel_name
                    if not target_model.exists():
                        errors.append(f'File {f.name} references missing model in set "{set_id}": {target_model}')
    except Exception as e:
        errors.append(f'JSON error in {f.name}: {e}')
print(f'   Checked {json_count} items JSON files across all set namespaces.')

# 2. Check CIT properties sync across all sets
print('2. Checking optifine/cit vs citresewn/cit synchronization for all sets ...')
set_dirs = set()
if opt_base.exists():
    set_dirs.update(d.name for d in opt_base.iterdir() if d.is_dir())
if cit_base.exists():
    set_dirs.update(d.name for d in cit_base.iterdir() if d.is_dir())

total_props = 0
for s in sorted(set_dirs):
    opt_set = opt_base / s
    cit_set = cit_base / s

    opt_files = {f.name: f.read_text(encoding='utf-8') for f in opt_set.glob('*.properties')} if opt_set.exists() else {}
    cit_files = {f.name: f.read_text(encoding='utf-8') for f in cit_set.glob('*.properties')} if cit_set.exists() else {}

    if opt_files.keys() != cit_files.keys():
        errors.append(f'Set "{s}" mismatched property files: {set(opt_files.keys()) ^ set(cit_files.keys())}')

    for name in opt_files:
        if name in cit_files and opt_files[name] != cit_files[name]:
            errors.append(f'Set "{s}" content mismatch between optifine and citresewn for: {name}')

    total_props += len(opt_files)
    print(f'   Set "{s}": {len(opt_files)} properties synchronized.')

print(f'   Total properties verified: {total_props}')

if errors:
    print('\nERRORS FOUND:')
    for err in errors:
        print(' -', err)
else:
    print('\nALL MULTI-SET CHECKS PASSED PERFECTLY!')
