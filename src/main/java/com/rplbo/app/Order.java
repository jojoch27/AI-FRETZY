package com.rplbo.app;

public class Order {
    private final int orderId;
    private final String orderNumber;
    private final String customerName;
    private final String productName;
    private final String status;
    private final String updatedAt;

    public Order(int orderId, String status) {
        this(orderId, "", "", "", status, "");
    }

    public Order(int orderId, String orderNumber, String customerName, String productName, String status, String updatedAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.productName = productName;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }

    public String getStatus() {
        return status;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String checkStatus() {
        return status;
    }
}
