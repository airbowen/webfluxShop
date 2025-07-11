package com.store.repository;

import com.store.entity.OrderMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderMessageRepository extends JpaRepository<OrderMessageEntity, Long> {
    
    List<OrderMessageEntity> findByStatus(String status);
    
    List<OrderMessageEntity> findByTopic(String topic);
    
    List<OrderMessageEntity> findByStatusAndRetryCountLessThan(String status, Integer maxRetryCount);
    
    @Query("SELECT om FROM OrderMessageEntity om WHERE om.status = 'FAILED' AND om.retryCount < :maxRetryCount AND (om.lastRetryTime IS NULL OR om.lastRetryTime < :retryBefore)")
    List<OrderMessageEntity> findFailedMessagesForRetry(@Param("maxRetryCount") Integer maxRetryCount, 
                                                       @Param("retryBefore") LocalDateTime retryBefore);
    
    @Query("SELECT om FROM OrderMessageEntity om WHERE om.status = 'PENDING' AND om.createTime < :timeoutBefore")
    List<OrderMessageEntity> findPendingMessagesForTimeout(@Param("timeoutBefore") LocalDateTime timeoutBefore);
} 