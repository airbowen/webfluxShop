package com.store.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisLockUtil(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Try to acquire a distributed lock
     * @param lockKey The lock key
     * @param lockValue The lock value (usually a unique identifier)
     * @param expireTime The lock expiration time
     * @param timeUnit The time unit for expiration
     * @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.of(timeUnit.toMillis(expireTime), java.time.temporal.ChronoUnit.MILLIS));
        return Boolean.TRUE.equals(result);
    }
    
    /**
     * Release a distributed lock
     * @param lockKey The lock key
     * @param lockValue The lock value (must match the one used to acquire)
     * @return true if lock released, false otherwise
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = RedisScript.of(script, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
        return Long.valueOf(1L).equals(result);
    }
    
    /**
     * Check if a lock exists
     * @param lockKey The lock key
     * @return true if lock exists, false otherwise
     */
    public boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
    
    /**
     * Get lock value
     * @param lockKey The lock key
     * @return The lock value or null if not found
     */
    public String getLockValue(String lockKey) {
        return redisTemplate.opsForValue().get(lockKey);
    }
    
    /**
     * Set a key with expiration for idempotency
     * @param key The key
     * @param value The value
     * @param expireTime The expiration time
     * @param timeUnit The time unit for expiration
     * @return true if set successfully, false if key already exists
     */
    public boolean setIfAbsent(String key, String value, long expireTime, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.of(timeUnit.toMillis(expireTime), java.time.temporal.ChronoUnit.MILLIS));
        return Boolean.TRUE.equals(result);
    }
    
    /**
     * Delete a key
     * @param key The key to delete
     * @return true if deleted, false otherwise
     */
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }
} 