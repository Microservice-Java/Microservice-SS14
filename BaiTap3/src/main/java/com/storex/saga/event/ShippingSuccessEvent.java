package com.storex.saga.event;

public class ShippingSuccessEvent {
    private Long orderId;
    private String trackingNumber;

    public ShippingSuccessEvent() {
    }

    public ShippingSuccessEvent(Long orderId, String trackingNumber) {
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
}
