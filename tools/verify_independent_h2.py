#!/usr/bin/env python3
"""Exercise the generated child JVM against supplied H2 JARs; downloads/changes no dependencies."""
import argparse
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'src/main/resources/studio/runtime/IndependentH2.java.txt'


def port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def closed(number):
    with socket.socket() as sock:
        sock.settimeout(.2)
        return sock.connect_ex(('127.0.0.1', number)) != 0


def launch(java, classes, jar, tcp, web):
    process = subprocess.Popen([java, '-cp', os.pathsep.join([str(classes), str(jar)]),
                                'org.ikasan.studio.boot.IndependentH2', str(tcp), str(web)],
                               stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    lines, ready = [], threading.Event()

    def output():
        for line in process.stdout:
            lines.append(line)
            if 'IKASAN_STUDIO_H2_READY' in line:
                ready.set()

    threading.Thread(target=output, daemon=True).start()
    return process, lines, ready


def query(java, jar, tcp, database, sql):
    result = subprocess.run([java, '-cp', str(jar), 'org.h2.tools.Shell', '-url',
                             f'jdbc:h2:tcp://127.0.0.1:{tcp}/{database.as_posix()}',
                             '-user', 'sa', '-password', '', '-sql', sql], text=True, capture_output=True, timeout=15)
    assert result.returncode == 0 and 'Error:' not in result.stdout, result.stdout + result.stderr
    return result.stdout


def stop(process):
    if process.poll() is None:
        process.stdin.write('STOP\n')
        process.stdin.flush()
        assert process.wait(timeout=10) == 0


def verify(java, classes, jar, database):
    tcp, web = port(), port()
    while tcp == web:
        web = port()
    process, lines, ready = launch(java, classes, jar, tcp, web)
    try:
        assert ready.wait(10), ''.join(lines)
        assert process.poll() is None, ''.join(lines)
        with urllib.request.urlopen(f'http://127.0.0.1:{web}/', timeout=5) as response:
            assert response.status == 200
        query(java, jar, tcp, database, 'CREATE TABLE PROBE(ID INT PRIMARY KEY); INSERT INTO PROBE VALUES(1);')
        time.sleep(1.2)
        assert process.poll() is None, 'Server stopped while idle'
        assert '1' in query(java, jar, tcp, database, 'SELECT COUNT(*) FROM PROBE;')
        stop(process)
        assert closed(tcp) and closed(web), 'Graceful stop left a listener open'
        process, lines, ready = launch(java, classes, jar, tcp, web)
        assert ready.wait(10), ''.join(lines)
        assert '1' in query(java, jar, tcp, database, 'SELECT COUNT(*) FROM PROBE;')
        process.terminate()  # The IDE's normal Java Stop also exercises the JVM shutdown hook.
        process.wait(timeout=10)
        assert closed(tcp) and closed(web), 'JVM shutdown hook left a listener open'
        with socket.socket() as occupied:
            occupied.bind(('127.0.0.1', 0))
            occupied.listen()
            conflicting_web = occupied.getsockname()[1]
            process, lines, ready = launch(java, classes, jar, tcp, conflicting_web)
            assert process.wait(timeout=15) != 0
            assert not ready.is_set(), 'A port conflict was reported as ready'
            assert closed(tcp), 'Web startup failure leaked the TCP server'
        print(f'PASS {jar.name}: web/TCP, idle/later query, persisted data, graceful stop, JVM Stop, startup rollback', flush=True)
    finally:
        if process.poll() is None:
            process.kill()
            process.wait(timeout=5)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--h2-jar', action='append', type=Path, required=True)
    parser.add_argument('--java', default='java')
    parser.add_argument('--javac', default='javac')
    args = parser.parse_args()
    output = ROOT / 'build/verification/independent-h2'
    output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output) as temporary:
        work = Path(temporary)
        source = work / 'IndependentH2.java'
        source.write_text(SOURCE.read_text())
        subprocess.run([args.javac, '--release', '11', '-d', str(work), str(source)], check=True)
        for i, jar in enumerate(args.h2_jar):
            verify(args.java, work, jar.resolve(strict=True), work / f'database-{i}')
        missing = subprocess.run([args.java, '-cp', str(work), 'org.ikasan.studio.boot.IndependentH2',
                                  str(port()), str(port())], capture_output=True, text=True, timeout=10)
        assert missing.returncode != 0 and 'H2 is missing' in missing.stderr
        print('PASS missing H2 dependency: actionable startup failure')


if __name__ == '__main__':
    main()
