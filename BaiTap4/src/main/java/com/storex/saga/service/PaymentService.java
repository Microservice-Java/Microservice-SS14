package com.storex.saga.service;

import com.storex.saga.model.OrchestratorPayment;
import com.storex.saga.repository.OrchestratorPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrchestratorPaymentRepository paymentRepository;

    public PaymentService(OrchestratorPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public OrchestratorPayment processPayment(Long orderId, Double amount) {
        log.info("PaymentService -> Processing payment for Order ID {}: amount = {}", orderId, amount);

        if (amount < 0) {
            throw new IllegalArgumentException("Invalid payment amount: " + amount);
        }

        OrchestratorPayment payment = new OrchestratorPayment(orderId, amount, "SUCCESS");
        paymentRepository.save(payment);
        log.info("PaymentService -> Payment SUCCESS for Order ID {}", orderId);
        return payment;
    }

    @Transactional
    public void refundPayment(Long orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
            log.info("Compensating Action: Payment for Order ID {} REFUNDED successfully!", orderId);
        });
    }
}
