package com.storex.saga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orchestrator_orders")
public class OrchestratorOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private Double originalAmount;
    private Double discountAmount;
    private Double finalAmount;
    private String voucherCode;
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    private OrchestratorOrderStatus status;

    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrchestratorOrder() {
    }

    public OrchestratorOrder(Long customerId, Double originalAmount, String voucherCode, String shippingAddress) {
        this.customerId = customerId;
        this.originalAmount = originalAmount;
        this.discountAmount = 0.0;
        this.finalAmount = originalAmount;
        this.voucherCode = voucherCode;
        this.shippingAddress = shippingAddress;
        this.status = OrchestratorOrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Double originalAmount) { this.originalAmount = originalAmount; }
    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }
    public Double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Double finalAmount) { this.finalAmount = finalAmount; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public OrchestratorOrderStatus getStatus() { return status; }
    public void setStatus(OrchestratorOrderStatus status) { this.status = status; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
