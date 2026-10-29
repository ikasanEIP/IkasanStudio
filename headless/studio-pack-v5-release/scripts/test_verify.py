"""Evidence must not turn missing/skipped runtime tests into a green release gate."""
import json
import zipfile
import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch
import verify


class RuntimeEvidenceTest(unittest.TestCase):
    def test_missing_and_skipped_tests_are_not_success(self):
        self.assertFalse(verify.runtime_reports_pass([]))
        self.assertFalse(verify.runtime_reports_pass([dict(tests=1, failures=0, errors=0, skipped=1)]))
        self.assertFalse(verify.runtime_reports_pass([dict(tests=0, failures=0, errors=0, skipped=0)]))

    def test_successful_control_does_not_hide_failed_integration(self):
        control = dict(tests=3, failures=0, errors=0, skipped=0)
        integration = dict(tests=1, failures=1, errors=0, skipped=0)
        self.assertTrue(verify.runtime_reports_pass([control]))
        self.assertFalse(verify.runtime_reports_pass([control, integration]))

    def test_readable_summary_keeps_failure_visible(self):
        summary = verify.summary_markdown(dict(status="failed", started="today",
            stages=[dict(name="rollback", exitCode=1, log="rollback.log")],
            runtimeTests={"rollback": [dict(tests=4, failures=1, errors=0, skipped=0)]}))
        self.assertIn("FAIL (exit 1)", summary)
        self.assertIn("[log](rollback.log)", summary)
        self.assertIn("| rollback | 4 | 1 | 0 | 0 |", summary)

    def test_collects_each_reactor_module_not_only_parent(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for module, failures in [("jms", 1), ("ftp", 0)]:
                folder = root / module / "target/surefire-reports"
                folder.mkdir(parents=True)
                (folder / "TEST-Transport.xml").write_text(
                    f'<testsuite name="Transport" tests="1" failures="{failures}" errors="0" skipped="0"/>')
            with patch.object(verify, "PACK", root):
                results = verify.test_results(root)
            self.assertEqual(2, len(results))
            self.assertEqual(1, sum(r["failures"] for r in results))
            self.assertFalse(verify.runtime_reports_pass(results))


class DependencyAuditTest(unittest.TestCase):
    def write_project(self, root, version="5.1.0", modules=()):
        root.mkdir(parents=True, exist_ok=True)
        (root / "pom.xml").write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><modules>'
            + "".join("<module>" + name + "</module>" for name in modules) + "</modules></project>")
        (root / "target").mkdir(exist_ok=True)
        graph = dict(groupId="example", artifactId="fixture", version="1", children=[
            dict(groupId="other", artifactId="wrapper", version="3", children=[
                dict(groupId="org.ikasan", artifactId="ikasan-test", version=version, scope="test")])])
        (root / "target/v5-dependencies.json").write_text(json.dumps(graph))

    def test_accepts_coherent_platform_and_unrelated_library_versions(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root)
            audit = verify.dependency_audit(root, "5.1.0")
            self.assertEqual([], audit["errors"])
            self.assertEqual("org.ikasan:ikasan-test", audit["dependencies"][0]["dependency"])

    def test_reports_transitive_test_dependency_from_wrong_platform(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root, modules=("child",))
            self.write_project(root / "child", "4.1.6")
            audit = verify.dependency_audit(root, "5.1.0")
            self.assertEqual(1, len(audit["errors"]))
            self.assertIn("child: org.ikasan:ikasan-test resolved 4.1.6; pack requires 5.1.0", audit["errors"][0])

    def test_missing_child_report_cannot_hide_behind_successful_parent(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root, modules=("child",))
            self.write_project(root / "child")
            (root / "child/target/v5-dependencies.json").unlink()
            audit = verify.dependency_audit(root, "5.1.0")
            self.assertTrue(any("child: missing or invalid" in error for error in audit["errors"]))

    def test_independent_fork_is_pinned_not_ignored(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root)
            tree = root / "target/v5-dependencies.json"
            graph = json.loads(tree.read_text())
            fork = dict(groupId="org.ikasan", artifactId="commons-dbcp2", version="1.0.0")
            graph["children"].append(fork)
            tree.write_text(json.dumps(graph))
            self.assertFalse(verify.dependency_audit(root, "5.1.0")["errors"])
            fork["version"] = "1.1.0"
            tree.write_text(json.dumps(graph))
            audit = verify.dependency_audit(root, "5.1.0")
            self.assertTrue(any("commons-dbcp2 resolved 1.1.0; pack requires 1.0.0" in e for e in audit["errors"]))

    def test_release_bigqueue_is_independently_pinned_not_platform_versioned(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root)
            tree = root / "target/v5-dependencies.json"
            graph = json.loads(tree.read_text())
            fork = dict(groupId="org.ikasan", artifactId="bigqueue", version="1.0.0")
            graph["children"].append(fork)
            tree.write_text(json.dumps(graph))
            self.assertEqual([], verify.dependency_audit(root, "5.1.0")["errors"])
            fork["version"] = "5.1.0"
            tree.write_text(json.dumps(graph))
            self.assertTrue(any("bigqueue resolved 5.1.0; pack requires 1.0.0" in error
                                for error in verify.dependency_audit(root, "5.1.0")["errors"]))

    def test_malformed_and_empty_graphs_fail_closed(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self.write_project(root)
            for content in ("not json", "{}", json.dumps(dict(groupId="example", artifactId="empty", version="1"))):
                with self.subTest(content=content):
                    (root / "target/v5-dependencies.json").write_text(content)
                    self.assertTrue(verify.dependency_audit(root, "5.1.0")["errors"])


class ReactorArchiveTest(unittest.TestCase):
    def test_separation_and_test_leak_detection(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            contents = {"user": ["org/example/transport/transport/TransportConverter.class"],
                        "generated": ["org/ikasan/studio/boot/Application.class"], "user-flow-tests": []}
            for module, entries in contents.items():
                folder = root / module / "target"
                folder.mkdir(parents=True)
                with zipfile.ZipFile(folder / "app.jar", "w") as jar:
                    for entry in entries:
                        jar.writestr(entry, b"fixture")
            self.assertFalse(verify.reactor_archive_errors(root))
            with zipfile.ZipFile(root / "generated/target/app.jar", "a") as jar:
                jar.writestr("org/ikasan/studio/flowtests/support/Helper.class", b"fixture")
                jar.writestr("org/example/transport/transport/TransportConverter.class", b"fixture")
            errors = verify.reactor_archive_errors(root)
            self.assertTrue(any("test classes" in e for e in errors))
            self.assertTrue(any("owned by user" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
