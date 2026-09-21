package com.storex.order.client;

public interface InventoryClient {

    /**
     * Completes compensating transaction to return stock when order payment fails or times out.
     */
    boolean increaseStock(Long productId, Integer quantity);
}
