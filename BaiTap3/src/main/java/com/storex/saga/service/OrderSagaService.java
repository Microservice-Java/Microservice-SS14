package com.storex.saga.service;

import com.storex.saga.bus.ChoreographyEventPublisher;
import com.storex.saga.event.*;
import com.storex.saga.model.SagaOrder;
import com.storex.saga.model.SagaOrderStatus;
import com.storex.saga.repository.SagaOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderSagaService {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);

    private final SagaOrderRepository orderRepository;
    private final ChoreographyEventPublisher eventPublisher;

    public OrderSagaService(SagaOrderRepository orderRepository,
                            ChoreographyEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SagaOrder createOrder(Long customerId, Double amount, String shippingAddress) {
        SagaOrder order = new SagaOrder(customerId, amount, shippingAddress, SagaOrderStatus.PENDING);
        order = orderRepository.save(order);

        log.info("OrderService -> Created Order ID {} [PENDING]. Emitting OrderCreatedEvent...", order.getId());
        eventPublisher.publish(new OrderCreatedEvent(order.getId(), customerId, amount, shippingAddress));
        return order;
    }

    @EventListener
    @Transactional
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("OrderService -> Received PaymentSuccessEvent for Order ID {}", event.getOrderId());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(SagaOrderStatus.PAYMENT_SUCCESS);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });
    }

    @EventListener
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("OrderService -> Received PaymentFailedEvent for Order ID {}: {}", event.getOrderId(), event.getReason());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(SagaOrderStatus.CANCELED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });
    }

    @EventListener
    @Transactional
    public void onShippingSuccess(ShippingSuccessEvent event) {
        log.info("OrderService -> Received ShippingSuccessEvent for Order ID {}. Tracking: {}",
                event.getOrderId(), event.getTrackingNumber());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(SagaOrderStatus.COMPLETED);
            order.setTrackingNumber(event.getTrackingNumber());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });
    }

    @EventListener
    @Transactional
    public void onShippingFailed(ShippingFailedEvent event) {
        log.error("OrderService -> Received ShippingFailedEvent for Order ID {}: {}. Initiating Compensation...",
                event.getOrderId(), event.getReason());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(SagaOrderStatus.COMPENSATING);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Trigger compensating transaction: Compensate Payment
            eventPublisher.publish(new CompensatePaymentEvent(order.getId(), order.getAmount(), event.getReason()));
        });
    }

    @EventListener
    @Transactional
    public void onRefundSuccess(RefundSuccessEvent event) {
        log.info("OrderService -> Received RefundSuccessEvent for Order ID {}. Canceling Order...", event.getOrderId());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(SagaOrderStatus.CANCELED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });
    }

    @Transactional
    public void checkShippingTimeout(int timeoutSeconds) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(timeoutSeconds);
        List<SagaOrder> timedOutOrders = orderRepository.findByStatusAndUpdatedAtBefore(SagaOrderStatus.PAYMENT_SUCCESS, threshold);

        for (SagaOrder order : timedOutOrders) {
            log.warn("OrderService -> Shipping timeout detected for Order ID {} (No response after {}s). Triggering Auto-Compensation!",
                    order.getId(), timeoutSeconds);

            order.setStatus(SagaOrderStatus.COMPENSATING);
            orderRepository.save(order);

            eventPublisher.publish(new CompensatePaymentEvent(
                    order.getId(), order.getAmount(), "Shipping Service Timeout (> " + timeoutSeconds + "s)"
            ));
        }
    }
}
