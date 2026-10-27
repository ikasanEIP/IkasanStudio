import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import urllib.request
import zipfile

HERE = Path(__file__).resolve().parent


def module(name):
    spec = importlib.util.spec_from_file_location(name, HERE / (name + ".py"))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


builder = module("build_kit")
verifier = module("verify")


class KitTests(unittest.TestCase):
    def test_tool_preflight_checks_maven_and_java_independently(self):
        verifier.validate_tools("Apache Maven 3.8.6\nJava version: 17.0.12, vendor: Example")
        verifier.validate_tools("Apache Maven 3.9.9\nJava version: 17.0.1, vendor: Example")
        for version in ("Apache Maven 3.6.3\nJava version: 17.0.12, vendor: Example",
                        "Apache Maven 4.0.0\nJava version: 17.0.12, vendor: Example",
                        "Apache Maven 3.8.6\nJava version: 21.0.1, vendor: Example"):
            with self.assertRaises(ValueError):
                verifier.validate_tools(version)

    def test_inventory_detects_tampering_and_missing_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "public.txt"
            path.write_text("public example")
            inventory = {"files": {"public.txt": hashlib.sha256(path.read_bytes()).hexdigest()}}
            (root / "inventory.json").write_text(json.dumps(inventory))
            verifier.verify_inventory(root)
            path.write_text("changed")
            with self.assertRaisesRegex(ValueError, "Missing or changed"):
                verifier.verify_inventory(root)
            path.unlink()
            with self.assertRaisesRegex(ValueError, "Missing or changed"):
                verifier.verify_inventory(root)

    def test_inventory_rejects_escaping_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for path in ("../outside", str(root.parent / "absolute")):
                with self.assertRaisesRegex(ValueError, "escapes"):
                    verifier.checked_path(root, path)

    def test_mirror_serves_only_requested_artifacts_and_blocks_settings_and_traversal(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "selected.pom").write_bytes(b"<project/>")
            (root / "unused-private.jar").write_bytes(b"not selected")
            (root / "settings.xml").write_text("synthetic forbidden settings")
            with builder.ArtifactMirror(root) as mirror:
                base = "http://127.0.0.1:" + str(mirror.server.server_port)
                with urllib.request.urlopen(base + "/selected.pom") as response:
                    self.assertEqual(b"<project/>", response.read())
                for path in ("settings.xml", "%2e%2e/outside.pom"):
                    with self.assertRaises(urllib.error.HTTPError):
                        urllib.request.urlopen(base + "/" + path)
                self.assertEqual({"selected.pom"}, mirror.requests)

    def test_public_copy_excludes_build_ide_and_python_cache(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source"
            source.mkdir()
            (source / "pom.xml").write_text("public")
            for name in ("target", ".idea", "__pycache__"):
                (source / name).mkdir()
                (source / name / "excluded").write_text("excluded")
            builder.copy_public_tree(source, root / "result")
            self.assertEqual(["pom.xml"], [p.name for p in (root / "result").iterdir()])

    def test_archive_is_relocatable_and_checksum_inventory_survives_extraction(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            kit = root / "kit"
            kit.mkdir()
            (kit / "public.txt").write_text("example")
            (kit / "inventory.json").write_text(json.dumps({"files": builder.inventory(kit)}))
            archive = root / "kit.zip"
            builder.archive_kit(kit, archive)
            with zipfile.ZipFile(archive) as source:
                source.extractall(root / "path with spaces")
            verifier.verify_inventory(root / "path with spaces/kit")

    def test_starter_keeps_freemarker_literal_and_pins_lifecycle_plugins(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            example = root / "ikasan-studio-ancillary/example-metapack"
            resource = example / "example-pack/src/main/resources/studio/metapack/ExampleIkasan4"
            resource.mkdir(parents=True)
            (example / "pom.xml").write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion></project>')
            template = '<#if value??>${value}</#if>\n'
            (resource / "component.ftl").write_text(template)
            with patch.object(builder, "ROOT", root):
                builder.prepare_starter(root / "starter")
            self.assertEqual(template, (root / "starter/example-pack/src/main/resources/studio/metapack/SharedIkasan4/component.ftl").read_text())
            xml = (root / "starter/pom.xml").read_text()
            for plugin, version in builder.PLUGINS.items():
                self.assertIn(plugin, xml)
                self.assertIn(version, xml)


if __name__ == "__main__":
    unittest.main()
