#!/usr/bin/env python3
"""Create, verify and compare a repeatable Studio migration fixture (Python 3 stdlib only)."""
import argparse, datetime, hashlib, json, os, re, signal, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parent
REPO=ROOT.parent.parent

def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def user_hashes(project):
 return {str(p.relative_to(project)):digest(p) for p in sorted((project/'user/src').rglob('*')) if p.is_file()}
def save_report(directory,report):
 directory.mkdir(parents=True,exist_ok=True)
 (directory/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 lines=['# Migration acceptance report','',f"Result: **{report['status']}**",'',f"Version: {report.get('version','comparison')}",'', '| Check | Result | Detail |','| --- | --- | --- |']
 for c in report['checks']:
  detail=str(c.get('detail','')).replace('|','\\|').replace('\n',' ')[:1000]
  lines.append(f"| {c['name']} | {c['status']} | {detail} |")
 if 'coverage' in report:
  lines+=['','## Catalogue coverage','','| Key | Coverage |','| --- | --- |']
  for key,places in report['coverage'].items(): lines.append(f"| {key} | {'; '.join(places)} |")
 lines+=['','Runtime tests use isolated application instances. Test teardown does not stop your IDE module.',
         'A returned Spring context close is not evidence that every JVM worker terminated. See build.log and shutdown notes.']
 (directory/'report.md').write_text('\n'.join(lines)+'\n')

def create(args):
 if not (ROOT/'builder').is_dir():raise ValueError('Create requires the Studio repository; use verify, serve or compare in a generated workspace')
 name=args.name or datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'-'+args.version
 if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*',name): raise ValueError('Use a simple workspace name containing letters, digits, dots, underscores or hyphens')
 # builder/build.gradle.kts resolves the headless generator/packs from the local Maven repo (mavenLocal()), not a
 # public repository - they are never released there. Publish the current source so create() has no undocumented
 # manual prerequisite and never runs a stale generator left over from an earlier checkout.
 subprocess.run([str(REPO/'gradlew'),'-p',str(REPO/'headless'),'publishToMavenLocal','--console=plain'],check=True)
 subprocess.run([str(REPO/'gradlew'),'-p',str(ROOT/'builder'),'run','--args',f'"{ROOT}" {args.version} {name}','--console=plain'],check=True)
 project=ROOT/'build'/name
 print('Open this Maven project in IntelliJ:',project)
 print('Verify:',sys.executable,Path(__file__),'verify',project)

def verify(args):
 project=Path(args.project).resolve();model=project/'generated/src/main/model/model.json'
 data=json.loads(model.read_text());stamp=datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
 directory=Path(args.report).resolve() if args.report else project/'acceptance-reports'/stamp
 if directory.exists():raise ValueError('Choose a new report directory; previous evidence is never overwritten')
 directory.mkdir(parents=True);runtime=directory/'runtime';runtime.mkdir()
 before=user_hashes(project);model_before=digest(model)
 command=['mvn','-f',str(project/'pom.xml'),f'-Dfixture.runtimeDirectory={runtime}',f'-Dfixture.reportFile={directory / "runtime.json"}','test']
 started=datetime.datetime.now(datetime.timezone.utc).isoformat()
 with (directory/'build.log').open('w') as log:
  process=subprocess.Popen(command,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
  try:code=process.wait(timeout=300)
  except subprocess.TimeoutExpired:
   os.killpg(process.pid,signal.SIGTERM)
   try:process.wait(timeout=10)
   except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait()
   code=124
 runtime_report=directory/'runtime.json'
 report=json.loads(runtime_report.read_text()) if runtime_report.exists() else {'checks':[{'name':'runtime evidence','status':'FAIL','detail':'No completed runtime report; inspect build.log'}]}
 report['checks'].extend([
  dict(name='Maven build and test execution',status='PASS' if code==0 else 'FAIL',detail=f'exit {code}'),
  dict(name='developer source unchanged by verification',status='PASS' if before==user_hashes(project) else 'FAIL'),
  dict(name='model unchanged by verification',status='PASS' if model_before==digest(model) else 'FAIL')])
 report.update(version=data['version'],startedAt=started,project=str(project),userSourceHashes=before,model=data,command=command)
 coverage=json.loads((ROOT/'coverage.json').read_text())
 catalogue=json.loads((project/'generated/src/main/model/component-catalogue.json').read_text())
 for component in catalogue['components']:
  key=component['key']
  if key not in coverage:
   if component.get('role') in ['End Point','Module','Flow','Debug']:
    coverage[key]=['Structural/visual catalogue entry; no independent runtime implementation. See coverage notes.']
   else:
    coverage[key]=['MISSING'];report['checks'].append(dict(name='catalogue coverage '+key,status='FAIL',detail='New executable component needs a fixture scenario'))
 report['coverage']=dict(sorted(coverage.items()))
 report['status']='FAIL' if any(c['status']=='FAIL' for c in report['checks']) else 'BLOCKED' if any(c['status']=='BLOCKED' for c in report['checks']) else 'PASS'
 save_report(directory,report);print(directory/'report.md');return 0 if report['status']=='PASS' else 1

def serve(args):
 project=Path(args.project).resolve()
 subprocess.run(['mvn','-f',str(project/'pom.xml'),'-DskipTests','package',
  'org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath',
  '-Dmdep.outputFile=target/service-classpath.txt'],check=True)
 cp=(project/'verification/target/service-classpath.txt').read_text().strip()
 cp=str(project/'verification/target/test-classes')+os.pathsep+cp
 return subprocess.call(['java','-cp',cp,'fixture.verification.LocalServices'],cwd=project)

def compare(args):
 before=json.loads(Path(args.before).read_text());after=json.loads(Path(args.after).read_text())
 checks=[dict(name='baseline passed',status='PASS' if before['status']=='PASS' else 'FAIL'),dict(name='target passed',status='PASS' if after['status']=='PASS' else 'FAIL'),dict(name='developer source preserved across migration',status='PASS' if before['userSourceHashes']==after['userSourceHashes'] else 'FAIL')]
 def normalized(value,field=None):
  if isinstance(value,dict):return {k:normalized(v,k) for k,v in value.items() if not(field is None and k=='version')}
  if isinstance(value,list):return [normalized(v,'item') for v in value]
  if isinstance(value,str) and field in {'fromType','toType','type','objectClass','implementingClass'}:
   return value.replace('javax.jms.','jakarta.jms.').replace('javax.xml.bind.','jakarta.xml.bind.')
  return value
 checks.append(dict(name='model structure and settings preserved',status='PASS' if normalized(before['model'])==normalized(after['model']) else 'FAIL',detail='Allows only version and JMS/JAXB namespace changes; inspect other changes explicitly'))
 checks.append(dict(name='same acceptance checks executed',status='PASS' if {c['name'] for c in before['checks']}=={c['name'] for c in after['checks']} else 'FAIL'))
 result=dict(status='PASS' if all(c['status']=='PASS' for c in checks) else 'FAIL',checks=checks,before=str(args.before),after=str(args.after))
 directory=Path(args.report); 
 if directory.exists():raise ValueError('Choose a new comparison report directory')
 save_report(directory,result);print(directory/'report.md');return 0 if result['status']=='PASS' else 1

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);sub=parser.add_subparsers(dest='action',required=True)
 p=sub.add_parser('create');p.add_argument('--version',choices=['V3.3.9','V4.1.6'],default='V3.3.9');p.add_argument('--name')
 p=sub.add_parser('verify');p.add_argument('project');p.add_argument('--report')
 p=sub.add_parser('serve');p.add_argument('project')
 p=sub.add_parser('compare');p.add_argument('before');p.add_argument('after');p.add_argument('--report',required=True)
 args=parser.parse_args()
 try:sys.exit({'create':create,'verify':verify,'compare':compare,'serve':serve}[args.action](args) or 0)
 except (OSError,ValueError,subprocess.CalledProcessError) as error:parser.exit(2,str(error)+'\n')
