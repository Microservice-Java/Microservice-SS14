package com.storex.saga.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_state_data")
public class SagaStateData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String currentStep; // "ORDER_CREATED", "VOUCHER_APPLIED", "PAYMENT_COMPLETED", "SHIPPING_COMPLETED", "FAILED"
    private String lastError;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SagaStateData() {
    }

    public SagaStateData(Long orderId, String currentStep) {
        this.orderId = orderId;
        this.currentStep = currentStep;
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
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
