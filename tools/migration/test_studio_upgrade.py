import copy
import json
from pathlib import Path
import tempfile
import time
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import studio_upgrade as tool

class UpgradeTest(unittest.TestCase):
    def test_report_timestamps_preserve_original_verification_times(self):
        before = self.report(); after = copy.deepcopy(before)
        before['verifiedAt'] = '2026-09-26T12:00:00+00:00'
        after['verifiedAt'] = '2026-09-26T13:00:00+00:00'
        after.update(version='V4.1.6', model={'version': 'V4.1.6', 'flows': []})
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            for report in (dict(before, checks=[]), tool.comparison(before, after)):
                tool.save(directory, report)
                markdown = (directory/'report.md').read_text()
                self.assertIn('Report generated at (UTC):', markdown)
                self.assertIn(before['verifiedAt'], markdown)
                self.assertIn('reportGeneratedAt', json.loads((directory/'report.json').read_text()))
            self.assertIn(after['verifiedAt'], markdown)

    def test_jms_failure_explains_import_migration_choice(self):
        diagnostics = tool.build_diagnostics('package javax.jms does not exist', 'V4.1.6')
        self.assertIn('--update-user-imports true', diagnostics[0]['fix'])
        self.assertIn('updateUserImports=false', diagnostics[0]['fix'])
        self.assertEqual([], tool.build_diagnostics('package javax.jms does not exist', 'V3.3.9'))

    def report(self):
        return dict(format=tool.FORMAT, status='PASS', version='V3.3.9', model={'version':'V3.3.9','flows':[]},
                    userSourceHashes={'user/src/Order.java':'abc'}, command=['mvn','-B','clean','verify'],
                    tests=[dict(id='user/testOne',status='PASS')])
    def test_comparison_rejects_lost_tests_and_sources(self):
        before=self.report(); after=copy.deepcopy(before)
        after.update(version='V4.1.6',model={'version':'V4.1.6','flows':[]})
        self.assertEqual('PASS',tool.comparison(before,after)['status'])
        after['tests']=[]
        self.assertEqual('FAIL',tool.comparison(before,after)['status'])
        after['tests']=before['tests'];after['userSourceHashes']={}
        self.assertEqual('FAIL',tool.comparison(before,after)['status'])
    def test_comparison_does_not_hide_description_changes(self):
        self.assertNotEqual(tool.normalized({'description':'javax.jms.Message'}),tool.normalized({'description':'jakarta.jms.Message'}))
    def test_stale_and_skipped_reports_are_not_successful_execution(self):
        with tempfile.TemporaryDirectory() as tmp:
            project=Path(tmp);report=project/'user/target/surefire-reports/TEST-example.xml'
            report.parent.mkdir(parents=True)
            report.write_text('<testsuite><testcase classname="A" name="one"><skipped/></testcase></testsuite>')
            self.assertEqual([],tool.test_reports(project,time.time_ns()+1_000_000_000))
            self.assertEqual('SKIP',tool.test_reports(project,0)[0]['status'])
    def test_verify_records_incomplete_when_maven_runs_no_tests(self):
        with tempfile.TemporaryDirectory() as tmp:
            project=Path(tmp);(project/'user').mkdir();(project/'pom.xml').write_text('<project/>')
            (project/tool.MODEL).parent.mkdir(parents=True);(project/tool.MODEL).write_text('{"version":"V3.3.9"}')
            args=SimpleNamespace(project=project,report='migration-before',maven='mvn',maven_arg=[],timeout=1)
            with patch.object(tool,'execute',return_value=0): self.assertEqual(2,tool.verify(args))
            report=json.loads((project/'migration-before/report.json').read_text())
            self.assertEqual('INCOMPLETE',report['status'])
            self.assertEqual('Maven completed successfully (exit code 0).', report['checks'][0]['detail'])
    def test_verify_reports_failed_maven_even_if_a_test_passed(self):
        with tempfile.TemporaryDirectory() as tmp:
            project=Path(tmp);(project/'user').mkdir();(project/'pom.xml').write_text('<project/>')
            (project/tool.MODEL).parent.mkdir(parents=True);(project/tool.MODEL).write_text('{"version":"V3.3.9"}')
            args=SimpleNamespace(project=project,report='migration-before',maven='mvn',maven_arg=[],timeout=1)
            with patch.object(tool,'execute',return_value=1),patch.object(tool,'test_reports',return_value=[dict(id='A',status='PASS')]):
                self.assertEqual(1,tool.verify(args))
    def test_exact_plan_allows_only_its_expected_model(self):
        import base64
        before=self.report();after=copy.deepcopy(before)
        after.update(version='V4.1.6',model={'version':'V4.1.6','flows':[], 'newDefault':True})
        plan={'changes':[{'path':tool.MODEL.as_posix(),
              'before':base64.b64encode(json.dumps(before['model']).encode()).decode(),
              'after':base64.b64encode(json.dumps(after['model']).encode()).decode()}]}
        self.assertEqual('FAIL',tool.comparison(before,after)['status'])
        self.assertEqual('PASS',tool.comparison(before,after,plan)['status'])
        after['model']['flows']=[{'name':'unexpected'}]
        self.assertEqual('FAIL',tool.comparison(before,after,plan)['status'])
    def test_report_cannot_be_deleted_by_maven_clean(self):
        with tempfile.TemporaryDirectory() as tmp:
            project=Path(tmp);(project/'user').mkdir();(project/'pom.xml').write_text('<project/>')
            (project/tool.MODEL).parent.mkdir(parents=True);(project/tool.MODEL).write_text('{"version":"V3.3.9"}')
            args=SimpleNamespace(project=project,report='target/migration-before',maven='mvn',maven_arg=[],timeout=1)
            with self.assertRaises(ValueError): tool.verify(args)
    def test_additional_module_tests_are_protected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);f=root/'integration/src/test/java/Acceptance.java';f.parent.mkdir(parents=True);f.write_text('test')
            old=tool.sources(root);f.write_text('changed');self.assertNotEqual(old,tool.sources(root))

    def test_jdk_guidance_uses_recorded_maven_not_current_environment(self):
        findings = tool.build_diagnostics('Java version: 11.0.25, vendor: Example', 'V4.1.6')
        self.assertIn('JDK 17', findings[0]['problem'])
        self.assertIn('JAVA_HOME', findings[0]['fix'])
        self.assertEqual([], tool.build_diagnostics('Java version: 17.0.12, vendor: Example', 'V4.1.6'))
        self.assertEqual([], tool.build_diagnostics('', 'V4.1.6'))

    def test_legacy_build_failure_is_explained_without_inventing_jdk(self):
        before = self.report(); after = copy.deepcopy(before)
        after.update(version='V4.1.6', status='FAIL', tests=[], checks=[tool.check('Maven build and verification', False, 'exit 1')])
        after['diagnostics'] = tool.build_diagnostics('package javax.xml.bind.annotation does not exist', 'V4.1.6')
        result = tool.comparison(before, after)
        self.assertIn('not recorded', result['javaEnvironment']['After']['actual'])
        self.assertIn('Maven build', result['checks'][1]['detail'])
        with tempfile.TemporaryDirectory() as tmp:
            tool.save(Path(tmp), result)
            text = (Path(tmp)/'report.md').read_text()
            self.assertIn('jakarta.xml.bind.annotation', text)
            self.assertIn('compilation may have prevented tests', text)
            for c in result['checks']:
                if c['status'] != 'PASS': self.assertIn('### '+c['name'], text)

    def test_maven_version_banner_does_not_change_test_command_comparison(self):
        before = self.report(); after = copy.deepcopy(before)
        after.update(version='V4.1.6', model={'version':'V4.1.6','flows':[]})
        after['command'].insert(2, '-V')
        self.assertEqual('PASS', tool.comparison(before, after)['status'])

    def test_comparison_accepts_only_exact_reviewed_import_bytes(self):
        import base64, hashlib
        before = self.report(); after = copy.deepcopy(before)
        path = 'user/src/main/java/Converter.java'
        old = b'import javax.jms.Message;'; new = b'import jakarta.jms.Message;'
        before['userSourceHashes'] = {path: hashlib.sha256(old).hexdigest()}
        after.update(version='V4.1.6', model={'version':'V4.1.6','flows':[]}, userSourceHashes={path:hashlib.sha256(new).hexdigest()})
        plan = {'changes':[
            {'path':tool.MODEL.as_posix(), 'before':base64.b64encode(json.dumps(before['model']).encode()).decode(),
             'after':base64.b64encode(json.dumps(after['model']).encode()).decode()},
            {'path':path,'before':base64.b64encode(old).decode(),'after':base64.b64encode(new).decode()}]}
        self.assertEqual('PASS', tool.comparison(before, after, plan)['status'])
        self.assertEqual('FAIL', tool.comparison(before, after)['status'])
        after['userSourceHashes'][path] = hashlib.sha256(new+b' // unrelated edit').hexdigest()
        self.assertEqual('FAIL', tool.comparison(before, after, plan)['status'])

if __name__=='__main__':unittest.main()
