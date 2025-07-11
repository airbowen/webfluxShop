package com.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.OrderCreateRequestDTO;
import com.store.dto.OrderResponseDTO;
import com.store.entity.*;
import com.store.repository.*;
import com.store.util.RedisLockUtil;
import com.store.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMessageRepository orderMessageRepository;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private RedisLockUtil redisLockUtil;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private ProductEntity product1;
    private ProductEntity product2;
    private OrderCreateRequestDTO.OrderItemDTO item1;
    private OrderCreateRequestDTO.OrderItemDTO item2;

    @BeforeEach
    void setUp() {
        // Setup test products
        product1 = new ProductEntity();
        product1.setId(1L);
        product1.setName("iPhone 15");
        product1.setMerchantId(1L);
        product1.setPrice(new BigDecimal("999.99"));
        product1.setStock(50);
        product1.setStatus("ON_SALE");
        product1.setVersion(0);

        product2 = new ProductEntity();
        product2.setId(2L);
        product2.setName("MacBook Pro");
        product2.setMerchantId(1L);
        product2.setPrice(new BigDecimal("1999.99"));
        product2.setStock(25);
        product2.setStatus("ON_SALE");
        product2.setVersion(0);

        // Setup test order items
        item1 = new OrderCreateRequestDTO.OrderItemDTO(1L, 2);
        item2 = new OrderCreateRequestDTO.OrderItemDTO(2L, 1);
    }

    @Test
    void createOrder_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        String orderCode = "ORD123456789";
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item1, item2));

        when(snowflakeIdGenerator.nextOrderCode()).thenReturn(orderCode);
        when(redisLockUtil.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(redisLockUtil.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(productRepository.findByIdsAndOnSale(anyList())).thenReturn(Arrays.asList(product1, product2));
        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(product2));
        when(productRepository.saveAll(anyList())).thenReturn(Arrays.asList(product1, product2));

        OrderEntity savedOrder = new OrderEntity(orderCode, userId, 1L, new BigDecimal("2999.97"));
        savedOrder.setId(1L);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(null);

        // Act
        List<OrderResponseDTO> result = orderService.createOrder(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderCode, result.get(0).getOrderCode());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(1L, result.get(0).getMerchantId());

        verify(redisLockUtil).tryLock(anyString(), anyString(), anyLong(), any());
        verify(redisLockUtil).setIfAbsent(anyString(), anyString(), anyLong(), any());
        verify(productRepository).findByIdsAndOnSale(Arrays.asList(1L, 2L));
        verify(orderRepository).save(any(OrderEntity.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(redisLockUtil).releaseLock(anyString(), anyString());
    }

    @Test
    void createOrder_EmptyItems_ThrowsException() {
        // Arrange
        Long userId = 1L;
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(request, userId);
        });
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        // Arrange
        Long userId = 1L;
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item1));

        when(productRepository.findByIdsAndOnSale(anyList())).thenReturn(Arrays.asList());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(request, userId);
        });
    }

    @Test
    void createOrder_InsufficientStock_ThrowsException() {
        // Arrange
        Long userId = 1L;
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item1));

        product1.setStock(1); // Only 1 in stock, but requesting 2
        when(productRepository.findByIdsAndOnSale(anyList())).thenReturn(Arrays.asList(product1));
        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product1));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(request, userId);
        });
    }

    @Test
    void createOrder_LockAcquisitionFailed_ThrowsException() {
        // Arrange
        Long userId = 1L;
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item1));

        when(redisLockUtil.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request, userId);
        });
    }

    @Test
    void getOrderByCode_Success() {
        // Arrange
        String orderCode = "ORD123456789";
        Long orderId = 1L;
        Long userId = 1L;

        OrderEntity order = new OrderEntity(orderCode, userId, 1L, new BigDecimal("999.99"));
        order.setId(orderId);
        order.setCreateTime(LocalDateTime.now());

        OrderItemEntity orderItem = new OrderItemEntity(orderId, 1L, 1, new BigDecimal("999.99"));
        orderItem.setId(1L);

        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(orderItem));
        when(productRepository.findAllById(anyList())).thenReturn(Arrays.asList(product1));

        // Act
        OrderResponseDTO result = orderService.getOrderByCode(orderCode);

        // Assert
        assertNotNull(result);
        assertEquals(orderCode, result.getOrderCode());
        assertEquals(userId, result.getUserId());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getOrderByCode_OrderNotFound_ThrowsException() {
        // Arrange
        String orderCode = "ORD123456789";
        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getOrderByCode(orderCode);
        });
    }

    @Test
    void getOrdersByUser_Success() {
        // Arrange
        Long userId = 1L;
        String orderCode = "ORD123456789";
        Long orderId = 1L;

        OrderEntity order = new OrderEntity(orderCode, userId, 1L, new BigDecimal("999.99"));
        order.setId(orderId);
        order.setCreateTime(LocalDateTime.now());

        when(orderRepository.findByUserIdOrderByCreateTimeDesc(userId)).thenReturn(Arrays.asList(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Arrays.asList());
        when(productRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // Act
        List<OrderResponseDTO> result = orderService.getOrdersByUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderCode, result.get(0).getOrderCode());
    }
} 