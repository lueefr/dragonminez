import subprocess
import tempfile
import unittest
import json
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from unittest.mock import patch
from scripts.release.github_release import artifact_change, next_tag, notes, predecessors, version_key


class GitHubReleaseTest(unittest.TestCase):
    def test_numeric_order_and_reserved_tags(self):
        tags = {'v2.2', 'v2.2-hotfix', 'v2.2-hotfix-9', 'v2.2-hotfix-14', 'v2.2.1', 'v2.2.1-hotfix-4'}
        self.assertGreater(version_key('v2.2-hotfix-10'), version_key('v2.2-hotfix-9'))
        self.assertGreater(version_key('v2.2.1'), version_key('v2.2-hotfix-14'))
        self.assertEqual(next_tag('2.2.1', tags), 'v2.2.1-hotfix-5')
        self.assertEqual(next_tag('2.2.1', tags, 'global'), 'v2.2.1-hotfix-15')
        self.assertEqual(next_tag('2.2.2', tags), 'v2.2.2')
        self.assertIsNone(version_key('v2.2.1-rc1'))
        with self.assertRaises(ValueError): next_tag('2.2.1-rc1', tags)

    def test_only_mod_changes_create_releases(self):
        self.assertFalse(artifact_change(['docs/wiki/Home.md', 'CHANGELOG.md', '.github/workflows/gradle.yml', 'scripts/release/github_release.py']))
        self.assertTrue(artifact_change(['src/main/java/example/Test.java']))
        self.assertTrue(artifact_change(['src/generated/resources/a.json']))
        self.assertTrue(artifact_change(['build.gradle.kts']))

    def test_exact_notes_never_fall_back_to_global_changelog(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '2.2.1.md').write_text('Wrong base release')
            with patch('scripts.release.github_release.run', return_value=''):
                with self.assertRaisesRegex(ValueError, 'No mod changes'):
                    notes('v2.2.1-hotfix-2', 'b' * 40, 'v2.2.1-hotfix-1', 'owner/repo', root)
            (root / '2.2.1-hotfix-2.md').write_text('Correct captured power notes')
            body = notes('v2.2.1-hotfix-2', 'b' * 40, 'v2.2.1-hotfix-1', 'owner/repo', root)
            self.assertIn('Correct captured power notes', body)
            self.assertNotIn('Wrong base release', body)
            self.assertIn('source-commit: ' + 'b' * 40, body)
            self.assertIn('v2.2.1-hotfix-1...v2.2.1-hotfix-2', body)

    def test_generated_notes_use_first_parent_artifact_commits(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args): return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()
            git('init', '-q'); git('config', 'user.name', 'Test'); git('config', 'user.email', 'test@example.invalid')
            (root / 'gradle.properties').write_text('mod_version=2.2.1')
            git('add', '.'); git('commit', '-qm', 'Initial'); git('tag', 'v2.2.1')
            (root / 'docs').mkdir(); (root / 'docs' / 'Guide.md').write_text('Guide')
            git('add', '.'); git('commit', '-qm', 'Only documentation')
            (root / 'src' / 'main').mkdir(parents=True); (root / 'src' / 'main' / 'feature.txt').write_text('feature')
            git('add', '.'); git('commit', '-qm', 'Fix [combat] result'); source = git('rev-parse', 'HEAD')
            import os
            previous = os.getcwd()
            try:
                os.chdir(root)
                body = notes('v2.2.1-hotfix-1', source, 'v2.2.1', 'owner/repo', root / 'notes')
                self.assertIn(r'Fix \[combat\] result', body)
                self.assertNotIn('Only documentation', body)
                self.assertNotIn('- Initial', body)
            finally: os.chdir(previous)

    def test_obsolete_build_cannot_be_promoted(self):
        from scripts.release.github_release import main
        releases = [[{'tag_name': 'v2.2.1-hotfix-2'}]]
        def command(*args):
            if args[:2] == ('gh', 'api'): return json.dumps(releases)
            if args[:3] == ('git', 'tag', '--list'): return 'v2.2.1\nv2.2.1-hotfix-2'
            raise AssertionError(f"Unexpected mutation or command: {args}")
        output = StringIO()
        with patch('sys.argv', ['release', 'publish', '--repository', 'owner/repo', '--source', 'old', '--dry-run']), patch('scripts.release.github_release.run', side_effect=command), patch('scripts.release.github_release.resolve', side_effect=lambda ref: 'new' if ref == 'v2.2.1-hotfix-2' else 'old'), patch('scripts.release.github_release.ancestor', side_effect=lambda a,b: (a,b) in {('old','origin/main'),('old','new')}), patch('scripts.release.github_release.read_gradle_properties', return_value={'mod_version':'2.2.1'}), redirect_stdout(output):
            main()
        self.assertIn('Skipping obsolete source', output.getvalue())

    def test_predecessor_ignores_foreign_and_mutable_branch_targets(self):
        releases = [{'tag_name': 'v2.2.1', 'target_commitish': 'main'}, {'tag_name': 'v2.2.1-hotfix-2'}, {'tag_name': 'v2.2.1-hotfix-1', 'draft': True}]
        with patch('scripts.release.github_release.resolve', side_effect=lambda tag: {'v2.2.1': 'base', 'v2.2.1-hotfix-2': 'foreign'}.get(tag)), patch('scripts.release.github_release.ancestor', side_effect=lambda a, b: a == 'base'):
            self.assertEqual(predecessors(releases, 'source'), [((2, 2, 1, 0), 'v2.2.1', 'base')])


if __name__ == '__main__': unittest.main()
