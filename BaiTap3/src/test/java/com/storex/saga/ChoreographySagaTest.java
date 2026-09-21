package com.storex.saga;

import com.storex.saga.model.PaymentRecord;
import com.storex.saga.model.SagaOrder;
import com.storex.saga.model.SagaOrderStatus;
import com.storex.saga.model.ShipmentRecord;
import com.storex.saga.repository.PaymentRepository;
import com.storex.saga.repository.SagaOrderRepository;
import com.storex.saga.repository.ShipmentRepository;
import com.storex.saga.service.OrderSagaService;
import com.storex.saga.service.ShippingSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChoreographySagaTest {

    @Autowired
    private OrderSagaService orderSagaService;

    @Autowired
    private ShippingSagaService shippingSagaService;

    @Autowired
    private SagaOrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        shipmentRepository.deleteAll();
        shippingSagaService.setSimulateTimeout(false);
    }

    @Test
    @DisplayName("Kịch bản 1: Luồng thành công (Happy Path) -> Order -> Payment -> Shipping -> COMPLETED")
    void testHappyPath_OrderToPaymentToShipping_Completed() {
        // Act
        SagaOrder order = orderSagaService.createOrder(1001L, 250000.0, "123 Nguyen Hue, Quan 1, TP.HCM");

        // Assert
        SagaOrder finalOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(SagaOrderStatus.COMPLETED, finalOrder.getStatus());
        assertNotNull(finalOrder.getTrackingNumber());
        assertTrue(finalOrder.getTrackingNumber().startsWith("TRK-"));

        PaymentRecord paymentRecord = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("SUCCESS", paymentRecord.getStatus());
        assertEquals(250000.0, paymentRecord.getAmount());

        ShipmentRecord shipmentRecord = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("SUCCESS", shipmentRecord.getStatus());
        assertEquals(finalOrder.getTrackingNumber(), shipmentRecord.getTrackingNumber());
    }

    @Test
    @DisplayName("Kịch bản 2: Giao hàng thất bại (Địa chỉ unsupported) -> Bù trừ Hoàn tiền & Hủy đơn")
    void testShippingFailedCompensatingPath_RefundAndCanceled() {
        // Act: Đặt hàng với địa chỉ không hỗ trợ
        SagaOrder order = orderSagaService.createOrder(1002L, 500000.0, "Zone 99 Unsupported Island");

        // Assert
        SagaOrder finalOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(SagaOrderStatus.CANCELED, finalOrder.getStatus());
        assertNull(finalOrder.getTrackingNumber());

        PaymentRecord paymentRecord = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("REFUNDED", paymentRecord.getStatus(), "Payment phải được chuyển sang REFUNDED");

        ShipmentRecord shipmentRecord = shipmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("FAILED", shipmentRecord.getStatus());
    }

    @Test
    @DisplayName("Kịch bản 3: Shipping bị Timeout (>30s) -> Tự động kích hoạt Bù trừ Hoàn tiền & Hủy đơn")
    void testShippingTimeoutCompensatingPath_AutoRefundAndCanceled() {
        // Arrange: Giả lập Shipping Service bị treo/timeout
        shippingSagaService.setSimulateTimeout(true);

        SagaOrder order = orderSagaService.createOrder(1003L, 300000.0, "456 Le Loi, Quan 1, TP.HCM");

        // Đơn hàng hiện tại đã thanh toán thành công nhưng chưa nhận được Shipping response (đang đứng ở PAYMENT_SUCCESS)
        SagaOrder pendingOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(SagaOrderStatus.PAYMENT_SUCCESS, pendingOrder.getStatus());

        // Giả lập thời gian trôi qua 35 giây (quá thời hạn 30s)
        pendingOrder.setUpdatedAt(LocalDateTime.now().minusSeconds(35));
        orderRepository.save(pendingOrder);

        // Act: Kích hoạt job kiểm tra timeout
        orderSagaService.checkShippingTimeout(30);

        // Assert: Đơn hàng tự động hủy và hoàn tiền
        SagaOrder finalOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(SagaOrderStatus.CANCELED, finalOrder.getStatus());

        PaymentRecord paymentRecord = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("REFUNDED", paymentRecord.getStatus());
    }
}
