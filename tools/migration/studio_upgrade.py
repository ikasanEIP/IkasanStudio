#!/usr/bin/env python3
"""Before/after upgrade evidence for a developer's Studio Maven project (Python 3 stdlib)."""
import argparse
import collections
import datetime
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

MODEL = Path('generated/src/main/model/model.json')
FORMAT = 'ikasan-project-verification-1'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sources(project):
    """Protect user files and application/test sources in additional Maven modules."""
    result = {}
    for base, dirs, files in os.walk(project, followlinks=False):
        dirs[:] = [d for d in dirs if d not in {'target', '.git', '.idea', '.ikasan-studio', '__pycache__'}
                   and d not in {'migration-before', 'migration-after', 'migration-comparison', 'migration-reports', 'acceptance-reports'}]
        for name in files:
            path = Path(base) / name
            rel = path.relative_to(project)
            if rel.parts[0] != 'user' and ('src' not in rel.parts or rel.is_relative_to('generated/src/main')):
                continue
            if path.is_symlink():
                raise ValueError(f'Source symlinks are not supported: {rel}')
            result[rel.as_posix()] = digest(path)
        for d in dirs:
            if (Path(base)/d).is_symlink():
                raise ValueError(f'Project directory symlinks are not supported: {Path(base)/d}')
    return dict(sorted(result.items()))


def check(name, passed, detail=''):
    return dict(name=name, status='PASS' if passed else 'FAIL', detail=detail)


