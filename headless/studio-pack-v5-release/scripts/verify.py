#!/usr/bin/env python3
"""Explicit Ikasan 5.1.0 release compatibility gate. Does not publish or modify developer projects."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zipfile

PACK = Path(__file__).resolve().parents[1]
ROOT = PACK.parents[1]
EXPORTS = ["generateCompatibilityFixture", "generateComponentCoverageFixture",
           "generateRuntimeTransportFixtures", "generateProcessingFixtures",
           "generateInputFixtures", "generateRollbackFixtures", "generateReactorFixture",
           "generateMigrationFixtures"]
SUITES = [
    ("compatibility-fixture", ["-Dtest=ExampleFlowFlowTest,BlueConsoleTest", "test"]),
    ("component-coverage-fixture", ["test-compile"]),
    ("runtime-transports", ["test"]),
    ("processing-fixtures", ["test"]),
    ("input-fixtures", ["test"]),
    ("reactor-fixture", ["verify"]),
    ("migration-fixtures", ["test"]),
    ("rollback-fixtures", ["test"]),
]


# Independently versioned libraries, NOT dependency overrides. Verified against the published
# org.ikasan:ikasan-build:5.1.0 parent (release commit 565c8f854e5b5ca0e690c78a0ba6112b89af8ff7).
# Reconcile this narrow policy with each formal platform; never ignore an entire group/prefix.
INDEPENDENT_ARTIFACTS = {
    "org.ikasan:commons-dbcp2": {
        "version": "1.0.0",
        "reason": "Ikasan's Apache Commons DBCP fork has an independent version. "
                  "The release parent declares version.ikasan.commons-dbcp2=1.0.0."
    },
    "org.ikasan:bigqueue": {
        "version": "1.0.0",
        "reason": "The 5.1.0 release parent independently pins BigQueue with version.big.queue=1.0.0."
    }
}


def test_results(directory):
    """Include only reports produced by this invocation (fixtures are freshly exported)."""
    results = []
    for file in sorted(directory.rglob("target/surefire-reports/TEST-*.xml")):
        suite = ET.parse(file).getroot()
        results.append({"file": str(file.relative_to(PACK)), "name": suite.get("name"),
                        **{key: int(suite.get(key, "0"))
                           for key in ("tests", "failures", "errors", "skipped")}})
    return results


def runtime_reports_pass(results):
    # A green Maven exit with skipped/missing tests is not runtime evidence.
    return bool(results) and sum(r["tests"] for r in results) > 0 and all(
        r["failures"] == 0 and r["errors"] == 0 and r["skipped"] == 0 for r in results)


def dependency_audit(directory, expected_version):
    """Audit resolved graphs for every declared reactor module, including test dependencies."""
    inventory, errors, trees = [], [], []
    pending, visited = [directory.resolve()], set()
    while pending:
        module = pending.pop()
        if module in visited:
            continue
        visited.add(module)
        if not module.is_relative_to(directory.resolve()):
            errors.append("Reactor module is outside the fixture: " + str(module))
            continue
        label = str(module.relative_to(directory.resolve()))
        try:
            pom = ET.parse(module / "pom.xml").getroot()
            pending.extend((module / child.text.strip()).resolve()
                           for child in pom.findall("{*}modules/{*}module"))
            tree = module / "target/v5-dependencies.json"
            graph = json.loads(tree.read_text())
            trees.append(str(tree.relative_to(directory.resolve())))
            nodes = [graph]
            while nodes:
                node = nodes.pop()
                if not isinstance(node, dict) or any(not isinstance(node.get(key), str) or not node[key]
                                                    for key in ("groupId", "artifactId", "version")):
                    raise ValueError("Malformed dependency node")
                children = node.get("children", [])
                if not isinstance(children, list):
                    raise ValueError("Malformed dependency children")
                nodes.extend(children)
                if node["groupId"] == "org.ikasan":
                    coordinate = node["groupId"] + ":" + node["artifactId"]
                    independent = INDEPENDENT_ARTIFACTS.get(coordinate)
                    required = independent["version"] if independent else expected_version
                    inventory.append(dict(module=label, dependency=coordinate, version=node["version"],
                                          requiredVersion=required, independentVersion=bool(independent)))
                    if node["version"] != required:
                        errors.append(label + ": " + coordinate + " resolved " + node["version"]
                                      + "; pack requires " + required)
        except (OSError, ValueError, ET.ParseError, AttributeError) as failure:
            errors.append(label + ": missing or invalid dependency evidence: " + str(failure))
    if not inventory:
        errors.append("No resolved org.ikasan dependencies found; cannot verify the pack platform")
    return dict(expectedVersion=expected_version, independentArtifacts=INDEPENDENT_ARTIFACTS, trees=sorted(trees),
                dependencies=sorted(inventory, key=lambda item: (item["module"], item["dependency"], item["version"])),
                errors=errors)


def reactor_archive_errors(directory):
    errors = []
    expected = {"user": "org/example/transport/transport/TransportConverter.class",
                "generated": "org/ikasan/studio/boot/Application.class", "user-flow-tests": None}
    for module, required in expected.items():
        jars = list((directory / module / "target").glob("*.jar"))
        if len(jars) != 1:
            errors.append(module + ": expected exactly one built JAR")
            continue
        try:
            with zipfile.ZipFile(jars[0]) as archive:
                names = set(archive.namelist())
                if required and required not in names:
                    errors.append(module + ": missing " + required)
                if any(name.startswith("org/ikasan/studio/flowtests/") for name in names):
                    errors.append(module + ": test classes must not be packaged as production code")
                for other, owned in expected.items():
                    if other != module and owned and owned in names:
                        errors.append(module + ": contains code owned by " + other)
        except (OSError, zipfile.BadZipFile) as failure:
            errors.append(module + ": " + str(failure))
    return errors


def summary_markdown(evidence):
    lines = ["# Ikasan 5 development verification", "", "Status: **" + evidence["status"] + "**", "",
             "Started: " + evidence["started"], "",
             "| Stage | Result | Evidence |", "| --- | --- | --- |"]
    if "error" in evidence:
        lines[4:4] = ["Error: " + evidence["error"], ""]
    for stage in evidence["stages"]:
        result = "PASS" if stage["exitCode"] == 0 else "FAIL (exit " + str(stage["exitCode"]) + ")"
        log = "[log](" + stage["log"] + ")" if "log" in stage else stage.get("reason", "")
        lines.append("| " + stage["name"] + " | " + result + " | " + log + " |")
    lines += ["", "| Fixture suite | Tests | Failures | Errors | Skipped |", "| --- | ---: | ---: | ---: | ---: |"]
    for suite, reports in evidence["runtimeTests"].items():
        totals = [sum(report[key] for report in reports) for key in ("tests", "failures", "errors", "skipped")]
        lines.append("| " + suite + " | " + " | ".join(map(str, totals)) + " |")
    lines += ["", "The catalogue fixture is compilation-only. A zero test count there is intentional.", "",
              "This is release-pack verification evidence, not Marketplace or migration approval.",
              "See [summary.json](summary.json) for commands, environment, archive hashes and source state.", ""]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", required=True, type=Path, help="Java 25 runtime JDK; Gradle retains its own JDK")
    parser.add_argument("--maven", default="mvn", help="Maven executable or absolute path")
    parser.add_argument("--timeout", type=int, default=900, help="Timeout in seconds per command (default 900)")
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    java = args.java_home.resolve() / "bin/java"
    maven = shutil.which(args.maven)
    if not java.is_file() or not maven:
        parser.error("Provide an existing JDK and Maven executable")
    version = subprocess.run([str(java), "-version"], capture_output=True, text=True, check=True, timeout=30)
    java_version = version.stdout + version.stderr
    metadata = json.loads(next((PACK / "src/main/resources/studio/metapack").glob("*/metapack.json")).read_text())
    if not re.search(r'version "' + re.escape(metadata["javaVersion"]) + r'(?:[.\-"])', java_version):
        parser.error("This pack requires Java " + metadata["javaVersion"] + "; received: " + java_version.strip())
    # Never put results inside an export directory: those are deleted by Gradle.
    report = PACK / "build/verification" / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    report.mkdir(parents=True)
    evidence = {"started": datetime.now(timezone.utc).isoformat(), "pack": metadata,
                "java": java_version.strip(), "stages": [], "runtimeTests": {}, "status": "running"}
    env = dict(os.environ, JAVA_HOME=str(args.java_home.resolve()))
    env["PATH"] = str(java.parent) + os.pathsep + env.get("PATH", "")

    def save():
        (report / "summary.json").write_text(json.dumps(evidence, indent=2) + "\n")
        (report / "summary.md").write_text(summary_markdown(evidence))

    def run(name, command, environment=None):
        print(name + " ...", flush=True)
        stage = {"name": name, "command": command, "log": name + ".log"}
        start = time.monotonic()
        with (report / stage["log"]).open("w") as log:
            # Own a process group so a timeout terminates Maven/Gradle children as well.
            process = subprocess.Popen(command, cwd=ROOT, env=environment, stdout=log,
                                       stderr=subprocess.STDOUT, start_new_session=True)
            try:
                stage["exitCode"] = process.wait(timeout=args.timeout)
            except (subprocess.TimeoutExpired, KeyboardInterrupt) as failure:
                import signal
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
                stage["exitCode"] = 124 if isinstance(failure, subprocess.TimeoutExpired) else 130
        stage["seconds"] = round(time.monotonic() - start, 2)
        evidence["stages"].append(stage)
        save()
        print("  " + ("PASS" if stage["exitCode"] == 0 else "FAIL") + " — " + str(report / stage["log"]), flush=True)
        if stage["exitCode"] == 130:
            raise KeyboardInterrupt
        return stage["exitCode"] == 0

    save()
    try:
        run("maven-version", [maven, "--version"], env)
        for label, command in [("commit", ["git", "rev-parse", "HEAD"]),
                               ("workingTree", ["git", "status", "--short"])]:
            evidence[label] = subprocess.check_output(command, cwd=ROOT, text=True).strip()
        command = [str(ROOT / "gradlew"), "-p", "headless", "--console=plain", "--no-daemon", "--rerun-tasks",
                   ":studio-pack-v5-release:test", ":studio-pack-v5-release:jar"]
        command += [":studio-pack-v5-release:" + task for task in EXPORTS]
        if not run("pack-and-exports", command):
            evidence["status"] = "failed: fixture export; Maven suites not run"
            save()
            return 1
        pack_reports = PACK / "build/test-results/test"
        for file in pack_reports.glob("TEST-*.xml"):
            destination = report / "reports/pack" / file.name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(file, destination)
        evidence["archives"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                                for p in sorted((PACK / "build/libs").glob("*.jar"))}
        for suite, goals in SUITES:
            directory = PACK / "build" / suite
            base = [maven, "-B", "-nsu", "-fae", "-f", str(directory / "pom.xml")]
            # Use clean even after export, so a successful result cannot be from stale classes.
            run(suite, base + ["clean"] + goals, env)
            if suite == "reactor-fixture":
                ownership_errors = reactor_archive_errors(directory)
                evidence["stages"].append({"name": "reactor-archive-ownership",
                    "exitCode": 1 if ownership_errors else 0,
                    "reason": "; ".join(ownership_errors) if ownership_errors else "User/generated code and test support remain separated"})
            evidence["runtimeTests"][suite] = test_results(directory)
            if any(goal in ("test", "package", "verify") for goal in goals) and not runtime_reports_pass(evidence["runtimeTests"][suite]):
                evidence["stages"].append({"name": suite + "-test-evidence", "exitCode": 1,
                    "reason": "Runtime reports missing, skipped or failing; inspect preserved reports"})
            for file in directory.rglob("target/surefire-reports/*"):
                if file.is_file() and file.suffix in (".xml", ".txt"):
                    destination = report / "reports" / suite / file.relative_to(directory)
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(file, destination)
            # Explicit plugin version makes the evidence collector itself repeatable.
            run(suite + "-dependencies", base + ["org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree",
                "-DoutputType=json", "-DoutputFile=target/v5-dependencies.json"], env)
            audit = dependency_audit(directory, metadata["ikasanVersion"])
            evidence.setdefault("dependencyAudits", {})[suite] = audit
            audit_log = suite + "-dependency-audit.json"
            (report / audit_log).write_text(json.dumps(audit, indent=2) + "\n")
            for tree in audit["trees"]:
                destination = report / "dependencies" / suite / tree
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(directory / tree, destination)
            evidence["stages"].append({"name": suite + "-dependency-version-audit",
                "exitCode": 1 if audit["errors"] else 0, "log": audit_log})
            print("  Dependency versions: " + ("FAIL" if audit["errors"] else "PASS"), flush=True)
            save()
        evidence["status"] = "passed" if all(s["exitCode"] == 0 for s in evidence["stages"]) else "failed"
    except KeyboardInterrupt:
        evidence["status"] = "interrupted"
    except Exception as failure:
        evidence["status"] = "failed"
        evidence["error"] = str(failure)
    finally:
        evidence["finished"] = datetime.now(timezone.utc).isoformat()
        save()
        print("Evidence: " + str(report / "summary.json"), flush=True)
    return 0 if evidence["status"] == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())
