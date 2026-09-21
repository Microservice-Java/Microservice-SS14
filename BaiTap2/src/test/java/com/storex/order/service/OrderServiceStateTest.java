package com.storex.order.service;

import com.storex.order.client.InventoryClient;
import com.storex.order.client.PaymentClient;
import com.storex.order.model.Order;
import com.storex.order.model.OrderStatus;
import com.storex.order.model.PaymentResponseEvent;
import com.storex.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderServiceStateTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        when(inventoryClient.increaseStock(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @DisplayName("Kịch bản 1: Nhận Event SUCCESS từ Payment -> Cập nhật trạng thái thành PAID")
    void testPaymentEventSuccess_UpdatesOrderStatusToPaid() {
        // Arrange
        Order order = orderService.createOrder(101L, 2);
        PaymentResponseEvent event = new PaymentResponseEvent(order.getId(), "SUCCESS", "Payment processed");

        // Act
        orderService.processPaymentResponse(event);

        // Assert
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
        verify(inventoryClient, never()).increaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Kịch bản 2: Nhận Event FAILED/REJECTED -> Cập nhật CANCELED và Hoàn kho")
    void testPaymentEventFailed_UpdatesOrderStatusToCanceled_AndTriggersInventoryRollback() {
        // Arrange
        Order order = orderService.createOrder(102L, 5);
        PaymentResponseEvent event = new PaymentResponseEvent(order.getId(), "REJECTED", "Insufficient balance");

        // Act
        orderService.processPaymentResponse(event);

        // Assert
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELED, updatedOrder.getStatus());
        verify(inventoryClient, times(1)).increaseStock(102L, 5);
    }

    @Test
    @DisplayName("Kịch bản 3: Đảm bảo tính Idempotency -> Bỏ qua Event trùng lặp khi đơn đã PAID")
    void testIdempotency_DuplicatePaymentEvent_Ignored() {
        // Arrange
        Order order = orderService.createOrder(103L, 1);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        PaymentResponseEvent event = new PaymentResponseEvent(order.getId(), "FAILED", "Late error event");

        // Act
        orderService.processPaymentResponse(event);

        // Assert
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus(), "Trạng thái PAID phải được giữ nguyên");
        verify(inventoryClient, never()).increaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Kịch bản 4: Timeout Reconcile -> Đối soát Payment-Service báo SUCCESS -> Cập nhật PAID")
    void testReconcilePendingOrders_PaymentServicePaid_UpdatesStatusToPaid() {
        // Arrange
        Order order = orderService.createOrder(104L, 3);
        // Giả lập đơn hàng được tạo 10 phút trước (quá thời hạn 5 phút)
        order.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        orderRepository.save(order);

        when(paymentClient.checkPaymentStatus(order.getId())).thenReturn("SUCCESS");

        // Act
        orderService.reconcilePendingOrders(5);

        // Assert
        Order reconciledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, reconciledOrder.getStatus());
        verify(inventoryClient, never()).increaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Kịch bản 5: Timeout Reconcile -> Đối soát Payment-Service báo FAILED/NOT_FOUND -> Hủy đơn và Hoàn kho")
    void testReconcilePendingOrders_PaymentServiceFailed_CancelsOrderAndRollsBackInventory() {
        // Arrange
        Order order = orderService.createOrder(105L, 4);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        orderRepository.save(order);

        when(paymentClient.checkPaymentStatus(order.getId())).thenReturn("NOT_FOUND");

        // Act
        orderService.reconcilePendingOrders(5);

        // Assert
        Order reconciledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELED, reconciledOrder.getStatus());
        verify(inventoryClient, times(1)).increaseStock(105L, 4);
    }
}
