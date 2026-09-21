package com.storex.order.client;

public interface InventoryClient {
    boolean decreaseStock(String productId, Integer quantity);
    boolean increaseStock(String productId, Integer quantity);
}
