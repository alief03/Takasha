import os, glob, json

# 1. Check all JSON files in resources
res_dir = 'sakura-weapons/src/main/resources/assets'
json_count = 0
for root, dirs, files in os.walk(res_dir):
    for f in files:
        if f.endswith('.json'):
            json_count += 1
            p = os.path.join(root, f)
            with open(p, 'r', encoding='utf-8-sig') as jf:
                try:
                    data = json.load(jf)
                except Exception as e:
                    print(f'JSON ERROR in {p}: {e}')
                    raise
print(f'Parsed {json_count} JSON files successfully!')

# 2. Check duplicates in when conditions in minecraft/items
items_dir = os.path.join(res_dir, 'minecraft', 'items')
has_dup = False
for f in os.listdir(items_dir):
    if f.endswith('.json'):
        p = os.path.join(items_dir, f)
        with open(p, 'r', encoding='utf-8-sig') as jf:
            data = json.load(jf)
        model = data.get('model', {})
        cases = model.get('cases', [])
        for i, c in enumerate(cases):
            when = c.get('when', [])
            if isinstance(when, list):
                if len(when) != len(set(when)):
                    has_dup = True
                    dups = [x for x in when if when.count(x) > 1]
                    print(f'DUPLICATE in {f} case {i}: {set(dups)}')
if not has_dup:
    print('All item models have 100% unique when entries!')

# 3. Verify elytra.json and paper.json have Valentine Wing
for target in ['elytra.json', 'paper.json']:
    p = os.path.join(items_dir, target)
    with open(p, 'r', encoding='utf-8') as jf:
        data = json.load(jf)
    found = False
    for c in data['model']['cases']:
        if c.get('model', {}).get('model') == 'valentine:item/wing':
            assert 'Valentine Wing' in c['when'], f'Valentine Wing missing in {target}'
            assert 'Valentine Wings' in c['when'], f'Valentine Wings missing in {target}'
            assert 'Sayap Valentine' in c['when'], f'Sayap Valentine missing in {target}'
            found = True
            print(f'{target}: verified valentine:item/wing present with {len(c["when"])} keywords.')
    assert found, f'valentine:item/wing NOT found in {target}'

# 4. Check for any remaining quiver references in codebase
remaining_quiver = []
for root, dirs, files in os.walk('sakura-weapons/src/main'):
    for f in files:
        p = os.path.join(root, f)
        try:
            with open(p, 'r', encoding='utf-8') as sf:
                if 'quiver' in sf.read().lower():
                    remaining_quiver.append(p)
        except:
            pass
if remaining_quiver:
    print('Remaining quiver references found in:', remaining_quiver)
else:
    print('Zero quiver references remain in sakura-weapons/src/main! Completely clean.')
