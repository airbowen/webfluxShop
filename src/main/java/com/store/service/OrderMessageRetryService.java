package com.store.service;

import com.store.entity.OrderMessageEntity;
import com.store.repository.OrderMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderMessageRetryService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderMessageRetryService.class);
    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_INTERVAL_MINUTES = 5;
    
    private final OrderMessageRepository orderMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public OrderMessageRetryService(OrderMessageRepository orderMessageRepository,
                                   KafkaTemplate<String, String> kafkaTemplate) {
        this.orderMessageRepository = orderMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void retryFailedMessages() {
        logger.info("Starting retry process for failed messages");
        
        LocalDateTime retryBefore = LocalDateTime.now().minusMinutes(RETRY_INTERVAL_MINUTES);
        List<OrderMessageEntity> failedMessages = orderMessageRepository.findFailedMessagesForRetry(MAX_RETRY_COUNT, retryBefore);
        
        logger.info("Found {} failed messages to retry", failedMessages.size());
        
        for (OrderMessageEntity message : failedMessages) {
            try {
                retryMessage(message);
            } catch (Exception e) {
                logger.error("Failed to retry message {}: {}", message.getId(), e.getMessage());
                updateMessageRetryCount(message);
            }
        }
    }
    
    @Scheduled(fixedRate = 600000) // Run every 10 minutes
    @Transactional
    public void handleTimeoutMessages() {
        logger.info("Starting timeout process for pending messages");
        
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(30); // 30 minutes timeout
        List<OrderMessageEntity> pendingMessages = orderMessageRepository.findPendingMessagesForTimeout(timeoutBefore);
        
        logger.info("Found {} pending messages to timeout", pendingMessages.size());
        
        for (OrderMessageEntity message : pendingMessages) {
            try {
                handleTimeoutMessage(message);
            } catch (Exception e) {
                logger.error("Failed to handle timeout message {}: {}", message.getId(), e.getMessage());
            }
        }
    }
    
    private void retryMessage(OrderMessageEntity message) {
        logger.info("Retrying message {} for topic {}", message.getId(), message.getTopic());
        
        // Send to Kafka
        kafkaTemplate.send(message.getTopic(), message.getPayload())
                .whenComplete(
                        (result, ex) -> {
                            if (ex == null) {
                                logger.info("Message {} retried successfully", message.getId());
                                message.setStatus("SENT");
                                message.setRetryCount(message.getRetryCount() + 1);
                                message.setLastRetryTime(LocalDateTime.now());
                                orderMessageRepository.save(message);
                            } else {
                                logger.error("Failed to retry message {}: {}", message.getId(), ex.getMessage());
                                updateMessageRetryCount(message);
                            }
                        }
                );
    }
    
    private void updateMessageRetryCount(OrderMessageEntity message) {
        message.setRetryCount(message.getRetryCount() + 1);
        message.setLastRetryTime(LocalDateTime.now());
        
        if (message.getRetryCount() >= MAX_RETRY_COUNT) {
            message.setStatus("DEAD");
            logger.warn("Message {} marked as DEAD after {} retries", message.getId(), MAX_RETRY_COUNT);
        }
        
        orderMessageRepository.save(message);
    }
    
    private void handleTimeoutMessage(OrderMessageEntity message) {
        logger.warn("Message {} timed out, marking as FAILED", message.getId());
        message.setStatus("FAILED");
        orderMessageRepository.save(message);
    }
    
    public void manualRetryMessage(Long messageId) {
        OrderMessageEntity message = orderMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        
        if ("DEAD".equals(message.getStatus())) {
            message.setStatus("FAILED");
            message.setRetryCount(0);
            orderMessageRepository.save(message);
        }
        
        retryMessage(message);
    }
    
    public List<OrderMessageEntity> getFailedMessages() {
        return orderMessageRepository.findByStatus("FAILED");
    }
    
    public List<OrderMessageEntity> getDeadMessages() {
        return orderMessageRepository.findByStatus("DEAD");
    }
} 