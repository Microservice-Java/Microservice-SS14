package com.storex.saga.service;

import com.storex.saga.model.OrchestratorOrder;
import com.storex.saga.model.OrchestratorOrderStatus;
import com.storex.saga.repository.OrchestratorOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrchestratorOrderRepository orderRepository;

    public OrderService(OrchestratorOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrchestratorOrder createPendingOrder(Long customerId, Double originalAmount, String voucherCode, String shippingAddress) {
        OrchestratorOrder order = new OrchestratorOrder(customerId, originalAmount, voucherCode, shippingAddress);
        order = orderRepository.save(order);
        log.info("OrderService -> Order ID {} created in PENDING status. Amount: {}", order.getId(), originalAmount);
        return order;
    }

    @Transactional
    public void updateOrderDiscount(Long orderId, Double discountAmount) {
        OrchestratorOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(Math.max(0.0, order.getOriginalAmount() - discountAmount));
        order.setStatus(OrchestratorOrderStatus.VOUCHER_APPLIED);
        orderRepository.save(order);
        log.info("OrderService -> Order ID {} updated with discount {}. Final Amount: {}",
                orderId, discountAmount, order.getFinalAmount());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrchestratorOrderStatus status, String trackingNumber) {
        OrchestratorOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(status);
        if (trackingNumber != null) {
            order.setTrackingNumber(trackingNumber);
        }
        orderRepository.save(order);
        log.info("OrderService -> Order ID {} updated to status {}", orderId, status);
    }
}
