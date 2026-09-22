"""Deterministic specification. Re-run only when intentionally revising the fixture."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parent
USER=ROOT/'project/user/src/main/java'
def write(path,text):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
write(ROOT/'baseline.json',json.dumps(dict(applicationPackageName='fixture',name='MigrationRegression',version='V3.3.9',flowStartupType='AUTOMATIC',port='18580',h2DbPortNumber='18581',h2WebPortNumber='18582',useEmbeddedH2=True,flows=[]),indent=2)+'\n')
batches=[]; coverage={}; ops=[]; flow=''
def begin(name):
 global ops,flow
 flow=name;ops=[dict(type='addFlow',flow=flow)];batches.append(ops)
def add(key,name,props=None,route=None):
 op=dict(type='addComponent',flow=flow,key=key,name=name)
 if props:op['properties']=props
 if route:op['route']=route
 ops.append(op);coverage.setdefault(key,[]).append(flow+'/'+name)
def custom(key,name,interface,method,props=None,route=None):
 clazz=name.replace(' ','');pkg='fixture.'+flow.replace(' ','').lower()
 fields={'userImplementedClassName':clazz,**(props or {})}
 add(key,name,fields,route)
 write(USER/Path(pkg.replace('.','/'))/(clazz+'.java'),f'package {pkg};\n@org.springframework.stereotype.Component("{pkg}.{clazz}")\npublic class {clazz} implements {interface} {{\n{method}\n}}\n')
def timer():add('Scheduled Consumer','Timer',{'cronExpression':'0/5 * * * * ?','eager':False})
def text_source(text):custom('Converter','Make Text','org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,String>',f'public String convert(org.quartz.JobExecutionContext input) {{ return "{text}"; }}',{'fromType':'org.quartz.JobExecutionContext','toType':'java.lang.String'})
order='fixture.Order'
begin('Core Pipeline');timer()
custom('Converter','Make Batch','org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,java.util.List>', 'private int batch; public java.util.List convert(org.quartz.JobExecutionContext input) { int b=++batch; return java.util.List.of(java.util.List.of(new fixture.Order("B"+b+"P",2,true),new fixture.Order("B"+b+"S",1,false),new fixture.Order("B"+b+"R",0,false),new fixture.Order("",1,false),new fixture.Order("B"+b+"DROP",-1,false))); }',{'fromType':'org.quartz.JobExecutionContext','toType':'java.util.List'})
add('Default List Splitter','Unpack Batch')
custom('Splitter','Split Orders','org.ikasan.spec.component.splitting.Splitter<java.util.List,fixture.Order>', 'public java.util.List<fixture.Order> split(java.util.List input) { return new java.util.ArrayList<fixture.Order>(input); }',{'fromType':'java.util.List','toType':order})
custom('Message Filter','Accept Named Orders','org.ikasan.spec.component.filter.Filter<fixture.Order>','public fixture.Order filter(fixture.Order o) { return o.getId().isEmpty()?null:o; }',{'fromType':order})
custom('Default Message Filter','Accept Known Quantities','org.ikasan.spec.component.filter.FilterRule<fixture.Order>','public boolean accept(fixture.Order o) { return o.getQuantity()>=0; }',{'fromType':order})
custom('Translator','Mark Translated','org.ikasan.spec.component.transformation.Translator<fixture.Order>','public void translate(fixture.Order o) { o.setStage("translated"); }',{'type':order})
custom('Broker','Enrich Order','org.ikasan.spec.component.endpoint.Broker<fixture.Order,fixture.Order>','public fixture.Order invoke(fixture.Order o) { o.setStage(o.getStage()+" enriched"); return o; }',{'fromType':order,'toType':order})
custom('Single Recipient Router','Choose Route','org.ikasan.spec.component.routing.SingleRecipientRouter<fixture.Order>','public String route(fixture.Order o) { return o.getQuantity()<=0?"Rejected":o.isPriority()?"Priority":"Standard"; }',{'fromType':order,'toType':'java.lang.String'})
ops.append(dict(type='configureRoutes',flow=flow,component='Choose Route',names=['Priority','Standard','Rejected']))
for route in ['Priority','Standard','Rejected']:add('Logging Producer','Log '+route,route=[route])
begin('Fanout');add('Generic Consumer','Controlled Input',{'userImplementedClassName':'ControlledInput'})
pkg='fixture.fanout'
write(USER/'fixture/fanout/ControlledInput.java','''package fixture.fanout;
@org.springframework.stereotype.Component("fixture.fanout.ControlledInput")
public class ControlledInput implements org.ikasan.spec.component.endpoint.Consumer<org.ikasan.spec.event.EventListener,org.ikasan.spec.event.EventFactory> {
 private org.ikasan.spec.event.EventListener listener; private org.ikasan.spec.event.EventFactory factory; private volatile boolean running;
 public void setListener(org.ikasan.spec.event.EventListener l){listener=l;} public void setEventFactory(org.ikasan.spec.event.EventFactory f){factory=f;}
 public org.ikasan.spec.event.EventFactory getEventFactory(){return factory;} public void start(){running=true;} public void stop(){running=false;} public boolean isRunning(){return running;}
 public void send(fixture.Order order){if(!running)throw new IllegalStateException("Not running");listener.invoke(factory.newEvent(order.getId(),order));}
}
''')
custom('Multi Recipient Router','Copy To Both','org.ikasan.spec.component.routing.MultiRecipientRouter<fixture.Order>','public java.util.List<String> route(fixture.Order o) { return java.util.List.of("Audit","Fulfilment"); }',{'fromType':order,'toType':'java.util.List<String>'})
ops.append(dict(type='configureRoutes',flow=flow,component='Copy To Both',names=['Audit','Fulfilment']))
custom('Generic Producer','Audit Order','org.ikasan.spec.component.endpoint.Producer<fixture.Order>','public void invoke(fixture.Order o) { o.setStage("audit mutation"); }',{'fromType':order},['Audit'])
add('Dev Null Producer','Fulfil Order',route=['Fulfilment'])
begin('Exclusion');add('Scheduled Consumer','Timer',{'cronExpression':'0/5 * * * * ?','eager':False,'messageProvider':'OrderProvider'})
write(USER/'fixture/exclusion/OrderProvider.java','''package fixture.exclusion;
@org.springframework.stereotype.Component("fixture.exclusion.OrderProvider")
public class OrderProvider implements org.ikasan.component.endpoint.quartz.consumer.MessageProvider<fixture.Order> {
 private int count; private fixture.Order pending;
 public synchronized fixture.Order invoke(org.quartz.JobExecutionContext c){
  if(pending==null){int n=count+1;pending=new fixture.Order("E"+n,n%3==2?0:1,false);}
  if(!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("Transactional source required");
  org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
   public void afterCompletion(int status){if(status==STATUS_COMMITTED)synchronized(OrderProvider.this){count++;pending=null;}}
  });
  return pending;
 }
}
''')
custom('Converter','Validate Order','org.ikasan.spec.component.transformation.Converter<fixture.Order,fixture.Order>','public fixture.Order convert(fixture.Order o) { if(o.getQuantity()<=0)throw new org.ikasan.spec.component.transformation.TransformationException("Invalid quantity "+o.getId());return o; }',{'fromType':order,'toType':order})
add('Logging Producer','Accepted Order');ops.append(dict(type='setExceptionResolution',flow=flow,exception='org.ikasan.spec.component.transformation.TransformationException',action='excludeEvent'));coverage['Exception Resolver']=[flow]
begin('Event Source');add('Event Generating Consumer','Paced Events',{'endpointEventProvider':'PacedEvents'});add('Dev Null Producer','Discard Sample')
write(USER/'fixture/eventsource/PacedEvents.java','''package fixture.eventsource;
@org.springframework.stereotype.Component("fixture.eventsource.PacedEvents")
public class PacedEvents implements org.ikasan.component.endpoint.consumer.api.spec.EndpointEventProvider<String> {
 private int count; private String pending;
 public String getEvent(){try { Thread.sleep(1000); } catch(InterruptedException e){Thread.currentThread().interrupt();return null;} if(pending!=null){String p=pending;pending=null;return p;}return "EVENT-"+(++count);}
 public void rollback(){pending="EVENT-"+count;}
}
''')
jms={'destinationJndiName':'migration.orders','connectionFactoryName':'ConnectionFactory','connectionFactoryJndiPropertyFactoryInitial':'org.apache.activemq.jndi.ActiveMQInitialContextFactory','connectionFactoryJndiPropertyProviderUrl':'vm://migration-fixture?create=true&broker.persistent=false&broker.useJmx=false','sessionTransacted':True}
begin('JMS Sender');timer()
custom('Converter','Make JMS Order','org.ikasan.spec.component.transformation.Converter<org.quartz.JobExecutionContext,fixture.Order>','private int count; public fixture.Order convert(org.quartz.JobExecutionContext c){return new fixture.Order("J"+(++count),1,false);}',{'fromType':'org.quartz.JobExecutionContext','toType':order})
add('Spring JMS Producer','Send Order',jms)
begin('JMS Receiver');add('Spring JMS Consumer','Receive Order',{**jms,'autoContentConversion':False,'trustedObjectPackages':'fixture,java.lang,java.util'})
custom('Converter','Require Object Message','org.ikasan.spec.component.transformation.Converter<org.ikasan.spec.flow.FlowEvent,Object>','public Object convert(org.ikasan.spec.flow.FlowEvent e){ Object p=e.getPayload(); if(!p.getClass().getSimpleName().contains("ObjectMessage"))throw new IllegalArgumentException("Expected ObjectMessage"); return p; }',{'fromType':'org.ikasan.spec.flow.FlowEvent','toType':'javax.jms.ObjectMessage'})
add('JMS Object Message To Object Converter','Decode Object')
custom('Converter','Require Order','org.ikasan.spec.component.transformation.Converter<org.ikasan.spec.flow.FlowEvent,fixture.Order>','public fixture.Order convert(org.ikasan.spec.flow.FlowEvent e){return (fixture.Order)e.getPayload();}',{'fromType':'org.ikasan.spec.flow.FlowEvent','toType':order})
add('Object To XML String Converter','Encode XML',{'objectClass':order,'rootName':'order','rootClassName':order});add('Logging Producer','Log XML')
for transport,port in [('FTP',2121),('SFTP',2222)]:
 common={'remoteHost':'127.0.0.1','remotePort':port,'username':'fixture','password':'fixture'}
 if transport=='SFTP': common.update(privateKeyFilename='${fixture.sftp.key:./services/id_rsa}',knownHostFilename='${fixture.sftp.knownHosts:./services/known_hosts}')
 begin(transport+' Sender');timer();text_source(transport+' migration payload')
 add('Converter','Encode File',{'conversionRecipeId':'string-to-file-transfer-payload'})
 add(transport+' Producer','Send File',{**common,'outputDirectory':'/upload','overwrite':True,'createParentDirectory':False})
 begin(transport+' Receiver');add(transport+' Consumer','Receive File',{**common,'sourceDirectory':'/upload','cronExpression':'0/2 * * * * ?','minAge':0,'destructive':False,'filenamePattern':'.*[.]dat','clientID':'migration-'+transport.lower()})
 add('Converter','Decode File',{'conversionRecipeId':'file-transfer-payload-to-string'});add('Logging Producer','Log File')
begin('Local Files');add('Local File Consumer','Read Lines',{'filenames':'./inputs/.*[.]txt','cronExpression':'0/2 * * * * ?'})
custom('Converter','Read File Contents','org.ikasan.spec.component.transformation.Converter<java.util.List,String>','public String convert(java.util.List paths) { try { var text=new StringBuilder();for(Object p:paths) text.append(java.nio.file.Files.readString(java.nio.file.Path.of(p.toString())));return text.toString(); }catch(java.io.IOException e){throw new org.ikasan.spec.component.transformation.TransformationException(e);} }',{'fromType':'java.util.List','toType':'java.lang.String'})
add('Logging Producer','Log Line')
begin('Mail');timer();text_source('Migration email body')
custom('Email Converter','Build Mail','org.ikasan.spec.component.transformation.Converter<String,org.ikasan.component.endpoint.email.producer.EmailPayload>','public org.ikasan.component.endpoint.email.producer.EmailPayload convert(String text) { var p=(org.ikasan.component.endpoint.email.producer.DefaultEmailPayload)org.ikasan.component.endpoint.email.producer.EmailPayload.newInstance(); p.setEmailBody(text);return p; }',{'fromType':'java.lang.String'})
add('Email Producer','Send Mail',{'toRecipient':'recipient@example.test','from':'fixture@example.test','mailSubject':'Migration fixture','mailhost':'127.0.0.1','mailSmtpHost':'127.0.0.1','mailSmtpPort':2525})
write(ROOT/'operations.json',json.dumps(batches,indent=2)+'\n');write(ROOT/'coverage.json',json.dumps(coverage,indent=2)+'\n')
write(USER/'fixture/Order.java','''package fixture;
public class Order implements java.io.Serializable {
 private static final long serialVersionUID=1L;
 private String id,stage="new"; private int quantity;private boolean priority;
 public Order(){} public Order(String id,int quantity,boolean priority){this.id=id;this.quantity=quantity;this.priority=priority;}
 public String getId(){return id;} public void setId(String v){id=v;} public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
 public boolean isPriority(){return priority;} public void setPriority(boolean v){priority=v;} public String getStage(){return stage;}public void setStage(String v){stage=v;}
 public String toString(){return id+"|"+quantity+"|"+priority+"|"+stage;}
}
''')
