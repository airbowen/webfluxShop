package com.store.controller;

import com.store.dto.PaymentRequestDTO;
import com.store.dto.PaymentResponseDTO;
import com.store.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginName = authentication.getName();
        // If userId is needed, fetch from DB using loginName
        logger.info("Processing payment for order: {} by user: {}", request.getOrderCode(), loginName);
        try {
            PaymentResponseDTO response = paymentService.processPayment(request);
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment request: {}", e.getMessage());
            PaymentResponseDTO errorResponse = new PaymentResponseDTO(
                    request.getOrderCode(),
                    null,
                    "FAILED",
                    e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Payment processing error: {}", e.getMessage());
            PaymentResponseDTO errorResponse = new PaymentResponseDTO(
                    request.getOrderCode(),
                    null,
                    "FAILED",
                    "Internal server error"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/{orderCode}/status")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(@PathVariable String orderCode) {
        
        logger.info("Getting payment status for order: {}", orderCode);
        
        try {
            PaymentResponseDTO response = paymentService.getPaymentStatus(orderCode);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Order not found: {}", orderCode);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Error getting payment status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 