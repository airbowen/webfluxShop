package com.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.OrderCreateRequestDTO;
import com.store.dto.OrderResponseDTO;
import com.store.service.OrderService;
import com.store.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String validToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        validToken = "valid.jwt.token";
        
        // Commented out legacy JwtUtil method calls due to refactor
        // jwtUtil.validateToken(validToken);
        // jwtUtil.extractUserId(validToken);
    }

    @Test
    void createOrder_Success() throws Exception {
        // Arrange
        OrderCreateRequestDTO.OrderItemDTO item = new OrderCreateRequestDTO.OrderItemDTO(1L, 2);
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item));

        OrderResponseDTO.OrderItemResponseDTO responseItem = new OrderResponseDTO.OrderItemResponseDTO(
                1L, 1L, "iPhone 15", 2, new BigDecimal("999.99"), "NONE"
        );
        OrderResponseDTO response = new OrderResponseDTO(
                1L, "ORD123456789", userId, 1L, new BigDecimal("1999.98"), "PENDING", LocalDateTime.now()
        );
        response.setItems(Arrays.asList(responseItem));

        when(orderService.createOrder(any(OrderCreateRequestDTO.class), eq(userId)))
                .thenReturn(Arrays.asList(response));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].orderCode").value("ORD123456789"))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].items[0].productName").value("iPhone 15"));
    }

    @Test
    void createOrder_MissingAuthorization_ReturnsUnauthorized() throws Exception {
        // Arrange
        OrderCreateRequestDTO.OrderItemDTO item = new OrderCreateRequestDTO.OrderItemDTO(1L, 2);
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_InvalidToken_ReturnsBadRequest() throws Exception {
        // Arrange
        OrderCreateRequestDTO.OrderItemDTO item = new OrderCreateRequestDTO.OrderItemDTO(1L, 2);
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList(item));

        // when(jwtUtil.validateToken("invalid.token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_EmptyItems_ReturnsBadRequest() throws Exception {
        // Arrange
        OrderCreateRequestDTO request = new OrderCreateRequestDTO(Arrays.asList());

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderByCode_Success() throws Exception {
        // Arrange
        String orderCode = "ORD123456789";
        OrderResponseDTO.OrderItemResponseDTO responseItem = new OrderResponseDTO.OrderItemResponseDTO(
                1L, 1L, "iPhone 15", 2, new BigDecimal("999.99"), "NONE"
        );
        OrderResponseDTO response = new OrderResponseDTO(
                1L, orderCode, userId, 1L, new BigDecimal("1999.98"), "PENDING", LocalDateTime.now()
        );
        response.setItems(Arrays.asList(responseItem));

        when(orderService.getOrderByCode(orderCode)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/orders/" + orderCode)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCode").value(orderCode))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items[0].productName").value("iPhone 15"));
    }

    @Test
    void getOrderByCode_OrderNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String orderCode = "ORD123456789";
        when(orderService.getOrderByCode(orderCode))
                .thenThrow(new IllegalArgumentException("Order not found: " + orderCode));

        // Act & Assert
        mockMvc.perform(get("/api/orders/" + orderCode)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderByCode_UnauthorizedAccess_ReturnsForbidden() throws Exception {
        // Arrange
        String orderCode = "ORD123456789";
        Long otherUserId = 2L;
        OrderResponseDTO response = new OrderResponseDTO(
                1L, orderCode, otherUserId, 1L, new BigDecimal("1999.98"), "PENDING", LocalDateTime.now()
        );

        when(orderService.getOrderByCode(orderCode)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/orders/" + orderCode)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserOrders_Success() throws Exception {
        // Arrange
        OrderResponseDTO response1 = new OrderResponseDTO(
                1L, "ORD123456789", userId, 1L, new BigDecimal("999.99"), "PENDING", LocalDateTime.now()
        );
        OrderResponseDTO response2 = new OrderResponseDTO(
                2L, "ORD123456790", userId, 2L, new BigDecimal("1999.99"), "PAID", LocalDateTime.now()
        );
        List<OrderResponseDTO> responses = Arrays.asList(response1, response2);

        when(orderService.getOrdersByUser(userId)).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderCode").value("ORD123456789"))
                .andExpect(jsonPath("$[1].orderCode").value("ORD123456790"))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[1].userId").value(userId));
    }

    @Test
    void getUserOrders_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(orderService.getOrdersByUser(userId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isInternalServerError());
    }
} 