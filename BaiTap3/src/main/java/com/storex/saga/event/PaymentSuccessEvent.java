package com.storex.saga.event;

public class PaymentSuccessEvent {
    private Long orderId;
    private Long paymentId;
    private Double amount;
    private String shippingAddress;

    public PaymentSuccessEvent() {
    }

    public PaymentSuccessEvent(Long orderId, Long paymentId, Double amount, String shippingAddress) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.shippingAddress = shippingAddress;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}
