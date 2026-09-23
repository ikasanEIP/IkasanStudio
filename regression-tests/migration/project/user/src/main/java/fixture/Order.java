package fixture;
public class Order implements java.io.Serializable {
 private static final long serialVersionUID=1L;
 private String id,stage="new"; private int quantity;private boolean priority;
 public Order(){} public Order(String id,int quantity,boolean priority){this.id=id;this.quantity=quantity;this.priority=priority;}
 public String getId(){return id;} public void setId(String v){id=v;} public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
 public boolean isPriority(){return priority;} public void setPriority(boolean v){priority=v;} public String getStage(){return stage;}public void setStage(String v){stage=v;}
 public String toString(){return id+"|"+quantity+"|"+priority+"|"+stage;}
}
