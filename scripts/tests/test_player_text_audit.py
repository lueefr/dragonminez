import json
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT=Path(__file__).resolve().parents[1]/'audit-player-text.py'

class PlayerTextAuditTest(unittest.TestCase):
    def audit(self, source, name='client/Fixture.java'):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            locale=root/'src/main/resources/assets/dragonminez/lang/en_us.json'
            locale.parent.mkdir(parents=True)
            locale.write_text(json.dumps({'example.message':'Message: %s'}))
            java=root/'src/main/java/com/dragonminez'/name
            java.parent.mkdir(parents=True,exist_ok=True);java.write_text(source)
            result=subprocess.run(['python3',str(SCRIPT),'--root',directory,'--json'],text=True,capture_output=True)
            return result.returncode,json.loads(result.stdout)
    def test_rejects_appended_strings_and_embedded_arguments(self):
        code,report=self.audit('Component.translatable("example.message", "embedded words").append(" hidden");')
        self.assertNotEqual(code,0)
        self.assertTrue(any('appended' in error for error in report['errors']))
        self.assertTrue(any('argument' in error for error in report['errors']))
    def test_tracks_local_labels_and_format_units_to_rendering(self):
        code,report=self.audit('String label = "Hidden label"; graphics.drawString(font,label,0,0,0); String time=String.format("%.1fs",seconds);')
        self.assertNotEqual(code,0)
        self.assertTrue(any('reaches renderer' in error for error in report['errors']))
        self.assertTrue(any('formatted label/unit' in error for error in report['errors']))
    def test_accepts_translated_components_and_numeric_data(self):
        code,report=self.audit('String value=String.format("%.1f",amount); Component label=Component.translatable("example.message",value); graphics.drawString(font,label,0,0,0);')
        self.assertEqual(code,0,report)
    def test_report_exception_does_not_hide_quest_ui_strings(self):
        code,report=self.audit('private static void other(){ Component.empty().append("UI text"); }','common/quest/QuestRegistry.java')
        self.assertNotEqual(code,0)
        self.assertTrue(report['errors'])

if __name__=='__main__':unittest.main()
