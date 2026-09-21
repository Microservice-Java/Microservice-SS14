package com.storex.saga.orchestrator;

import com.storex.saga.model.*;
import com.storex.saga.repository.SagaStateDataRepository;
import com.storex.saga.service.OrderService;
import com.storex.saga.service.PaymentService;
import com.storex.saga.service.ShippingService;
import com.storex.saga.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderService orderService;
    private final VoucherService voucherService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final SagaStateDataRepository sagaStateRepository;

    public OrderSagaOrchestrator(OrderService orderService,
                                 VoucherService voucherService,
                                 PaymentService paymentService,
                                 ShippingService shippingService,
                                 SagaStateDataRepository sagaStateRepository) {
        this.orderService = orderService;
        this.voucherService = voucherService;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
        this.sagaStateRepository = sagaStateRepository;
    }

    public OrchestratorOrder executeOrderSaga(Long customerId, Double originalAmount, String voucherCode, String shippingAddress) {
        log.info("=== ORCHESTRATOR: Starting Saga execution for Customer ID {} ===", customerId);

        OrchestratorOrder order = null;
        SagaStateData sagaState = null;

        try {
            // STEP 1: Create Pending Order
            log.info("Orchestrator -> Step 1: Creating Order");
            order = orderService.createPendingOrder(customerId, originalAmount, voucherCode, shippingAddress);
            sagaState = new SagaStateData(order.getId(), "ORDER_CREATED");
            sagaStateRepository.save(sagaState);

            // STEP 2: Apply Voucher
            log.info("Orchestrator -> Step 2: Applying Voucher '{}'", voucherCode);
            Double discount = voucherService.applyVoucher(voucherCode, originalAmount);
            orderService.updateOrderDiscount(order.getId(), discount);
            sagaState.setCurrentStep("VOUCHER_APPLIED");
            sagaStateRepository.save(sagaState);

            // Fetch updated order with final amount
            Double finalAmount = Math.max(0.0, originalAmount - discount);

            // STEP 3: Process Payment
            log.info("Orchestrator -> Step 3: Processing Payment for Final Amount {}", finalAmount);
            paymentService.processPayment(order.getId(), finalAmount);
            orderService.updateOrderStatus(order.getId(), OrchestratorOrderStatus.PAID, null);
            sagaState.setCurrentStep("PAYMENT_COMPLETED");
            sagaStateRepository.save(sagaState);

            // STEP 4: Create Shipment
            log.info("Orchestrator -> Step 4: Creating Shipment to '{}'", shippingAddress);
            OrchestratorShipment shipment = shippingService.createShipment(order.getId(), shippingAddress);
            orderService.updateOrderStatus(order.getId(), OrchestratorOrderStatus.COMPLETED, shipment.getTrackingNumber());
            sagaState.setCurrentStep("SHIPPING_COMPLETED");
            sagaStateRepository.save(sagaState);

            log.info("=== ORCHESTRATOR: Saga execution SUCCESS for Order ID {} ===", order.getId());
            return order;

        } catch (Exception e) {
            log.error("=== ORCHESTRATOR: Saga execution FAILED at step '{}': {} ===",
                    sagaState != null ? sagaState.getCurrentStep() : "ORDER_INIT", e.getMessage());

            if (sagaState != null) {
                sagaState.setLastError(e.getMessage());
                sagaStateRepository.save(sagaState);
            }

            // Trigger Centralized Compensation
            rollbackSaga(order, sagaState, voucherCode);

            throw new RuntimeException("Saga Execution Failed: " + e.getMessage(), e);
        }
    }

    private void rollbackSaga(OrchestratorOrder order, SagaStateData sagaState, String voucherCode) {
        if (order == null || sagaState == null) {
            log.warn("Orchestrator Rollback -> Order or SagaState is null, nothing to compensate.");
            return;
        }

        String currentStep = sagaState.getCurrentStep();
        log.info("Orchestrator Rollback -> Initiating Compensation from last successful step '{}'...", currentStep);

        sagaState.setCurrentStep("COMPENSATING");
        sagaStateRepository.save(sagaState);

        // Reverse step execution
        switch (currentStep) {
            case "PAYMENT_COMPLETED":
                // Failed at Step 4 (Shipping) -> Refund Payment, Release Voucher, Cancel Order
                log.info("Compensating Step: Refund Payment for Order ID {}", order.getId());
                paymentService.refundPayment(order.getId());
                
                log.info("Compensating Step: Release Voucher '{}'", voucherCode);
                voucherService.releaseVoucher(voucherCode);
                
                orderService.updateOrderStatus(order.getId(), OrchestratorOrderStatus.CANCELED, null);
                break;

            case "VOUCHER_APPLIED":
                // Failed at Step 3 (Payment) -> Release Voucher, Cancel Order
                log.info("Compensating Step: Release Voucher '{}'", voucherCode);
                voucherService.releaseVoucher(voucherCode);
                
                orderService.updateOrderStatus(order.getId(), OrchestratorOrderStatus.CANCELED, null);
                break;

            case "ORDER_CREATED":
                // Failed at Step 2 (Voucher) -> Cancel Order
                log.info("Compensating Step: Cancel Order ID {}", order.getId());
                orderService.updateOrderStatus(order.getId(), OrchestratorOrderStatus.CANCELED, null);
                break;

            default:
                log.warn("Unknown step for compensation: {}", currentStep);
                break;
        }

        sagaState.setCurrentStep("FAILED");
        sagaStateRepository.save(sagaState);
        log.info("Orchestrator Rollback -> Compensation Completed for Order ID {}", order.getId());
    }
}
