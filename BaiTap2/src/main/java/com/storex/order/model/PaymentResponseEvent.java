package com.storex.order.model;

public class PaymentResponseEvent {

    private Long orderId;
    private String status; // "SUCCESS", "REJECTED", "FAILED"
    private String reason;

    public PaymentResponseEvent() {
    }

    public PaymentResponseEvent(Long orderId, String status, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
