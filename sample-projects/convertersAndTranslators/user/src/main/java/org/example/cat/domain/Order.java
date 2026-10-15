package org.example.cat.domain;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlRootElement(name = "order")
@XmlAccessorType(XmlAccessType.FIELD)
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public String reference;
    public int quantity;
    public int orderNumber;

    public Order(String reference, int orderNumber, int quantity) {
        this.reference = reference;
        this.orderNumber = orderNumber;
        this.quantity = quantity;
    }

    public Order() {}

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return quantity == order.quantity && orderNumber == order.orderNumber && Objects.equals(reference, order.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, quantity, orderNumber);
    }

    @Override
    public String toString() {
        return "Order{" +
                "reference='" + reference + '\'' +
                ", quantity=" + quantity +
                ", orderNumber=" + orderNumber +
                '}';
    }
}
