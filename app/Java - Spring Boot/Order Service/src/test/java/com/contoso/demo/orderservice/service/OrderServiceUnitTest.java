package com.contoso.demo.orderservice.service;

import com.contoso.demo.orderservice.model.Order;
import com.contoso.demo.orderservice.model.OrderStatus;
import com.contoso.demo.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void totalForCustomerReturnsZeroWhenNoOrders() {
        when(orderRepository.findByCustomer("nobody")).thenReturn(Collections.emptyList());

        assertEquals(BigDecimal.ZERO, orderService.totalForCustomer("nobody"));
    }

    @Test
    void totalForCustomerSumsAllAmounts() {
        when(orderRepository.findByCustomer("alice")).thenReturn(Arrays.asList(
                new Order("alice", new BigDecimal("120.50")),
                new Order("alice", new BigDecimal("80.00"))
        ));

        assertEquals(new BigDecimal("200.50"), orderService.totalForCustomer("alice"));
    }

    @Test
    void findByIdDelegatesToRepository() {
        Order order = new Order("bob", new BigDecimal("42.99"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("bob", result.get().getCustomer());
    }

    @Test
    void createPersistsOrderAndLogsOnlyNonSensitiveLifecycleData(CapturedOutput output) {
        Order order = new Order("carol", new BigDecimal("10.00"));
        order.setId(7L);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.save(order)).thenReturn(order);

        orderService.create(order);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderRepository).save(order);
        assertTrue(output.getOut().contains(
                "operation=create orderId=7 status=PENDING outcome=success"));
        assertFalse(output.getOut().contains("carol"));
        assertFalse(output.getOut().contains("10.00"));
    }

    @Test
    void updateStatusPersistsTheRequestedStatusAndLogsLifecycleData(CapturedOutput output) {
        Order order = new Order("dave", new BigDecimal("15.00"));
        order.setId(4L);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Optional<Order> result = orderService.updateStatus(4L, OrderStatus.PROCESSING);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.PROCESSING, result.get().getStatus());
        verify(orderRepository).save(order);
        assertTrue(output.getOut().contains(
                "operation=updateStatus orderId=4 previousStatus=PENDING status=PROCESSING outcome=success"));
        assertFalse(output.getOut().contains("dave"));
        assertFalse(output.getOut().contains("15.00"));
    }

    @Test
    void updateStatusReturnsEmptyAndLogsNotFoundWhenOrderIsMissing(CapturedOutput output) {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.updateStatus(99L, OrderStatus.COMPLETED);

        assertFalse(result.isPresent());
        verify(orderRepository, never()).save(any(Order.class));
        assertTrue(output.getOut().contains(
                "operation=updateStatus orderId=99 requestedStatus=COMPLETED outcome=not_found"));
    }
}
