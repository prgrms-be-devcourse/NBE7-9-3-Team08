package com.backend.global.openai

import com.openai.client.OpenAIClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * OpenAIConfig 에서 openai.api.key 프로퍼티 유무에 따라
 * OpenAIClient 빈 생성 여부가 달라지는지 검증
 */
class OpenAIConfigTest {

    // OpenAIConfig 만 등록하는 아주 가벼운 컨텍스트 러너
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(OpenAIConfig::class.java)

    @Test
    fun `openai_api_key가 설정되어 있으면 OpenAIClient 빈이 생성된다`() {
        contextRunner
            .withPropertyValues("openai.api.key=test-key")
            .run { context ->
                // OpenAIClient 빈이 정확히 하나만 존재해야 함
                assertThat(context).hasSingleBean(OpenAIClient::class.java)
            }
    }

    @Test
    fun `openai_api_key가 없으면 OpenAIClient 빈이 생성되지 않는다`() {
        contextRunner
            // 일부러 openai.api.key 값을 넣지 않는다
            .run { context ->
                assertThat(context).doesNotHaveBean(OpenAIClient::class.java)
            }
    }
}
