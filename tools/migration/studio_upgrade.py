#!/usr/bin/env python3
"""Before/after upgrade evidence for a developer's Studio Maven project (Python 3 stdlib)."""
import argparse
import collections
import datetime
import hashlib
import json
import os
import re
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



def build_diagnostics(log, version):
    """Use recorded Maven output, never the comparison machine's current JAVA_HOME."""
    text = re.sub(r'\x1b\[[0-9;]*m', '', log)
    result = []
    match = re.search(r'Java version:\s*([^,\s]+)', text)
    minimum = {'V3.3.9': 11, 'V4.1.6': 17}.get(version)
    if match and minimum:
        parts = match.group(1).split('.')
        major = int(parts[1] if parts[0] == '1' else re.match(r'\d+', parts[0]).group())
        if major < minimum:
            result.append(dict(problem=f'Maven ran on Java {match.group(1)}; Ikasan {version} requires JDK {minimum} or newer.',
                fix=f'Set JAVA_HOME to a JDK {minimum} installation and put its bin directory first on PATH. '
                    'Run the same Maven executable (or wrapper) with -version to confirm its Java version. '
                    'For IDE runs also set the Maven runner/importer JDK and project SDK. '
                    'Then rerun after verification into a fresh report directory and compare again.'))
    if re.search(r'invalid target release: 17|release version 17 not supported|class file version 61.*(?:55|52)', text, re.I | re.S):
        result.append(dict(problem='The build reports a Java 17 compiler/runtime incompatibility.',
            fix='Use JDK 17 for Maven and check Maven toolchains or an explicit compiler executable. '
                'The compiler can differ from the JVM launching Maven. Confirm with Maven -version and build.log, then rerun verification.'))
    if version == 'V4.1.6' and 'package javax.xml.bind.annotation does not exist' in text:
        result.append(dict(problem='Developer code still imports javax.xml.bind.annotation; compilation failed before tests could run.',
            fix='Review the compiler locations in build.log. Update affected JAXB annotations/imports to jakarta.xml.bind.annotation '
                'for Ikasan 4 and check the matching Jakarta JAXB dependencies. Studio preserves developer code during migration. '
                'Record this intentional API adaptation; source-preservation comparison will flag the edits for review.'))
    if version == 'V4.1.6' and 'package javax.jms does not exist' in text:
        result.append(dict(problem='Developer code still imports javax.jms; compilation failed before tests could run.',
            fix='Ikasan 4 uses jakarta.jms. Review the affected imports in build.log. For automatic import migration, '
                'restore the pre-migration project using the migration snapshot, then create a NEW preview with '
                '--update-user-imports true (or select Update compatible imports in user code in Studio), review and apply it. '
                'An existing plan with updateUserImports=false preserves these imports. Do not edit the saved plan. '
                'Rerun after verification and compare using the new plan. Fully qualified references and other API changes require manual review.'))
    return result


def failed_evidence(report):
    details = [f"{c['name']}: {c.get('detail', '')}" for c in report.get('checks', []) if c['status'] != 'PASS']
    return '; '.join(details) or f"Verification result: {report['status']}. Open its report.md and build.log."


