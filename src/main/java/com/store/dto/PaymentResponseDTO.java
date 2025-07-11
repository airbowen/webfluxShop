package com.store.dto;

import java.time.LocalDateTime;

public class PaymentResponseDTO {
    
    private String orderCode;
    private String paymentId;
    private String status;
    private String message;
    private LocalDateTime paymentTime;
    private String transactionId;
    
    // Constructors
    public PaymentResponseDTO() {}
    
    public PaymentResponseDTO(String orderCode, String paymentId, String status, String message) {
        this.orderCode = orderCode;
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
        this.paymentTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getOrderCode() {
        return orderCode;
    }
    
    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }
    
    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
} 