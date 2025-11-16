package com.backend.domain.evaluation.service.impl

import com.backend.domain.evaluation.service.AiGateway
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service

@Service
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText(environment['openai.api.key'])")
class NoopAiGateway : AiGateway {

    @PostConstruct
    fun logActive() {
        println("[AiGateway] NoopAiGateway ACTIVE")
    }

    override fun complete(content: String, prompt: String): String {
        return "[OPENAI 비활성화] OPENAI_API_KEY가 설정되지 않았습니다."
    }
}
