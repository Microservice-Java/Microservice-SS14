package com.storex.saga;

import com.storex.saga.model.*;
import com.storex.saga.orchestrator.OrderSagaOrchestrator;
import com.storex.saga.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrchestratorSagaTest {

    @Autowired
    private OrderSagaOrchestrator orchestrator;

    @Autowired
    private OrchestratorOrderRepository orderRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private OrchestratorPaymentRepository paymentRepository;

    @Autowired
    private OrchestratorShipmentRepository shipmentRepository;

    @Autowired
    private SagaStateDataRepository sagaStateRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        shipmentRepository.deleteAll();
        sagaStateRepository.deleteAll();
        voucherRepository.deleteAll();

        // Seed initial Vouchers
        voucherRepository.save(new Voucher("SALE50K", 50000.0, 100000.0, 5));
        voucherRepository.save(new Voucher("HUGE100K", 100000.0, 500000.0, 1)); // Min 500K required
    }

    @Test
    @DisplayName("Kịch bản 1: Happy Path -> Order -> Voucher -> Payment -> Shipping -> COMPLETED")
    void testHappyPath_OrderVoucherPaymentShipping_Completed() {
        // Act
        OrchestratorOrder order = orchestrator.executeOrderSaga(2001L, 250000.0, "SALE50K", "123 Nguyen Hue, TP.HCM");

        // Assert
        OrchestratorOrder finalOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrchestratorOrderStatus.COMPLETED, finalOrder.getStatus());
        assertEquals(250000.0, finalOrder.getOriginalAmount());
        assertEquals(50000.0, finalOrder.getDiscountAmount());
        assertEquals(200000.0, finalOrder.getFinalAmount());
        assertNotNull(finalOrder.getTrackingNumber());

        Voucher voucher = voucherRepository.findByCode("SALE50K").orElseThrow();
        assertEquals(1, voucher.getUsedCount());

        OrchestratorPayment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(200000.0, payment.getAmount());

        OrchestratorShipment shipment = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("SUCCESS", shipment.getStatus());

        SagaStateData sagaState = sagaStateRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("SHIPPING_COMPLETED", sagaState.getCurrentStep());
    }

    @Test
    @DisplayName("Kịch bản 2: Voucher lỗi (Không đạt giá trị tối thiểu) -> Hủy đơn hàng (No Compensation needed)")
    void testVoucherFailure_RollsBackOrderToCanceled() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orchestrator.executeOrderSaga(2002L, 200000.0, "HUGE100K", "123 Nguyen Hue, TP.HCM"); // Min 500K required
        });

        // Check Order Status
        OrchestratorOrder order = orderRepository.findAll().get(0);
        assertEquals(OrchestratorOrderStatus.CANCELED, order.getStatus());

        Voucher voucher = voucherRepository.findByCode("HUGE100K").orElseThrow();
        assertEquals(0, voucher.getUsedCount());
    }

    @Test
    @DisplayName("Kịch bản 3: Shipping lỗi (Địa chỉ UNSUPPORTED) -> Refund Payment + Release Voucher + Cancel Order")
    void testShippingFailure_CompensatesPaymentAndVoucherAndCancelsOrder() {
        // Act & Assert: Địa chỉ giao hàng unsupported
        assertThrows(RuntimeException.class, () -> {
            orchestrator.executeOrderSaga(2003L, 300000.0, "SALE50K", "UNSUPPORTED_ISLAND_ADDRESS");
        });

        OrchestratorOrder order = orderRepository.findAll().get(0);
        assertEquals(OrchestratorOrderStatus.CANCELED, order.getStatus());

        // Check Payment Compensated
        OrchestratorPayment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("REFUNDED", payment.getStatus(), "Payment phải được REFUNDED");

        // Check Voucher Compensated
        Voucher voucher = voucherRepository.findByCode("SALE50K").orElseThrow();
        assertEquals(0, voucher.getUsedCount(), "Voucher usage count phải được trả lại 0");

        SagaStateData sagaState = sagaStateRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("FAILED", sagaState.getCurrentStep());
    }
}
