#!/usr/bin/env python3
"""Build a public VDI kit, warming an isolated curated Maven repository and proving an offline rebuild."""
import argparse
from datetime import datetime, timezone
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import threading
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent
ARTIFACTS = ("studio-generator", "studio-test-kit", "studio-pack-v4")
NS = "{http://maven.apache.org/POM/4.0.0}"
# Pin lifecycle plugins too: Maven releases have different lifecycle defaults.
PLUGINS = {"maven-clean-plugin": "3.4.1", "maven-resources-plugin": "3.3.1",
           "maven-jar-plugin": "3.4.2", "maven-install-plugin": "3.1.4"}


def run(command, cwd=ROOT, log=None):
    print("Running:", " ".join(map(str, command)), flush=True)
    if log is None:
        subprocess.run(list(map(str, command)), cwd=cwd, check=True)
    else:
        with Path(log).open("w", encoding="utf-8") as output:
            subprocess.run(list(map(str, command)), cwd=cwd, stdout=output, stderr=subprocess.STDOUT, check=True)


def copy_public_tree(source, target):
    shutil.copytree(source, target, ignore=shutil.ignore_patterns(
        "target", "build", ".git", ".idea", "__pycache__", "*.pyc"))


def prepare_starter(destination):
    copy_public_tree(ROOT / "ikasan-studio-ancillary/example-metapack", destination)
    for path in destination.rglob("*"):
        if path.is_file() and path.suffix in (".xml", ".json", ".java", ".md", ".ftl"):
            path.write_text(path.read_text(encoding="utf-8").replace("ExampleIkasan4", "SharedIkasan4"), encoding="utf-8")
    original = destination / "example-pack/src/main/resources/studio/metapack/ExampleIkasan4"
    original.rename(original.with_name("SharedIkasan4"))
    ET.register_namespace("", NS[1:-1])
    for pom in destination.rglob("pom.xml"):
        tree = ET.parse(pom)
        project = tree.getroot()
        build = project.find(NS + "build")
        if build is None:
            build = ET.SubElement(project, NS + "build")
        management = ET.SubElement(build, NS + "pluginManagement")
        plugins = ET.SubElement(management, NS + "plugins")
        for artifact, version in PLUGINS.items():
            plugin = ET.SubElement(plugins, NS + "plugin")
            ET.SubElement(plugin, NS + "artifactId").text = artifact
            ET.SubElement(plugin, NS + "version").text = version
        ET.indent(tree, space="  ")
        tree.write(pom, encoding="utf-8", xml_declaration=True)


def seed_studio_artifacts(repository):
    for artifact in ARTIFACTS:
        module = ROOT / "headless" / artifact
        pom = module / "build/publications/library/pom-default.xml"
        version = ET.parse(pom).getroot().findtext(NS + "version")
        target = repository / "org/ikasan/studio" / artifact / version
        target.mkdir(parents=True, exist_ok=True)
        for suffix, source in ((".pom", pom), (".jar", module / "build/libs" / (artifact + "-" + version + ".jar")),
                               ("-sources.jar", module / "build/libs" / (artifact + "-" + version + "-sources.jar"))):
            shutil.copy2(source, target / (artifact + "-" + version + suffix))
        # Mark these as locally installed, independent of any external repository ID.
        (target / "_remote.repositories").write_text("".join(p.name + ">=\n" for p in target.iterdir() if p.suffix in (".jar", ".pom")), encoding="utf-8")


class ArtifactMirror:
    """Serve only requested Maven artifacts. Never copy user settings, credentials or the entire cache."""
    def __init__(self, cache):
        self.cache = cache.resolve()
        self.requests = set()
        mirror = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *_):
                pass

            def do_GET(self):
                relative = urllib.parse.unquote(urllib.parse.urlsplit(self.path).path).lstrip("/")
                try:
                    candidate = (mirror.cache / relative).resolve()
                    if not candidate.is_relative_to(mirror.cache) or candidate.name in ("settings.xml", "settings-security.xml"):
                        self.send_error(404)
                        return
                    if relative.endswith((".sha1", ".md5")):
                        base = candidate.with_suffix("")
                        if base.is_file():
                            algorithm = "sha1" if relative.endswith(".sha1") else "md5"
                            data = hashlib.new(algorithm, base.read_bytes()).hexdigest().encode()
                        else:
                            self.send_error(404)
                            return
                    elif candidate.is_file() and candidate.suffix in (".jar", ".pom", ".xml", ".module"):
                        data = candidate.read_bytes()
                    else:
                        url = "https://repo.maven.apache.org/maven2/" + urllib.parse.quote(relative, safe="/")
                        with urllib.request.urlopen(url, timeout=30) as response:
                            data = response.read()
                    mirror.requests.add(relative)
                    self.send_response(200)
                    self.send_header("Content-Length", str(len(data)))
                    self.end_headers()
                    self.wfile.write(data)
                except (OSError, urllib.error.URLError):
                    self.send_error(404)

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    def __enter__(self):
        self.thread.start()
        return self

    def __exit__(self, *_):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()


