package com.storex.order.service;

import com.storex.order.client.InventoryClient;
import com.storex.order.client.PaymentClient;
import com.storex.order.model.Order;
import com.storex.order.model.OrderStatus;
import com.storex.order.model.PaymentResponseEvent;
import com.storex.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    public Order createOrder(Long productId, Integer quantity) {
        Order order = new Order(productId, quantity, OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @Transactional
    public void processPaymentResponse(PaymentResponseEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid PaymentResponseEvent");
            return;
        }

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + event.getOrderId()));

        // Idempotency check: if order is already processed, ignore late duplicate events
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order ID {} already in terminal status {}, skipping payment event status {}",
                    order.getId(), order.getStatus(), event.getStatus());
            return;
        }

        String eventStatus = event.getStatus() != null ? event.getStatus().toUpperCase() : "UNKNOWN";

        switch (eventStatus) {
            case "SUCCESS":
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Order ID {} payment SUCCESS -> Updated status to PAID", order.getId());
                break;

            case "REJECTED":
            case "FAILED":
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                log.info("Order ID {} payment {} -> Updated status to CANCELED", order.getId(), eventStatus);
                
                // Compensating action: Release stock back to inventory
                rollbackInventory(order);
                break;

            default:
                log.warn("Order ID {} received unknown payment status '{}' -> Retaining PENDING status", order.getId(), eventStatus);
                break;
        }
    }

    @Transactional
    public void reconcilePendingOrders(int expirationMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(expirationMinutes);
        List<Order> expiredPendingOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);

        if (expiredPendingOrders.isEmpty()) {
            log.debug("No expired PENDING orders found for reconciliation.");
            return;
        }

        log.info("Found {} expired PENDING orders to reconcile (Threshold: before {})",
                expiredPendingOrders.size(), threshold);

        for (Order order : expiredPendingOrders) {
            reconcileSingleOrder(order);
        }
    }

    private void reconcileSingleOrder(Order order) {
        try {
            log.info("Querying Payment-Service for Order ID {} status...", order.getId());
            String paymentStatus = paymentClient.checkPaymentStatus(order.getId());

            if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Reconciliation: Order ID {} synced to PAID based on Payment-Service state", order.getId());
            } else {
                // FAILED, REJECTED, NOT_FOUND, or NULL
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                log.info("Reconciliation: Order ID {} status '{}' -> CANCELED", order.getId(), paymentStatus);
                rollbackInventory(order);
            }
        } catch (Exception e) {
            log.error("Failed to query Payment-Service for Order ID {}: {}", order.getId(), e.getMessage());
        }
    }

    private void rollbackInventory(Order order) {
        try {
            boolean rollbacked = inventoryClient.increaseStock(order.getProductId(), order.getQuantity());
            if (rollbacked) {
                log.info("Compensating action: Restored stock {} for Product ID {} (Order ID {})",
                        order.getQuantity(), order.getProductId(), order.getId());
            } else {
                log.error("Compensating action failed for Order ID {}", order.getId());
            }
        } catch (Exception e) {
            log.error("Error invoking InventoryClient to restore stock for Order ID {}: {}",
                    order.getId(), e.getMessage());
        }
    }
}
