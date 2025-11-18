package com.backend.global.openai

import io.github.cdimascio.dotenv.Dotenv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull

class OpenAIKeyUtilTest {

    private var oldProp: String? = null

    @BeforeEach
    fun setUp() {
        // 테스트 전에 기존 시스템 프로퍼티 값 백업
        oldProp = System.getProperty("OPENAI_API_KEY")
    }

    @AfterEach
    fun tearDown() {
        // 테스트 끝나면 시스템 프로퍼티 원상 복구
        if (oldProp != null) {
            System.setProperty("OPENAI_API_KEY", oldProp)
        } else {
            System.clearProperty("OPENAI_API_KEY")
        }
    }

    private fun isEnvKeyBlank(): Boolean {
        val env = System.getenv("OPENAI_API_KEY")
        return env.isNullOrBlank()
    }

    @Test
    fun `환경변수와 시스템 프로퍼티가 둘 다 없으면 Dotenv 값 혹은 null을 반환한다`() {
        // 이 테스트는 "OS 환경변수는 비어있다"는 전제에서만 의미 있음
        Assumptions.assumeTrue(
            isEnvKeyBlank(),
            "환경 변수 OPENAI_API_KEY가 설정되어 있어 이 테스트는 건너뜁니다."
        )

        // 시스템 프로퍼티 비우기
        System.clearProperty("OPENAI_API_KEY")

        // .env 기준 기대값 계산 (실제 구현과 동일한 방식)
        val dotenvKey = try {
            Dotenv.configure().ignoreIfMissing().load().get("OPENAI_API_KEY")
        } catch (_: Throwable) {
            null
        }
        val expected = dotenvKey?.takeIf { it.isNotBlank() }

        val key = OpenAIKeyUtil.findKey()

        // 구현과 동일한 우선순위를 그대로 검증
        assertEquals(expected, key)
    }

    @Test
    fun `환경변수는 없고 시스템 프로퍼티만 있을 때는 프로퍼티 값을 반환한다`() {
        Assumptions.assumeTrue(
            isEnvKeyBlank(),
            "환경 변수 OPENAI_API_KEY가 설정되어 있어 이 테스트는 건너뜁니다."
        )

        System.setProperty("OPENAI_API_KEY", "property-key")

        val key = OpenAIKeyUtil.findKey()

        // .env에 뭐가 있든 간에 시스템 프로퍼티가 .env보다 우선이어야 함
        assertEquals("property-key", key)
    }

    @Test
    fun `환경변수와 시스템 프로퍼티가 모두 있을 때는 환경변수를 우선한다`() {
        val env = System.getenv("OPENAI_API_KEY")
        Assumptions.assumeTrue(
            !env.isNullOrBlank(),
            "환경 변수 OPENAI_API_KEY가 없어서 이 테스트는 건너뜁니다."
        )

        // 시스템 프로퍼티에 다른 값 설정
        System.setProperty("OPENAI_API_KEY", "property-key")

        val key = OpenAIKeyUtil.findKey()

        assertNotNull(key)
        // 최소한 시스템 프로퍼티 값(=property-key)보다는 OS 환경변수가 우선이어야 한다
        assertNotEquals("property-key", key)
    }
}