def guidance(name):
    return {
        'Maven build and verification': 'Open build.log and resolve the first build error. Check Maven JDK, dependencies and test-service settings; rerun into a fresh report directory.',
        'Test execution and evidence': 'Check that the Maven executable is available and test report XML is readable. Review build.log and rerun verification.',
        'Before verification passed': 'Fix the baseline build/tests on the original version before evaluating an upgrade. Inspect the before report and build.log.',
        'After verification passed': 'Resolve the after-build errors in this report or its build.log, then rerun after verification into a fresh directory and compare again.',
        'Supported version change': 'Use reports from the same project for the supported V3.3.9 ↔ V4.1.6 migration.',
        'Developer source and test files preserved': 'Review the listed file changes. Restore accidental edits; retain and document necessary API adaptations. Do not refresh baseline tests simply to obtain a pass.',
        'Developer files unchanged by verification': 'Review changes made during the test run; prevent tests/build plugins from modifying source files, then collect fresh evidence.',
        'Model unchanged by verification': 'Review model edits during verification and rerun from a stable saved model.',
        'Source/model preservation': 'Resolve the reported file-access problem, then rerun verification without concurrent source/model edits.',
        'Same Maven command and profiles': 'Use the same Maven executable, profiles and arguments for both runs. Collect new evidence when these settings differ.',
        'Same executed test cases': 'Resolve build failures first: compilation may have prevented tests from running. Otherwise check skipped tests, profiles and Surefire/Failsafe selection; retain the same baseline tests.',
        'Model structure and settings preserved': 'Review the model differences. Supply the saved preview using --plan for exact migration mapping; do not ignore unexplained configuration changes.',
        'No failing test cases': 'Open the failing Surefire/Failsafe reports and fix the application or test environment. Keep the original expectations for comparison.',
        'Executed project tests': 'If the build failed, fix it first. Otherwise enable real project tests and remove skip flags. Structural verification tests do not establish runtime/business coverage.'
    }.get(name, 'Review the recorded evidence, correct the cause and rerun verification into a fresh report directory.')

