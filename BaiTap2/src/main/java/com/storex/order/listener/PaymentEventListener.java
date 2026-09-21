package com.storex.order.listener;

import com.storex.order.model.PaymentResponseEvent;
import com.storex.order.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final OrderService orderService;

    public PaymentEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @EventListener
    public void handlePaymentResponse(PaymentResponseEvent event) {
        orderService.processPaymentResponse(event);
    }
}
