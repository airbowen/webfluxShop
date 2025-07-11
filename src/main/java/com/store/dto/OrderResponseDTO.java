package com.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {
    
    private Long id;
    private String orderCode;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime payTime;
    private String trackingNo;
    private LocalDateTime createTime;
    private List<OrderItemResponseDTO> items;
    
    // Constructors
    public OrderResponseDTO() {}
    
    public OrderResponseDTO(Long id, String orderCode, Long userId, Long merchantId, 
                           BigDecimal totalAmount, String status, LocalDateTime createTime) {
        this.id = id;
        this.orderCode = orderCode;
        this.userId = userId;
        this.merchantId = merchantId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createTime = createTime;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderCode() {
        return orderCode;
    }
    
    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getPayTime() {
        return payTime;
    }
    
    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }
    
    public String getTrackingNo() {
        return trackingNo;
    }
    
    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public List<OrderItemResponseDTO> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemResponseDTO> items) {
        this.items = items;
    }
    
    // Inner class for order item responses
    public static class OrderItemResponseDTO {
        
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private String refundStatus;
        
        // Constructors
        public OrderItemResponseDTO() {}
        
        public OrderItemResponseDTO(Long id, Long productId, String productName, 
                                   Integer quantity, BigDecimal price, String refundStatus) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.refundStatus = refundStatus;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        public String getRefundStatus() {
            return refundStatus;
        }
        
        public void setRefundStatus(String refundStatus) {
            this.refundStatus = refundStatus;
        }
    }
} 