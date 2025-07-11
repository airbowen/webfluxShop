package com.store.repository;

import com.store.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    
    Optional<OrderEntity> findByOrderCode(String orderCode);
    
    List<OrderEntity> findByUserId(Long userId);
    
    List<OrderEntity> findByMerchantId(Long merchantId);
    
    List<OrderEntity> findByUserIdAndStatus(Long userId, String status);
    
    List<OrderEntity> findByMerchantIdAndStatus(Long merchantId, String status);
    
    @Query("SELECT o FROM OrderEntity o WHERE o.userId = :userId ORDER BY o.createTime DESC")
    List<OrderEntity> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId);
    
    @Query("SELECT o FROM OrderEntity o WHERE o.merchantId = :merchantId ORDER BY o.createTime DESC")
    List<OrderEntity> findByMerchantIdOrderByCreateTimeDesc(@Param("merchantId") Long merchantId);
    
    boolean existsByOrderCode(String orderCode);
} 