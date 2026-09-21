package com.storex.saga.service;

import com.storex.saga.bus.ChoreographyEventPublisher;
import com.storex.saga.event.*;
import com.storex.saga.model.PaymentRecord;
import com.storex.saga.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSagaService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaService.class);

    private final PaymentRepository paymentRepository;
    private final ChoreographyEventPublisher eventPublisher;

    public PaymentSagaService(PaymentRepository paymentRepository,
                              ChoreographyEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("PaymentService -> Consumed OrderCreatedEvent for Order ID {}. Processing payment amount: {}",
                event.getOrderId(), event.getAmount());

        // Simulate payment logic: Amounts <= 0 fail payment
        if (event.getAmount() <= 0) {
            log.warn("PaymentService -> Invalid payment amount {}. Emitting PaymentFailedEvent...", event.getAmount());
            eventPublisher.publish(new PaymentFailedEvent(event.getOrderId(), "Invalid amount: " + event.getAmount()));
            return;
        }

        PaymentRecord record = new PaymentRecord(event.getOrderId(), event.getAmount(), "SUCCESS");
        paymentRepository.save(record);

        log.info("PaymentService -> Payment SUCCESS for Order ID {}. Emitting PaymentSuccessEvent...", event.getOrderId());
        eventPublisher.publish(new PaymentSuccessEvent(
                event.getOrderId(), record.getId(), event.getAmount(), event.getShippingAddress()
        ));
    }

    @EventListener
    @Transactional
    public void onCompensatePayment(CompensatePaymentEvent event) {
        log.info("PaymentService -> Consumed CompensatePaymentEvent for Order ID {}. Processing Refund...",
                event.getOrderId());

        paymentRepository.findByOrderId(event.getOrderId()).ifPresentOrElse(record -> {
            record.setStatus("REFUNDED");
            paymentRepository.save(record);
            log.info("PaymentService -> Refund completed for Order ID {}. Emitting RefundSuccessEvent...", event.getOrderId());
            eventPublisher.publish(new RefundSuccessEvent(event.getOrderId(), event.getAmount(), event.getReason()));
        }, () -> {
            log.warn("PaymentService -> No payment record found to refund for Order ID {}. Emitting RefundSuccessEvent anyway.", event.getOrderId());
            eventPublisher.publish(new RefundSuccessEvent(event.getOrderId(), event.getAmount(), "No prior payment record"));
        });
    }
}
