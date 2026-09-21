package com.storex.order.scheduler;

import com.storex.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final OrderService orderService;
    private final int expirationMinutes;

    public OrderTimeoutScheduler(OrderService orderService,
                                 @Value("${order.timeout.expiration-minutes:5}") int expirationMinutes) {
        this.orderService = orderService;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Scheduled job to check and resolve expired PENDING orders.
     * Runs every 1 minute.
     */
    @Scheduled(fixedDelayString = "${order.timeout.check-interval-ms:60000}")
    public void schedulePendingOrderReconciliation() {
        log.info("Starting scheduled reconciliation for PENDING orders older than {} minutes", expirationMinutes);
        orderService.reconcilePendingOrders(expirationMinutes);
    }
}
