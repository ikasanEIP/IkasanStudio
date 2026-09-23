package fixture.exclusion;
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
