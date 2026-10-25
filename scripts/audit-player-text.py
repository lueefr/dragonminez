#!/usr/bin/env python3
"""Audit mod-owned component entry points and the bundled source-language catalog."""
from pathlib import Path
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
locale = root / 'src/main/resources/assets/dragonminez/lang/en_us.json'
pairs = json.loads(locale.read_text(), object_pairs_hook=lambda values: values)
translations = {}
errors = []
for key, value in pairs:
    if key in translations:
        errors.append(f'duplicate bundled key: {key}')
    if not isinstance(value, str):
        errors.append(f'non-string translation: {key}')
    translations[key] = value

missing = {}
component_files = 0
raw_data_calls = 0
static_calls = 0
for source in sorted((root / 'src/main/java/com/dragonminez').rglob('*.java')):
    if 'gametest' in source.parts:
        continue
    text = source.read_text()
    relative = str(source.relative_to(root))
    if re.search(r'Component\.translatable|ConfigTranslations\.text|\btr\(', text):
        component_files += 1
    raw_data_calls += text.count('ConfigTranslations.text(')
    for match in re.finditer(r'Component\.literal\(', text):
        # This renderer draws already translated glyphs separately to color their outlines.
        if source.name != 'RuleNoticeScreen.java':
            errors.append(f'{relative}:{text[:match.start()].count(chr(10))+1}: direct literal component')
    if 'translatableWithFallback(' in text:
        errors.append(f'{relative}: bypasses the raw-key fallback with a custom fallback string')
    for match in re.finditer(r'(?:Component\.translatable|\btr)\("([^"\\]*)"\s*[,)]', text):
        key = match[1]
        static_calls += 1
        if key not in translations and not key.startswith(('item.minecraft.', 'entity.minecraft.', 'gui.ok', 'gui.cancel', 'gui.done', 'gui.back', 'attribute.modifier.', 'item.modifiers.')):
            missing.setdefault(key, []).append(f'{relative}:{text[:match.start()].count(chr(10))+1}')

report = {
    'bundled_keys': len(translations),
    'files_using_translation_components': component_files,
    'static_component_calls': static_calls,
    'dynamic_data_component_calls': raw_data_calls,
    'missing_static_keys': missing,
    'errors': errors,
    'literal_exception': 'RuleNoticeScreen: already resolved glyphs, preserving per-span color/outline',
    'dynamic_names': 'FormHair/FormAccess, ConfigTranslations.text and the client Language overlay retain keys until client locale resolution',
}
if '--json' in sys.argv:
    print(json.dumps(report, indent=2))
else:
    print(f"{report['bundled_keys']} bundled keys; {component_files} files; {static_calls} static calls; {raw_data_calls} dynamic data calls")
    for error in errors:
        print(error)
    for key, locations in missing.items():
        print('Missing bundled key:', key, locations[0])
sys.exit(bool(errors or missing))
