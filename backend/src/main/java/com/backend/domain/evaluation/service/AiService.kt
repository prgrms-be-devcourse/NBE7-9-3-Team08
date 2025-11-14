package com.backend.domain.evaluation.service

import com.backend.domain.evaluation.dto.AiDto
import org.springframework.stereotype.Service

@Service
class AiService(
    private val aiGateway: AiGateway,
) {

    fun complete(req: AiDto.CompleteRequest): AiDto.CompleteResponse {
        val result = aiGateway.complete(req.content, req.prompt)
        return AiDto.CompleteResponse(result)
    }
}