def clean_repository(repository):
    for path in repository.rglob("*"):
        if path.is_file() and (path.name.endswith((".lastUpdated", ".lock")) or path.name == "resolver-status.properties"):
            path.unlink()


def inventory(root):
    return {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(root.rglob("*")) if p.is_file() and p != root / "inventory.json"}


def archive_kit(root, archive):
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as output:
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            entry = zipfile.ZipInfo(root.name + "/" + path.relative_to(root).as_posix(), (1980, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o100644 << 16
            output.writestr(entry, path.read_bytes())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven", default=os.environ.get("MAVEN", "mvn"))
    parser.add_argument("--artifact-cache", type=Path, default=Path.home() / ".m2/repository",
                        help="Read-through artifact cache only; user settings are never read or copied.")
    parser.add_argument("--without-plugin", action="store_true", help="Explicitly omit the matching development plugin ZIP.")
    args = parser.parse_args()
    maven = shutil.which(args.maven)
    if maven is None:
        parser.error("Maven is required; pass --maven /path/to/mvn")
    build = ROOT / "build/metapack-authoring-kit"
    build.mkdir(parents=True, exist_ok=True)
    kit = build / "ikasan-metapack-authoring-kit"
    if kit.exists():
        parser.error("Output already exists. Preserve or remove build/metapack-authoring-kit/ikasan-metapack-authoring-kit before rebuilding.")
    kit.mkdir()
    copy_public_tree(SOURCE / "content", kit / "content-temp")
    for path in (kit / "content-temp").iterdir():
        shutil.move(str(path), kit / path.name)
    (kit / "content-temp").rmdir()
    shutil.copy2(SOURCE / "verify.py", kit / "verify.py")
    prepare_starter(kit / "starter")
    shutil.copy2(kit / "docs/STARTER.md", kit / "starter/README.md")
    copy_public_tree(kit / ".github", kit / "starter/.github")
    docs = kit / "reference"
    docs.mkdir()
    for name in ("CustomMetaPackHandover.md", "IndependentMetaPackArtifacts.md", "IkasanRuntimeJson.md", "TestingEngineAndMetaPacks.md"):
        shutil.copy2(ROOT / "docs" / name, docs / name)
    shutil.copy2(ROOT / "ikasan-studio-ancillary/example-metapack/README.md", docs / "ExampleMetaPack.md")
    shutil.copy2(ROOT / "headless/README.md", docs / "HeadlessEngine.md")
    for name in ("METAPACK.md", "METAPACK_COMPLIANCE.md"):
        shutil.copy2(ROOT / "src/main/resources/studio/metapack" / name, docs / name)
    copy_public_tree(ROOT / "src/main/resources/studio/metapack/schema", kit / "schemas")
    for name in ("LICENSE", "LICENSE.md", "LICENSE.txt", "NOTICE"):
        if (ROOT / name).is_file():
            shutil.copy2(ROOT / name, kit / name)
    tasks = [":" + artifact + ":" + task for artifact in ARTIFACTS
             for task in ("jar", "sourcesJar", "generatePomFileForLibraryPublication")]
    run([ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"), "-p", "headless", "--offline", *tasks], log=build / "studio-artifacts.log")
    repository = kit / "dependency-repository"
    seed_studio_artifacts(repository)
    with ArtifactMirror(args.artifact_cache) as mirror:
        settings = build / "bootstrap-settings.xml"
        settings.write_text((kit / "settings.xml").read_text().replace(
            "https://repo.maven.apache.org/maven2", "http://127.0.0.1:" + str(mirror.server.server_port)), encoding="utf-8")
        run([maven, "-B", "-nsu", "-s", settings, "-gs", settings, "-Dmaven.repo.local=" + str(repository),
             "-Djava.awt.headless=true", "clean", "install"], cwd=kit / "starter", log=build / "bootstrap.log")
        requested = sorted(mirror.requests)
    samples = kit / "samples"
    samples.mkdir()
    shutil.copy2(kit / "starter/example-pack/target/example-pack-0.1.0-SNAPSHOT.jar", samples / "shared-ikasan4-example-pack.jar")
    shutil.copy2(kit / "starter/example-runtime/target/example-runtime-0.1.0-SNAPSHOT.jar", samples / "example-runtime-0.1.0-SNAPSHOT.jar")
    shutil.copy2(kit / "starter/example-pack/target/samples/model.json", samples / "model.json")
    if not args.without_plugin:
        run([ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"), "buildPlugin", "verifyReleaseArchive",
             "-PstudioSandboxDirectory=build/authoring-kit-sandbox", "--offline"], log=build / "plugin.log")
        properties = {key.strip(): value.strip() for key, value in (
            line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
            if "=" in line and not line.lstrip().startswith("#"))}
        version = properties["pluginVersion"]
        candidates = list((ROOT / "build/distributions").glob("*-" + version + ".zip"))
        if len(candidates) != 1:
            raise ValueError("Cannot identify the matching plugin archive")
        (kit / "plugin").mkdir()
        shutil.copy2(candidates[0], kit / "plugin" / candidates[0].name)
    # Ship public engine/test-kit source for API context without shipping the entire Studio checkout.
    for artifact in ("studio-generator", "studio-test-kit"):
        source_jar = next((repository / "org/ikasan/studio" / artifact).rglob("*-sources.jar"))
        with zipfile.ZipFile(source_jar) as archive:
            archive.extractall(kit / "public-api-source" / artifact)
    # Discard only outputs within this kit; the Maven install above remains in the kit repository.
    for target in list((kit / "starter").rglob("target")):
        if target.is_dir():
            shutil.rmtree(target)
    clean_repository(repository)
    try:
        revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True, stderr=subprocess.DEVNULL).strip()
        dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip())
    except (OSError, subprocess.CalledProcessError):
        revision, dirty = None, None  # Source ZIPs have no Git metadata; file hashes still identify delivered bytes.
    receipt = {"createdUtc": datetime.now(timezone.utc).isoformat(), "studioCommit": revision,
               "workingTreeHadChanges": dirty, "ikasanVersion": "4.1.6", "packId": "SharedIkasan4",
               "generatorApiVersion": 1, "jdk": 17, "containsPrivateComponents": False,
               "pluginIncluded": not args.without_plugin, "requestedArtifactPaths": requested,
               "validation": "isolated repository bootstrap; offline verification required before packaging"}
    (kit / "build-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    # Avoid IDE/import caches or private additions silently becoming distribution content.
    (kit / "inventory.json").write_text(json.dumps({"files": inventory(kit)}, indent=2) + "\n", encoding="utf-8")
    run([sys.executable, kit / "verify.py", "--maven", maven], log=build / "offline-verification.log")
    # Offline verification generates target output and installs the example. Do not ship build output.
    for target in list((kit / "starter").rglob("target")):
        if target.is_dir():
            shutil.rmtree(target)
    if (kit / "work").exists():
        shutil.rmtree(kit / "work")
    clean_repository(repository)
    receipt["validation"] = "PASS: isolated-repository bootstrap and offline clean install, generated-factory compilation, pack contracts and packaged-JAR installer validation"
    (kit / "build-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    (kit / "inventory.json").write_text(json.dumps({"files": inventory(kit)}, indent=2) + "\n", encoding="utf-8")
    archive = build / "ikasan-metapack-authoring-kit.zip"
    archive_kit(kit, archive)
    (archive.with_suffix(".zip.sha256")).write_text(hashlib.sha256(archive.read_bytes()).hexdigest() + "  " + archive.name + "\n", encoding="utf-8")
    print("Created", archive, flush=True)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print("Kit build failed: " + str(error) + ". See build/metapack-authoring-kit/*.log.", file=sys.stderr)
        sys.exit(1)
