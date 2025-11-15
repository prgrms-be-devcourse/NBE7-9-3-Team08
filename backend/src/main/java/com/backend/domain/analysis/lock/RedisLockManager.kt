package com.backend.domain.analysis.lock

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@Primary
class RedisLockManager(
    private val redisTemplate: StringRedisTemplate
) {

    private val log = LoggerFactory.getLogger(RedisLockManager::class.java)

    // 락 획득
    fun tryLock(key: String): Boolean {
        val lockKey = "$LOCK_PREFIX$key"
        val lockValue = Thread.currentThread().name

        val success: Boolean =
            redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS))
                ?: false

        if (success) {
            log.debug("락 획득 성공: {}", key)
        } else {
            log.warn("중복 분석 요청 차단: {}", key)
        }

        return success
    }

    // 락 TTL 갱신
    fun refreshLock(key: String) {
        val lockKey = "$LOCK_PREFIX$key"
        redisTemplate.expire(lockKey, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS))
        log.debug("락 타임스탬프 갱신: {}", key)
    }

    // 락 해제
    fun releaseLock(key: String) {
        val lockKey = "$LOCK_PREFIX$key"
        redisTemplate.delete(lockKey)
        log.debug("락 해제: {}", key)
    }

    companion object {
        private const val LOCK_PREFIX = "analysis:lock:"
        private const val LOCK_TIMEOUT_SECONDS: Long = 300
    }
}
