package com.storex.saga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_orders")
public class SagaOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private Double amount;
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    private SagaOrderStatus status;

    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SagaOrder() {
    }

    public SagaOrder(Long customerId, Double amount, String shippingAddress, SagaOrderStatus status) {
        this.customerId = customerId;
        this.amount = amount;
        this.shippingAddress = shippingAddress;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public SagaOrderStatus getStatus() { return status; }
    public void setStatus(SagaOrderStatus status) { this.status = status; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
