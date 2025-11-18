package com.backend.domain.analysis.service

import com.backend.domain.analysis.lock.RedisLockManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
@Tag("redis")
class RedisLockManagerTest(
    @Autowired private val lockManager: RedisLockManager,
    @Autowired private val redisTemplate: StringRedisTemplate
) {

    @BeforeEach
    fun setUp() {
        redisTemplate.connectionFactory!!.connection.flushDb()
    }

    @AfterEach
    fun tearDown() {
        redisTemplate.connectionFactory!!.connection.flushDb()
    }

    @Test
    @DisplayName("같은 URL이 동시에 여러 요청이 들어오면 하나만 락 획득")
    fun `동시 요청은 하나만 락 획득`() {
        val key = "user1:https://github.com/test/repo"
        val threadCount = 10

        val executor: ExecutorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger()
        val failCount = AtomicInteger()

        repeat(threadCount) {
            executor.submit {
                try {
                    if (lockManager.tryLock(key)) {
                        successCount.incrementAndGet()
                    } else {
                        failCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(2, TimeUnit.SECONDS)
        executor.shutdown()

        println("성공: ${successCount.get()}, 실패: ${failCount.get()}")

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(9)

        lockManager.releaseLock(key)
        assertThat(lockManager.tryLock(key)).isTrue()
    }

    @Test
    @DisplayName("서로 다른 URL은 동시에 분석이 가능해야 함")
    fun `다른 키들은 동시에 락 획득 가능`() {
        val key1 = "user1:repo1"
        val key2 = "user1:repo2"

        val lock1 = lockManager.tryLock(key1)
        val lock2 = lockManager.tryLock(key2)

        assertThat(lock1).isTrue()
        assertThat(lock2).isTrue()
    }

    @Test
    @DisplayName("Redis TTL로 자동 만료 확인")
    fun `락 TTL 자동 만료 확인`() {
        val key = "user3:repo3"

        assertThat(lockManager.tryLock(key)).isTrue()
        assertThat(lockManager.tryLock(key)).isFalse()

        val ttl = redisTemplate.getExpire("analysis:lock:$key", TimeUnit.SECONDS)
        assertThat(ttl).isBetween(295L, 300L)
    }

    @Test
    @DisplayName("refreshLock 호출 시 TTL 연장 확인")
    fun `refreshLock 이 invoke 되면 TTL 연장`() {
        val key = "user4:repo4"

        lockManager.tryLock(key)

        Thread.sleep(3000)
        val ttlBefore = redisTemplate.getExpire("analysis:lock:$key", TimeUnit.SECONDS)

        lockManager.refreshLock(key)
        val ttlAfter = redisTemplate.getExpire("analysis:lock:$key", TimeUnit.SECONDS)

        assertThat(ttlAfter).isGreaterThan(ttlBefore)
        assertThat(ttlAfter).isBetween(295L, 300L)
    }

    @Test
    @DisplayName("락 해제 후 재획득 가능")
    fun `락 해제 후 다시 락 획득 가능`() {
        val key = "user5:repo5"

        assertThat(lockManager.tryLock(key)).isTrue()
        assertThat(lockManager.tryLock(key)).isFalse()

        lockManager.releaseLock(key)

        assertThat(lockManager.tryLock(key)).isTrue()
    }
}
