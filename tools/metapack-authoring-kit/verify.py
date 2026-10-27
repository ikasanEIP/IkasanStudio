#!/usr/bin/env python3
"""Verify the public kit inventory, then build its starter using only its bundled Maven repository."""
import argparse
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import sys


def checked_path(root, relative):
    path = (root / relative).resolve()
    if Path(relative).is_absolute() or not path.is_relative_to(root.resolve()):
        raise ValueError("Inventory path escapes the kit: " + relative)
    return path


def verify_inventory(root):
    inventory = json.loads((root / "inventory.json").read_text(encoding="utf-8"))
    for relative, digest in inventory["files"].items():
        path = checked_path(root, relative)
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            raise ValueError("Missing or changed kit file: " + relative)
    return inventory


def validate_tools(version):
    plain = re.sub(r"\x1b\[[0-9;]*m", "", version)
    maven = re.search(r"Apache Maven (\d+)\.(\d+)\.(\d+)", plain)
    java = re.search(r"Java version:\s*(\d+)[.\-\s,]", plain)
    if maven is None or tuple(map(int, maven.groups())) < (3, 8, 6) or maven.group(1) != "3":
        raise ValueError("Use Maven 3.8.6 or a newer Maven 3.x; Maven 4 is not verified by this kit.")
    if java is None or java.group(1) != "17":
        raise ValueError("Use a JDK 17 installation for this verified starter (set JAVA_HOME and PATH).")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven", default=os.environ.get("MAVEN", "mvn"))
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--inventory-only", action="store_true")
    mode.add_argument("--build-only", action="store_true",
                        help="After intentional starter edits, skip the original public-file checksums; still build offline.")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    if not args.build_only:
        verify_inventory(root)
        print("Public kit inventory verified.", flush=True)
    if args.inventory_only:
        return
    maven = shutil.which(args.maven)
    if maven is None:
        raise ValueError("Maven not found. Install approved Maven 3.8.6 or newer Maven 3.x and put mvn on PATH, or pass --maven.")
    version = subprocess.run([maven, "-version"], check=True, text=True, stdout=subprocess.PIPE).stdout
    print(version, flush=True)
    validate_tools(version)
    repository = root / "work/maven-repository"
    if not repository.exists():
        repository.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(root / "dependency-repository", repository)
    settings = str(root / "settings.xml")
    command = [maven, "-B", "-o", "-nsu", "-s", settings, "-gs", settings,
               "-Dmaven.repo.local=" + str(repository),
               "-Djava.awt.headless=true", "clean", "install"]
    subprocess.run(command, cwd=root / "starter", check=True)
    print("Offline pack validation, generated-factory compilation, packaged-JAR validation and local install passed.")
    print("Install starter/example-pack/target/example-pack-0.1.0-SNAPSHOT.jar into Studio, then restart the IDE.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print("Authoring kit verification failed: " + str(error), file=sys.stderr)
        sys.exit(1)
