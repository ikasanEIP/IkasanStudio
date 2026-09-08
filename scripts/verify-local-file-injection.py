#!/usr/bin/env python3
"""Compile and exercise the local-file debug controller against cached Ikasan APIs.
First export with STUDIO_INJECT_EXPORT=/tmp/studio-inject-check ./gradlew test --tests '*StudioInjectControllerTemplateTest'.
Then run: python3 scripts/verify-local-file-injection.py /tmp/studio-inject-check
No dependencies are downloaded.
"""
from pathlib import Path
import os
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
m2 = Path.home() / '.m2/repository'
for pack, release in [('V3.3.9', '11'), ('V4.1.6', '17')]:
    folder = Path(sys.argv[1]) / pack
    classes = folder / 'classes'
    classes.mkdir(exist_ok=True)
    jars = list((m2 / 'org/ikasan').glob('*/' + pack[1:] + '/*.jar'))
    if not jars:
        raise SystemExit('Missing cached Ikasan artifacts for ' + pack)
    for location in ['org/springframework', 'org/quartz-scheduler', 'com/fasterxml/jackson',
                     'javax/annotation', 'jakarta/annotation', 'javax/jms', 'jakarta/jms',
                     'javax/transaction', 'jakarta/transaction', 'org/slf4j/slf4j-api']:
        jars += sorted((m2 / location).rglob('*.jar'), reverse=True)
    jars = [jar for jar in jars if not jar.name.endswith(('-sources.jar', '-javadoc.jar'))]
    classpath = os.pathsep.join([str(classes)] + list(map(str, jars)))
    commands = [
        ['javac', '-proc:none', '--release', release, '-cp', classpath, '-d', str(classes),
         str(folder / 'StudioInjectController.java'),
         str(root / 'src/test/resources/studio/inject/VerifyLocalFiles.java')],
        ['java', '-cp', classpath, 'org.ikasan.studio.boot.VerifyLocalFiles']]
    for command in commands:
        result = subprocess.run(command, capture_output=True, text=True)
        print(result.stdout, end='')
        if result.returncode:
            print(result.stderr, file=sys.stderr)
            raise SystemExit(result.returncode)
    print(pack + ': interface-proxied local file consumer checks passed')
