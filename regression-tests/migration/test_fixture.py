"""Regression tests for rejecting misleading before/after acceptance evidence."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest

spec=importlib.util.spec_from_file_location('fixture',Path(__file__).with_name('fixture.py'))
fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)

class ComparisonTest(unittest.TestCase):
 def compare(self,before,after):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp)
   for name,data in [('before',before),('after',after)]: (root/(name+'.json')).write_text(json.dumps(data))
   code=fixture.compare(SimpleNamespace(before=root/'before.json',after=root/'after.json',report=root/'report'))
   return code,json.loads((root/'report/report.json').read_text())
 def baseline(self):
  return dict(status='PASS',checks=[dict(name='JMS delivery',status='PASS')],userSourceHashes={'user/src/Order.java':'abc'},model={'version':'V3.3.9','name':'Fixture','flows':[{'name':'JMS','fromType':'javax.jms.Message','description':'keep javax.jms.Message text'}]})
 def test_allows_supported_type_namespace_change(self):
  before=self.baseline();after=copy.deepcopy(before);after['model']['version']='V4.1.6';after['model']['flows'][0]['fromType']='jakarta.jms.Message'
  self.assertEqual(0,self.compare(before,after)[0])
 def test_fails_source_changes_even_when_both_runtime_runs_pass(self):
  before=self.baseline();after=copy.deepcopy(before);after['userSourceHashes']['user/src/Order.java']='changed'
  self.assertEqual(1,self.compare(before,after)[0])
 def test_does_not_hide_non_type_property_changes(self):
  before=self.baseline();after=copy.deepcopy(before);after['model']['flows'][0]['description']='keep jakarta.jms.Message text'
  self.assertEqual(1,self.compare(before,after)[0])
 def test_rejects_lost_or_changed_wiretap_decorators(self):
  before=self.baseline()
  before['model']['flows'][0]['decorators']=[{'type':'Wiretap','name':'BEFORE Enrich Order','timeToLive':'300'}, {'type':'LogWiretap','name':'AFTER Enrich Order'}]
  for change in ['remove','ttl','position']:
   with self.subTest(change=change):
    after=copy.deepcopy(before)
    decorators=after['model']['flows'][0]['decorators']
    if change=='remove': decorators.pop()
    elif change=='ttl': decorators[0]['timeToLive']='1'
    else: decorators[1]['name']='BEFORE Enrich Order'
    self.assertEqual(1,self.compare(before,after)[0])
 def test_fails_missing_acceptance_check_and_failed_runtime(self):
  before=self.baseline();after=copy.deepcopy(before);after['checks']=[];after['status']='FAIL'
  code,report=self.compare(before,after)
  self.assertEqual(1,code)
  self.assertEqual(2,sum(c['status']=='FAIL' for c in report['checks']))

if __name__=='__main__':unittest.main()