def save(directory, report):
    (directory/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    rows = ['# Project upgrade verification', '', f"Result: **{report['status']}**", '',
            'Evidence is limited to the project tests executed. This does not certify live delivery,',
            'flow readiness or external services unless those tests assert them.', '',
            '| Check | Result | Detail |', '| --- | --- | --- |']
    if 'version' in report:
        rows[4:4] = [f"Ikasan version: **{report['version']}**", '']
    for c in report['checks']:
        detail = str(c.get('detail', '')).replace('|', '\\|').replace('\n', ' ')
        rows.append(f"| {c['name']} | {c['status']} | {detail} |")
    (directory/'report.md').write_text('\n'.join(rows)+'\n')
    print(directory/'report.md', flush=True)


def new_directory(path):
    directory = Path(path).resolve()
    directory.mkdir(parents=True, exist_ok=False)
    return directory


def test_reports(project, started_ns):
    results = []
    for folder in ('surefire-reports', 'failsafe-reports'):
        for path in sorted(project.glob(f'**/target/{folder}/TEST-*.xml')):
            # clean verify should recreate reports; never count stale reports from inactive modules.
            if path.stat().st_mtime_ns < started_ns:
                continue
            tree = ET.parse(path)
            for case in tree.getroot().iter('testcase'):
                state = ('FAIL' if case.find('failure') is not None or case.find('error') is not None else
                         'SKIP' if case.find('skipped') is not None else 'PASS')
                results.append(dict(id=f"{path.parent.parent.parent.relative_to(project).as_posix()}/{folder}/"
                                       f"{case.get('classname','')}/{case.get('name','')}", status=state))
    return results


def execute(command, project, log, timeout):
    with log.open('w') as stream:
        process = subprocess.Popen(command, cwd=project, stdout=stream, stderr=subprocess.STDOUT,
                                   start_new_session=(os.name == 'posix'))
        try:
            deadline = time.monotonic() + timeout
            while True:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise subprocess.TimeoutExpired(command, timeout)
                try:
                    return process.wait(timeout=min(30, remaining))
                except subprocess.TimeoutExpired:
                    if time.monotonic() >= deadline:
                        raise
                    print(f'Maven verification is still running; progress: {log}', flush=True)
        except (subprocess.TimeoutExpired, KeyboardInterrupt):
            if os.name == 'posix':
                os.killpg(process.pid, signal.SIGTERM)
            else:
                process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                if os.name == 'posix':
                    os.killpg(process.pid, signal.SIGKILL)
                else:
                    process.kill()
                process.wait()
            return 124


def verify(args):
    project = Path(args.project).resolve()
    if not (project/'pom.xml').is_file() or not (project/'user').is_dir():
        raise ValueError('Expected a Studio Maven project with pom.xml and user/.')
    model = json.loads((project/MODEL).read_text())
    before = sources(project)
    model_hash = digest(project/MODEL)
    # Relative report locations belong to the project, regardless of the caller's working directory.
    report_path = Path(args.report) if args.report else Path('migration-reports')/datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    candidate = (project/report_path).resolve()
    if candidate.is_relative_to(project):
        parts = candidate.relative_to(project).parts
        if not parts or any(p in {'target', 'src', 'user', 'generated', '.git', '.ikasan-studio'} for p in parts):
            raise ValueError('Keep reports outside source, build output and Studio recovery directories.')
    directory = new_directory(candidate)
    command = [args.maven, '-B', 'clean', 'verify', *args.maven_arg]
    print('Testing: Maven clean verify using your project tests. Success requires a successful build, '
          'executed tests and unchanged model/developer sources. Tests use your configured services.', flush=True)
    print('Command: '+repr(command)+'; log: '+str(directory/'build.log'), flush=True)
    started = time.time_ns()
    checks = []
    tests = []
    try:
        code = execute(command, project, directory/'build.log', args.timeout)
        checks.append(check('Maven build and verification', code == 0, f'exit {code}; 124 means timeout/interruption'))
        tests = test_reports(project, started)
    except (OSError, ET.ParseError) as error:
        checks.append(check('Test execution and evidence', False, str(error)))
    try:
        checks.append(check('Developer files unchanged by verification', before == sources(project)))
        checks.append(check('Model unchanged by verification', model_hash == digest(project/MODEL)))
    except (OSError, ValueError) as error:
        checks.append(check('Source/model preservation', False, str(error)))
    passed = [t for t in tests if t['status'] == 'PASS']
    checks.append(check('No failing test cases', not any(t['status'] == 'FAIL' for t in tests)))
    checks.append(dict(name='Executed project tests', status='PASS' if passed else 'INCOMPLETE',
                       detail=f"{len(passed)} passed; {sum(t['status']=='SKIP' for t in tests)} skipped. No executed tests means behaviour is unverified."))
    report = dict(format=FORMAT, project=str(project), version=model.get('version'), model=model,
                  userSourceHashes=before, tests=tests, command=command, checks=checks,
                  verifiedAt=datetime.datetime.now(datetime.timezone.utc).isoformat())
    report['status'] = status(checks)
    save(directory, report)
    return exit_code(report['status'])


def status(checks):
    return 'FAIL' if any(c['status'] == 'FAIL' for c in checks) else 'INCOMPLETE' if any(c['status'] == 'INCOMPLETE' for c in checks) else 'PASS'


def exit_code(result):
    return {'PASS': 0, 'FAIL': 1, 'INCOMPLETE': 2}[result]


def normalized(value, field=None):
    # Conservative fallback for IDE migrations. Other changes require review, never blanket replacement.
    if isinstance(value, dict):
        return {k: normalized(v, k) for k, v in value.items() if not (field is None and k == 'version')}
    if isinstance(value, list):
        return [normalized(v, 'item') for v in value]
    if isinstance(value, str) and field in {'fromType', 'toType', 'type', 'objectClass', 'implementingClass'}:
        return value.replace('javax.jms.', 'jakarta.jms.').replace('javax.xml.bind.', 'jakarta.xml.bind.')
    return value


def comparison(before, after, plan=None):
    if before.get('format') != FORMAT or after.get('format') != FORMAT:
        raise ValueError('Expected reports produced by studio_upgrade.py.')
    versions = {before['version'], after['version']}
    changed = sorted(k for k in before['userSourceHashes'].keys() | after['userSourceHashes'].keys()
                     if before['userSourceHashes'].get(k) != after['userSourceHashes'].get(k))
    checks = [check('Before verification passed', before['status'] == 'PASS'),
              check('After verification passed', after['status'] == 'PASS'),
              check('Supported version change', versions == {'V3.3.9', 'V4.1.6'}),
              check('Developer source and test files preserved', not changed, ', '.join(changed)),
              check('Same Maven command and profiles', before['command'] == after['command']),
              check('Same executed test cases', bool(before['tests']) and
                    collections.Counter(t['id'] for t in before['tests'] if t['status'] == 'PASS') ==
                    collections.Counter(t['id'] for t in after['tests'] if t['status'] == 'PASS'))]
    if plan is not None:
        import base64
        change = next(c for c in plan['changes'] if c['path'] == MODEL.as_posix())
        matches = (before['model'] == json.loads(base64.b64decode(change['before'])) and
                   after['model'] == json.loads(base64.b64decode(change['after'])))
        detail = 'Matches the exact source and target model in the saved engine preview.'
    else:
        matches = normalized(before['model']) == normalized(after['model'])
        detail = 'Allows only root version and JMS/JAXB type-field substitutions. Other differences require review; use --plan for exact engine mapping.'
    checks.append(check('Model structure and settings preserved', matches, detail))
    return dict(format=FORMAT, status=status(checks), checks=checks)


def compare(args):
    before = json.loads(Path(args.before).read_text())
    after = json.loads(Path(args.after).read_text())
    plan = json.loads(Path(args.plan).read_text()) if args.plan else None
    report = comparison(before, after, plan)
    default = Path(args.after).resolve().parent.parent/'migration-comparison'
    directory = new_directory(args.report or default)
    save(directory, report)
    return exit_code(report['status'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='action', required=True)
    p = commands.add_parser('verify')
    p.add_argument('project')
    p.add_argument('--report', help='Fresh directory; relative paths are resolved under the project root')
    p.add_argument('--maven', default='mvn', help='Maven executable (use an absolute path for a wrapper)')
    p.add_argument('--maven-arg', action='append', default=[], help='Repeat for Maven settings/profiles, e.g. --maven-arg=-Pacceptance')
    p.add_argument('--timeout', type=int, default=900)
    p = commands.add_parser('compare')
    p.add_argument('before'); p.add_argument('after'); p.add_argument('--report'); p.add_argument('--plan')
    args = parser.parse_args()
    try:
        if getattr(args, 'timeout', 1) <= 0:
            raise ValueError('Timeout must be positive.')
        return {'verify': verify, 'compare': compare}[args.action](args)
    except (OSError, ValueError, KeyError, StopIteration) as error:
        print(f'Upgrade verification: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
