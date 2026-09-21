package com.storex.saga.event;

public class CompensatePaymentEvent {
    private Long orderId;
    private Double amount;
    private String reason;

    public CompensatePaymentEvent() {
    }

    public CompensatePaymentEvent(Long orderId, Double amount, String reason) {
        this.orderId = orderId;
        this.amount = amount;
        this.reason = reason;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
