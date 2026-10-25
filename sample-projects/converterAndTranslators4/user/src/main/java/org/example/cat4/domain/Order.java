package org.example.cat4.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "order")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"reference", "quantity", "orderNumber"})
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public String reference;
    public int orderNumber;
    public int quantity;

    public Order() {}

    @Override
    public String toString() {
        return "Order{" +
                "reference='" + reference + '\'' +
                ", quantity=" + quantity +
                ", orderNumber=" + orderNumber +
                '}';
    }

    public Order(String reference, int orderNumber, int quantity) {
        this.reference = reference;
        this.orderNumber = orderNumber;
        this.quantity = quantity;
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

    public Order(String reference, int quantity) {
        this.reference = reference;
        this.quantity = quantity;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }
}