def save(directory, report):
    report.setdefault('reportGeneratedAt', datetime.datetime.now(datetime.timezone.utc).isoformat())
    (directory/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    rows = ['# Project upgrade verification', '', f"Result: **{report['status']}**", '',
            'Evidence is limited to the project tests executed. This does not certify live delivery,',
            'flow readiness or external services unless those tests assert them.', '',
            '| Check | Result | Detail |', '| --- | --- | --- |']
    for phase, environment in report.get('javaEnvironment', {}).items():
        rows[4:4] = [f"{phase}: Ikasan **{environment['version']}**; minimum JDK **{environment['minimum']}**; Maven Java recorded: **{environment['actual']}**.", '']
    if 'version' in report:
        rows[4:4] = [f"Ikasan version: **{report['version']}**", '']
    timestamps = [f"Report generated at (UTC): {report['reportGeneratedAt']}"]
    if report.get('verifiedAt'):
        timestamps.append(f"Verification completed at (UTC): {report['verifiedAt']}")
    for phase, timestamp in report.get('verificationTimes', {}).items():
        timestamps.append(f"{phase} verification completed at (UTC): {timestamp or 'not recorded'}")
    rows[4:4] = timestamps + ['']
    for c in report['checks']:
        detail = str(c.get('detail', '')).replace('|', '\\|').replace('\n', ' ')
        rows.append(f"| {c['name']} | {c['status']} | {detail} |")
    failures = [c for c in report['checks'] if c['status'] != 'PASS']
    if failures:
        rows.extend(['', '## How to resolve the findings', ''])
        for finding in report.get('diagnostics', []):
            rows.extend([f"### {finding.get('phase', 'Build')}: {finding['problem']}", '', finding['fix'], ''])
        for c in failures:
            rows.extend([f"### {c['name']}", '', c.get('guidance') or guidance(c['name']), ''])
    if report.get('evidence'):
        rows.extend(['## Evidence', ''])
        for phase, path in report['evidence'].items():
            rows.append(f"- {phase}: `{path}` (report.json, report.md and build.log)")
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
    command = [args.maven, '-B', '-V', 'clean', 'verify', *args.maven_arg]
    print('Testing: Maven clean verify using your project tests. Success requires a successful build, '
          'executed tests and unchanged model/developer sources. Tests use your configured services.', flush=True)
    print('Command: '+repr(command)+'; log: '+str(directory/'build.log'), flush=True)
    started = time.time_ns()
    checks = []
    tests = []
    try:
        code = execute(command, project, directory/'build.log', args.timeout)
        build_detail = ('Maven completed successfully (exit code 0).' if code == 0 else
                        'Maven verification timed out or was interrupted (exit code 124).' if code == 124 else
                        f'Maven failed (exit code {code}); see build.log.')
        checks.append(check('Maven build and verification', code == 0, build_detail))
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
    log = directory/'build.log'
    log_text = log.read_text(errors='replace') if log.exists() else ''
    report['diagnostics'] = build_diagnostics(log_text, report['version'])
    java_match = re.search(r'Java version:\s*([^,\s]+)', log_text)
    report['mavenJavaVersion'] = java_match.group(1) if java_match else None
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
    approved = []
    if plan is not None:
        import base64
        seen = set()
        for change in plan.get('changes', []):
            path = change['path']
            if path in seen: raise ValueError('Duplicate path in migration plan: '+path)
            seen.add(path)
            if not path.startswith('user/src/main/java/') or not path.endswith('.java'): continue
            if '..' in path.split('/') or '\\' in path: raise ValueError('Invalid user import path in migration plan')
            if change.get('before') is None or change.get('after') is None: continue
            old = hashlib.sha256(base64.b64decode(change['before'], validate=True)).hexdigest()
            new = hashlib.sha256(base64.b64decode(change['after'], validate=True)).hexdigest()
            if path in changed and before['userSourceHashes'].get(path) == old and after['userSourceHashes'].get(path) == new:
                approved.append(path)
    unexpected = [path for path in changed if path not in approved]
    checks = [check('Before verification passed', before['status'] == 'PASS', '' if before['status'] == 'PASS' else failed_evidence(before)),
              check('After verification passed', after['status'] == 'PASS', '' if after['status'] == 'PASS' else failed_evidence(after)),
              check('Supported version change', versions == {'V3.3.9', 'V4.1.6'}),
              check('Developer source and test files preserved', not unexpected,
                    ('Approved plan changes: '+', '.join(approved)+'. ' if approved else '') +
                    ('Unexpected changes: '+', '.join(unexpected) if unexpected else 'All other protected files unchanged.')),
              check('Same Maven command and profiles', [a for a in before['command'] if a not in {'-V', '--show-version'}] == [a for a in after['command'] if a not in {'-V', '--show-version'}]),
              check('Same executed test cases', bool(before['tests']) and
                    collections.Counter(t['id'] for t in before['tests'] if t['status'] == 'PASS') ==
                    collections.Counter(t['id'] for t in after['tests'] if t['status'] == 'PASS'))]
    target_jdk = {'V3.3.9': 11, 'V4.1.6': 17}.get(after['version'])
    if target_jdk:
        checks[1]['guidance'] = (f"For Ikasan {after['version']}, use JDK {target_jdk}. Set JAVA_HOME to that JDK "
            "and put its bin directory first on PATH; confirm with the same Maven executable or wrapper using -version. "
            "Check Maven toolchains if a separate compiler is configured. " + guidance('After verification passed'))
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
    diagnostics = [dict(d, phase=phase) for phase, report in [('Before', before), ('After', after)] for d in report.get('diagnostics', [])]
    return dict(format=FORMAT, status=status(checks), checks=checks, diagnostics=diagnostics,
                verificationTimes={'Before': before.get('verifiedAt'), 'After': after.get('verifiedAt')},
                javaEnvironment={phase: dict(version=r['version'], minimum={'V3.3.9': 11, 'V4.1.6': 17}.get(r['version'], 'see target requirements'),
                                             actual=r.get('mavenJavaVersion') or 'not recorded; check Maven -version and rerun verification')
                                 for phase, r in [('After', after), ('Before', before)]})


def compare(args):
    before = json.loads(Path(args.before).read_text())
    after = json.loads(Path(args.after).read_text())
    plan = json.loads(Path(args.plan).read_text()) if args.plan else None
    for report, path in [(before, args.before), (after, args.after)]:
        log = Path(path).resolve().parent/'build.log'
        if log.is_file():
            log_text = log.read_text(errors='replace')
            report['diagnostics'] = build_diagnostics(log_text, report['version'])
            java_match = re.search(r'Java version:\s*([^,\s]+)', log_text)
            if java_match: report['mavenJavaVersion'] = java_match.group(1)
    report = comparison(before, after, plan)
    report['evidence'] = {'Before': str(Path(args.before).resolve().parent), 'After': str(Path(args.after).resolve().parent)}
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
