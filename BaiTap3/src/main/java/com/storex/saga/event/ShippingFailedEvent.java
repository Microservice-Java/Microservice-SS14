package com.storex.saga.event;

public class ShippingFailedEvent {
    private Long orderId;
    private String reason;

    public ShippingFailedEvent() {
    }

    public ShippingFailedEvent(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
