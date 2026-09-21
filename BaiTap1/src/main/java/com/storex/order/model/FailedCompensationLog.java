package com.storex.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_compensation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedCompensationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;
    private Integer quantity;
    private String reason;
    private String status; // PENDING_RETRY, RESOLVED, FAILED_PERMANENTLY
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastRetryAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (status == null) {
            status = "PENDING_RETRY";
        }
    }
}
