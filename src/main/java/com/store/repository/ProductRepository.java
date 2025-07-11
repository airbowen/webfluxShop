package com.store.repository;

import com.store.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    
    List<ProductEntity> findByMerchantIdAndStatus(Long merchantId, String status);
    
    List<ProductEntity> findByStatus(String status);
    
    @Lock(LockModeType.OPTIMISTIC)
    Optional<ProductEntity> findById(Long id);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findByIdWithPessimisticLock(@Param("id") Long id);
    
    @Query("SELECT p FROM ProductEntity p WHERE p.id IN :productIds AND p.status = 'ON_SALE'")
    List<ProductEntity> findByIdsAndOnSale(@Param("productIds") List<Long> productIds);
} 