package com.storex.order.client;

public interface PaymentClient {

    /**
     * Active query API (Reconcile API) to query payment status directly from Payment-Service
     * when events are lost or delayed.
     * 
     * @return "SUCCESS", "FAILED", "NOT_FOUND", or "PENDING"
     */
    String checkPaymentStatus(Long orderId);
}
