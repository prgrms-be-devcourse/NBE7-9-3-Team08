package com.backend.domain.user.util

import lombok.RequiredArgsConstructor
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@RequiredArgsConstructor
class RedisUtil(
    private val redisTemplate: StringRedisTemplate
) {
     // Redis에 데이터를 저장하면서 만료 시간을 설정
    fun setData(key: String, value: String, duration: Long) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(duration))
    }

     // Redis에서 데이터를 가져오기
    fun getData(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    // Redis에서 데이터를 삭제합니다.
    fun deleteData(key: String): Boolean {
        return redisTemplate.delete(key)
    }

     // Redis에서 데이터가 있는지 확인만 합니다.
    fun hasKey(key: String): Boolean {
        return redisTemplate.hasKey(key)
    }
}