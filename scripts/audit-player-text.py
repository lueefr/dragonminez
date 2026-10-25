#!/usr/bin/env python3
"""Audit mod-owned component entry points and the bundled source-language catalog."""
from pathlib import Path
import json
import re
import sys
from java_text_calls import arguments, STRING, FORMAT, embedded_labels

import argparse
parser=argparse.ArgumentParser()
parser.add_argument('--json',action='store_true')
parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
options=parser.parse_args()
root=options.root
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
    # Check complete calls, not just one spelling on one line.
    for call in re.finditer(r'(Component\.translatable|ConfigTranslations\.text|\btr|\btxt|\.append|drawString|drawCenteredString|drawStringWithBorder|drawCenteredStringWithBorder|dmzText)\(', text):
        args,end=arguments(text,call.end())
        name=call[1];line=text[:call.start()].count(chr(10))+1
        if name in ('Component.translatable','ConfigTranslations.text','tr','txt'):
            for index,(_,_,arg) in enumerate(args):
                if not STRING.fullmatch(arg):continue
                value=json.loads(arg)
                if not value or not re.search('[a-zA-Z]',value):continue
                if index==0:
                    if value not in translations and name in ('ConfigTranslations.text','txt'):
                        errors.append(f'{relative}:{line}: uncatalogued text/key: {value}')
                else:
                    errors.append(f'{relative}:{line}: embedded message argument: {value}')
        elif name=='.append' and args and args[0][2].startswith('"'):
            # The quest update report is a local diagnostic file, not player UI.
            report_start=text.rfind('private static void emitUpgradeReport',0,call.start())
            diagnostic=source.name=='QuestRegistry.java' and report_start>=0 and report_start==text.rfind('private static void ',0,call.start())
            if not diagnostic:errors.append(f'{relative}:{line}: embedded appended text')
        elif name!='.append':
            for _,_,arg in args:
                if STRING.fullmatch(arg) and json.loads(arg):errors.append(f'{relative}:{line}: direct rendered string')
            # Text arguments follow graphics/font arguments in the native helpers.
            index=1 if name in ('drawString','drawCenteredString','dmzText') else 2
            if index<len(args):
                for value in embedded_labels(args[index][2],text[:call.start()]):
                    errors.append(f'{relative}:{line}: embedded label reaches renderer: {value}')
    if 'client' in source.parts and source.name!='DebugFileWriter.java':
        for call in re.finditer(r'String\.format\(',text):
            args,end=arguments(text,call.end())
            index=1 if args[0][2].startswith('Locale.') else 0
            if index<len(args) and STRING.fullmatch(args[index][2]):
                value=json.loads(args[index][2])
                if re.search('[a-zA-Z]',FORMAT.sub('',value)):
                    errors.append(f'{relative}:{text[:call.start()].count(chr(10))+1}: embedded formatted label/unit: {value}')
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
    'runtime_missing_keys': 'ConfigTranslations.missingSnapshot records exhausted dynamic lookups in the client Language hook',
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
