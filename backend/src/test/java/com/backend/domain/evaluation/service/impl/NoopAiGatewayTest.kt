package com.backend.domain.evaluation.service.impl

import com.backend.domain.evaluation.service.AiGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoopAiGatewayTest {

    @Test
    fun `complete는 OpenAI 비활성화 안내 문구를 반환한다`() {
        // given
        val gateway: AiGateway = NoopAiGateway()

        // when
        val result = gateway.complete(
            content = "아무 컨텐츠",
            prompt = "아무 프롬프트"
        )

        // then
        assertThat(result)
            .contains("OPENAI 비활성화")
            .contains("OPENAI_API_KEY")
    }
}
