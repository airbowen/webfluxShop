package com.store.repository;

import com.store.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    
    List<OrderItemEntity> findByOrderId(Long orderId);
    
    List<OrderItemEntity> findByProductId(Long productId);
    
    @Query("SELECT oi FROM OrderItemEntity oi WHERE oi.orderId = :orderId")
    List<OrderItemEntity> findByOrderIdWithProductInfo(@Param("orderId") Long orderId);
    
    @Query("SELECT oi FROM OrderItemEntity oi WHERE oi.orderId IN :orderIds")
    List<OrderItemEntity> findByOrderIds(@Param("orderIds") List<Long> orderIds);
} 