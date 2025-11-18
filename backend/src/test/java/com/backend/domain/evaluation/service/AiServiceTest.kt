package com.backend.domain.evaluation.service

import com.backend.domain.evaluation.dto.AiDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AiServiceTest {

    /**
     * 진짜 OpenAI를 부르지 않고,
     * 우리가 원하는 문자열을 그대로 돌려주는 "가짜 게이트웨이"
     */
    class FakeAiGateway : AiGateway {
        override fun complete(content: String, prompt: String): String {
            return "[PROMPT]$prompt | [CONTENT]$content"
        }
    }

    @Test
    fun `content와 prompt를 넣으면 결과가 반환된다`() {
        // given
        val gateway: AiGateway = FakeAiGateway()
        val service = AiService(gateway)

        val req = AiDto.CompleteRequest(
            "안녕하세요",
            "요약해줘"
        )

        // when
        val res = service.complete(req)

        // then
        assertThat(res.result)
            .isEqualTo("[PROMPT]요약해줘 | [CONTENT]안녕하세요")
    }
}
