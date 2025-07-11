package com.store.controller;

import com.store.dto.OrderCreateRequestDTO;
import com.store.dto.OrderResponseDTO;
import com.store.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<List<OrderResponseDTO>> createOrder(
            @Valid @RequestBody OrderCreateRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginName = authentication.getName();
        // You may need to fetch userId from DB using loginName
        // For now, assume a method exists: getUserIdByLoginName
        Long userId = orderService.getUserIdByLoginName(loginName);
        logger.info("Creating order for user: {}", userId);
        
        try {
            List<OrderResponseDTO> orders = orderService.createOrder(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(orders);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable String orderCode) {
        
        logger.info("Getting order: {}", orderCode);
        
        try {
            OrderResponseDTO order = orderService.getOrderByCode(orderCode);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            logger.error("Order not found: {}", orderCode);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginName = authentication.getName();
        Long userId = orderService.getUserIdByLoginName(loginName);
        logger.info("Getting orders for user: {}", userId);
        
        try {
            List<OrderResponseDTO> orders = orderService.getOrdersByUser(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Failed to get user orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 