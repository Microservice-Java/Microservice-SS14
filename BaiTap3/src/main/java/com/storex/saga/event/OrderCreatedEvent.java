package com.storex.saga.event;

public class OrderCreatedEvent {
    private Long orderId;
    private Long customerId;
    private Double amount;
    private String shippingAddress;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Long orderId, Long customerId, Double amount, String shippingAddress) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.shippingAddress = shippingAddress;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}
