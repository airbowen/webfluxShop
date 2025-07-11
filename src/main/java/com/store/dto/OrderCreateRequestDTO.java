package com.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class OrderCreateRequestDTO {
    
    @NotEmpty(message = "Items list cannot be empty")
    @Size(min = 1, max = 50, message = "Items list must contain between 1 and 50 items")
    @Valid
    private List<OrderItemDTO> items;
    
    // Constructors
    public OrderCreateRequestDTO() {}
    
    public OrderCreateRequestDTO(List<OrderItemDTO> items) {
        this.items = items;
    }
    
    // Getters and Setters
    public List<OrderItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
    
    // Inner class for order items
    public static class OrderItemDTO {
        
        @NotNull(message = "Product ID cannot be null")
        private Long productId;
        
        @NotNull(message = "Quantity cannot be null")
        private Integer quantity;
        
        // Constructors
        public OrderItemDTO() {}
        
        public OrderItemDTO(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        // Getters and Setters
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
} 