#!/usr/bin/env python3
"""Audit the built plugin ZIP, without extracting or executing its contents."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile


def audit(archive, root):
    errors, resources, jars, descriptors = [], {}, [], []
    bytecode_count = 0
    offline_archives = []
    with zipfile.ZipFile(archive) as distribution:
        for name in distribution.namelist():
            if not name.endswith(".jar"):
                continue
            with zipfile.ZipFile(io.BytesIO(distribution.read(name))) as jar:
                names = jar.namelist()
                notices = [entry for entry in names if "license" in entry.lower() or "notice" in entry.lower()]
                jars.append({"path": name, "licence_entries": notices})
                if any(token in Path(name).name for token in ("mockito", "byte-buddy", "junit", "assertj")):
                    errors.append("Test tooling bundled in production: " + name)
                for entry in names:
                    if entry.endswith("/"):
                        continue
                    data = jar.read(entry)
                    resources.setdefault(entry, []).append(hashlib.sha256(data).hexdigest())
                    if entry == "studio/offline/studio-offline-tools.zip":
                        offline_archives.append(data)
                    if entry == "META-INF/plugin.xml":
                        descriptors.append(ET.fromstring(data))
                    if entry.startswith("org/ikasan/studio/") and entry.endswith(".class"):
                        bytecode_count += 1
                        if data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") > 61:
                            errors.append("Studio bytecode requires newer than Java 17: " + entry)

    if len(offline_archives) != 1:
        errors.append("Expected one bundled offline tools archive")
    else:
        with zipfile.ZipFile(io.BytesIO(offline_archives[0])) as tools:
            entries = tools.namelist()
            for required in ("bin/studio-cli", "bin/studio-cli.bat", "bin/studio_upgrade.py", "README.md", "CommandLineMigration.md", "LICENSE.txt"):
                if not any(name.endswith("/" + required) for name in entries):
                    errors.append("Missing offline tool: " + required)
            for artifact in ("studio-cli-", "studio-generator-", "studio-pack-v3-", "studio-pack-v4-"):
                if not any("/lib/" + artifact in name and name.endswith(".jar") for name in entries):
                    errors.append("Missing offline runtime artifact: " + artifact)
            script = next((n for n in entries if n.endswith("/bin/studio_upgrade.py")), None)
            if script and tools.read(script) != (root / "tools/migration/studio_upgrade.py").read_bytes():
                errors.append("Bundled project verifier differs from source")
            for name in entries:
                if "/lib/studio-generator-" in name or "/lib/studio-pack-" in name:
                    with zipfile.ZipFile(io.BytesIO(tools.read(name))) as runtime:
                        for entry in runtime.namelist():
                            if entry.endswith("/") or not entry.startswith(("org/ikasan/studio/core/", "studio/metapack/")):
                                continue
                            if hashlib.sha256(runtime.read(entry)).hexdigest() not in resources.get(entry, []):
                                errors.append("Offline engine/pack differs from installed plugin: " + entry)
                if name.endswith(".jar") and any(token in Path(name).name for token in ("junit", "mockito", "assertj", "byte-buddy")):
                    errors.append("Test dependency in offline tools: " + name)

    properties = {}
    for line in (root / "gradle.properties").read_text().splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
    if len(descriptors) != 1:
        errors.append("Expected exactly one plugin.xml, found " + str(len(descriptors)))
    else:
        descriptor = descriptors[0]
        for field, expected in (("id", properties["pluginGroup"]), ("version", properties["pluginVersion"]),
                                ("name", properties["pluginName"])):
            if descriptor.findtext(field) != expected:
                errors.append("Incorrect plugin metadata: " + field)
        idea = descriptor.find("idea-version")
        if idea is None or idea.get("since-build") != properties["pluginSinceBuild"]:
            errors.append("Incorrect minimum IDE build")
        for field in ("description", "change-notes", "vendor"):
            if not descriptor.findtext(field, "").strip():
                errors.append("Missing plugin metadata: " + field)
        if "Initial scaffold" in descriptor.findtext("change-notes", ""):
            errors.append("Change notes still contain template boilerplate")
        # Optional content modules still need their descriptors at plugin load time.
        for module in descriptor.findall("content/module"):
            module_name = module.get("name", "")
            module_descriptor = module_name + ".xml"
            if module_descriptor not in resources:
                errors.append("Missing declared module descriptor: " + module_descriptor)
        dependencies = {item.text for item in descriptor.findall("depends")}
        for required in ("com.intellij.modules.platform", "com.intellij.java", "org.jetbrains.idea.maven",
                         "org.jetbrains.plugins.terminal"):
            if required not in dependencies:
                errors.append("Missing IDE dependency: " + required)

    source = root / "src/main/resources"
    expected = [path for path in source.rglob("*") if path.is_file()
                and path.relative_to(source).as_posix() != "META-INF/plugin.xml"
                and not (path.parent == source / "studio/metapack" and path.suffix == ".md")]
    for path in expected:
        name = path.relative_to(source).as_posix()
        if hashlib.sha256(path.read_bytes()).hexdigest() not in resources.get(name, []):
            errors.append("Missing or changed packaged resource: " + name)
    license_hash = hashlib.sha256((root / "LICENSE.txt").read_bytes()).hexdigest()
    if license_hash not in resources.get("META-INF/LICENSE.txt", []):
        errors.append("Project BSD licence is absent or differs from LICENSE.txt")
    for required in ("org/ikasan/studio/ui/actions/StartTestMailServerAction.class",
                     "org/ikasan/studio/intellij/runtime/TestFtpServerService.class",
                     "org/apache/ftpserver/FtpServerFactory.class",
                     "META-INF/pluginIcon.svg", "META-INF/pluginIcon_dark.svg"):
        if required not in resources:
            errors.append("Missing runtime or harness resource: " + required)
    if not bytecode_count:
        errors.append("No Studio bytecode found")
    packs = sorted(name.split("/")[-2] for name in resources if name.endswith("/metapack.json"))
    if not packs:
        errors.append("No packaged meta-packs")
    return {"archive": str(archive), "sha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
            "status": "FAIL" if errors else "PASS", "errors": errors, "meta_packs": packs,
            "source_resources_checked": len(expected), "studio_classes_checked": bytecode_count,
            "libraries": jars,
            "note": "Licence entries are an inventory, not legal clearance. UI installation and external harness processes require smoke tests."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--zip", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.zip, Path(__file__).resolve().parents[1])
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    print(f"Release archive {report['status']}: {report['source_resources_checked']} resources, "
          f"{report['studio_classes_checked']} Studio classes; report: {args.report}")
    for error in report["errors"]:
        print(error, file=sys.stderr)
    return bool(report["errors"])


if __name__ == "__main__":
    sys.exit(main())
