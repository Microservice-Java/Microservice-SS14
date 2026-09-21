package com.storex.order.service;

import com.storex.order.client.InventoryClient;
import com.storex.order.dto.OrderRequest;
import com.storex.order.model.FailedCompensationLog;
import com.storex.order.model.Order;
import com.storex.order.repository.FailedCompensationLogRepository;
import com.storex.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FailedCompensationLogRepository compensationLogRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Test 1: Tạo đơn hàng thành công khi Trừ kho & Lưu DB đều thuận lợi")
    void createOrder_Success() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .productId("PROD-001")
                .quantity(2)
                .build();

        Order savedOrder = Order.builder()
                .id(100L)
                .productId("PROD-001")
                .quantity(2)
                .status("PENDING")
                .build();

        when(inventoryClient.decreaseStock("PROD-001", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        ResponseEntity<Order> response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getId());

        verify(inventoryClient, times(1)).decreaseStock("PROD-001", 2);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(inventoryClient, never()).increaseStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Test 2: Từ chối tạo đơn khi Sản phẩm hết hàng trong kho")
    void createOrder_OutOfStock() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .productId("PROD-002")
                .quantity(5)
                .build();

        when(inventoryClient.decreaseStock("PROD-002", 5)).thenReturn(false);

        // Act
        ResponseEntity<Order> response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verify(inventoryClient, times(1)).decreaseStock("PROD-002", 5);
        verify(orderRepository, never()).save(any(Order.class));
        verify(inventoryClient, never()).increaseStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Test 3 (Bù Trừ Thành Công): Lưu DB đơn hàng bị lỗi -> Tự động gọi increaseStock để hoàn kho")
    void createOrder_SaveOrderFailed_CompensatedSuccessfully() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .productId("PROD-003")
                .quantity(3)
                .build();

        when(inventoryClient.decreaseStock("PROD-003", 3)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database timeout exception"));
        when(inventoryClient.increaseStock("PROD-003", 3)).thenReturn(true);

        // Act
        ResponseEntity<Order> response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        // Verify: Trừ kho được gọi -> DB save bị lỗi -> Bù trừ hoàn kho được gọi thành công!
        verify(inventoryClient, times(1)).decreaseStock("PROD-003", 3);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(inventoryClient, times(1)).increaseStock("PROD-003", 3);
        verify(compensationLogRepository, never()).save(any(FailedCompensationLog.class));
    }

    @Test
    @DisplayName("Test 4 (Lỗi Kép Double Failure): Lưu DB đơn hàng lỗi AND Hoàn kho lỗi -> Lưu log vào FailedCompensationLog cho Scheduled Job")
    void createOrder_DoubleFailure_LogsCompensationForBackgroundRetry() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .productId("PROD-004")
                .quantity(1)
                .build();

        when(inventoryClient.decreaseStock("PROD-004", 1)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB Connection Loss"));
        when(inventoryClient.increaseStock("PROD-004", 1)).thenThrow(new RuntimeException("Inventory Service Offline"));

        // Act
        ResponseEntity<Order> response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        // Verify: Trừ kho -> Save DB lỗi -> Hoàn kho lỗi -> Lưu bản ghi FailedCompensationLog với status PENDING_RETRY
        verify(inventoryClient, times(1)).decreaseStock("PROD-004", 1);
        verify(inventoryClient, times(1)).increaseStock("PROD-004", 1);

        ArgumentCaptor<FailedCompensationLog> logCaptor = ArgumentCaptor.forClass(FailedCompensationLog.class);
        verify(compensationLogRepository, times(1)).save(logCaptor.capture());

        FailedCompensationLog savedLog = logCaptor.getValue();
        assertEquals("PROD-004", savedLog.getProductId());
        assertEquals(1, savedLog.getQuantity());
        assertEquals("PENDING_RETRY", savedLog.getStatus());
    }

    @Test
    @DisplayName("Test 5 (Background Scheduled Job): Quét các log bù trừ lỗi và hoàn kho thành công -> Cập nhật status RESOLVED")
    void compensatingJobService_RetryPendingLogsSuccess() {
        // Arrange
        CompensatingJobService jobService = new CompensatingJobService(inventoryClient, compensationLogRepository);

        FailedCompensationLog pendingLog = FailedCompensationLog.builder()
                .id(1L)
                .productId("PROD-005")
                .quantity(4)
                .status("PENDING_RETRY")
                .retryCount(0)
                .build();

        when(compensationLogRepository.findByStatus("PENDING_RETRY")).thenReturn(List.of(pendingLog));
        when(inventoryClient.increaseStock("PROD-005", 4)).thenReturn(true);

        // Act
        jobService.retryFailedCompensations();

        // Assert
        verify(inventoryClient, times(1)).increaseStock("PROD-005", 4);
        verify(compensationLogRepository, times(1)).save(pendingLog);
        assertEquals("RESOLVED", pendingLog.getStatus());
    }
}
