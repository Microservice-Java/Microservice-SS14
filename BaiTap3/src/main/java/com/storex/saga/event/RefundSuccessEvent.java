package com.storex.saga.event;

public class RefundSuccessEvent {
    private Long orderId;
    private Double amountRefunded;
    private String reason;

    public RefundSuccessEvent() {
    }

    public RefundSuccessEvent(Long orderId, Double amountRefunded, String reason) {
        this.orderId = orderId;
        this.amountRefunded = amountRefunded;
        this.reason = reason;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Double getAmountRefunded() { return amountRefunded; }
    public void setAmountRefunded(Double amountRefunded) { this.amountRefunded = amountRefunded; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
