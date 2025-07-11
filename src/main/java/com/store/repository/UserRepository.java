package com.store.repository;

import com.store.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByLoginName(String loginName);
    
    @Query("SELECT u FROM UserEntity u WHERE u.loginName = :loginName AND u.password = :password")
    Optional<UserEntity> findByLoginNameAndPassword(@Param("loginName") String loginName, 
                                                   @Param("password") String password);
    
    boolean existsByLoginName(String loginName);
    
    boolean existsByEmail(String email);
} 