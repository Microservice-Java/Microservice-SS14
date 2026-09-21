package com.storex.saga.model;

public enum OrchestratorOrderStatus {
    PENDING,
    VOUCHER_APPLIED,
    PAID,
    COMPLETED,
    CANCELED,
    FAILED
}
