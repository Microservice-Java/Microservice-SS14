package com.storex.saga.model;

public enum SagaOrderStatus {
    PENDING,
    PAYMENT_SUCCESS,
    COMPLETED,
    COMPENSATING,
    CANCELED
}
