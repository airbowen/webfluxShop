package com.store.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.dto.PaymentRequestDTO;
import com.store.dto.PaymentResponseDTO;
import com.store.entity.OrderEntity;
import com.store.entity.OrderMessageEntity;
import com.store.repository.OrderMessageRepository;
import com.store.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderMessageRepository orderMessageRepository;
    
    public PaymentService(OrderRepository orderRepository,
                         KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         OrderMessageRepository orderMessageRepository) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.orderMessageRepository = orderMessageRepository;
    }
    
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        logger.info("Processing payment for order: {}", request.getOrderCode());
        
        // Validate order exists and is in PENDING status
        OrderEntity order = validateOrder(request.getOrderCode());
        
        // Validate payment amount matches order total
        validatePaymentAmount(order, request.getAmount());
        
        try {
            // Process payment with external gateway (simulated)
            PaymentResponseDTO response = processPaymentWithGateway(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                // Update order status to PAID
                updateOrderStatus(order, "PAID");
                
                // Send payment success event
                sendPaymentEvent(PAYMENT_PROCESSED_TOPIC, order.getOrderCode(), response);
                
                logger.info("Payment processed successfully for order: {}", request.getOrderCode());
            } else {
                // Send payment failed event
                sendPaymentEvent(PAYMENT_FAILED_TOPIC, order.getOrderCode(), response);
                
                logger.error("Payment failed for order: {} - {}", request.getOrderCode(), response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Payment processing error for order: {}", request.getOrderCode(), e);
            
            PaymentResponseDTO errorResponse = new PaymentResponseDTO(
                    request.getOrderCode(),
                    UUID.randomUUID().toString(),
                    "FAILED",
                    "Payment processing error: " + e.getMessage()
            );
            
            sendPaymentEvent(PAYMENT_FAILED_TOPIC, order.getOrderCode(), errorResponse);
            
            return errorResponse;
        }
    }
    
    private OrderEntity validateOrder(String orderCode) {
        Optional<OrderEntity> orderOpt = orderRepository.findByOrderCode(orderCode);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderCode);
        }
        
        OrderEntity order = orderOpt.get();
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not in PENDING status: " + order.getStatus());
        }
        
        return order;
    }
    
    private void validatePaymentAmount(OrderEntity order, Double paymentAmount) {
        BigDecimal orderAmount = order.getTotalAmount();
        BigDecimal paymentAmountBD = BigDecimal.valueOf(paymentAmount);
        
        if (orderAmount.compareTo(paymentAmountBD) != 0) {
            throw new IllegalArgumentException(
                    String.format("Payment amount %.2f does not match order total %.2f", 
                            paymentAmount, orderAmount)
            );
        }
    }
    
    private PaymentResponseDTO processPaymentWithGateway(PaymentRequestDTO request) {
        // Simulate external payment gateway processing
        // In a real implementation, this would call an actual payment gateway API
        
        try {
            // Simulate network delay
            Thread.sleep(1000);
            
            // Simulate payment success/failure based on card number
            String lastFourDigits = request.getCardNumber().substring(12);
            boolean isSuccess = !"0000".equals(lastFourDigits); // Fail if card ends with 0000
            
            if (isSuccess) {
                return new PaymentResponseDTO(
                        request.getOrderCode(),
                        UUID.randomUUID().toString(),
                        "SUCCESS",
                        "Payment processed successfully"
                );
            } else {
                return new PaymentResponseDTO(
                        request.getOrderCode(),
                        UUID.randomUUID().toString(),
                        "FAILED",
                        "Insufficient funds"
                );
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
    }
    
    private void updateOrderStatus(OrderEntity order, String status) {
        order.setStatus(status);
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);
    }
    
    private void sendPaymentEvent(String topic, String orderCode, PaymentResponseDTO response) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("orderCode", orderCode);
            event.put("paymentId", response.getPaymentId());
            event.put("status", response.getStatus());
            event.put("message", response.getMessage());
            event.put("paymentTime", response.getPaymentTime());
            event.put("transactionId", response.getTransactionId());

            String payload = objectMapper.writeValueAsString(event);

            // Always log the message first as PENDING
            AtomicReference<OrderMessageEntity> msgEntityRef = new AtomicReference<>(new OrderMessageEntity(topic, payload));
            msgEntityRef.get().setStatus("PENDING");
            msgEntityRef.set(orderMessageRepository.save(msgEntityRef.get()));

            kafkaTemplate.send(topic, orderCode, payload)
                .whenComplete((result, ex) -> {
                    OrderMessageEntity msgEntity = msgEntityRef.get();
                    if (ex == null) {
                        logger.info("Payment event sent successfully to topic: {}", topic);
                        msgEntity.setStatus("SENT");
                    } else {
                        logger.error("Failed to send payment event to topic: {}", topic, ex);
                        msgEntity.setStatus("FAILED");
                    }
                    msgEntity.setLastRetryTime(LocalDateTime.now());
                    orderMessageRepository.save(msgEntity);
                });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payment event", e);
        }
    }
    
    public PaymentResponseDTO getPaymentStatus(String orderCode) {
        Optional<OrderEntity> orderOpt = orderRepository.findByOrderCode(orderCode);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderCode);
        }
        
        OrderEntity order = orderOpt.get();
        
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setOrderCode(orderCode);
        response.setStatus(order.getStatus());
        
        if ("PAID".equals(order.getStatus())) {
            response.setMessage("Payment completed successfully");
            response.setPaymentTime(order.getPayTime());
        } else if ("PENDING".equals(order.getStatus())) {
            response.setMessage("Payment pending");
        } else {
            response.setMessage("Payment status: " + order.getStatus());
        }
        
        return response;
    }
} 