package com.backend.domain.evaluation.controller

import com.backend.domain.evaluation.dto.AiDto
import com.backend.domain.evaluation.service.AiService
import com.backend.global.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiService: AiService,
) {

    /**
     * POST /api/ai/complete
     *
     * 요청:
     * {
     *   "content": "분석 대상 글",
     *   "prompt":  "역할/지시문"
     * }
     *
     * 응답:
     * {
     *   "code": "OK",
     *   "message": "성공",
     *   "data": { "result": "모델 응답" }
     * }
     */
    @PostMapping("/complete")
    fun complete(
        @Valid @RequestBody request: AiDto.CompleteRequest,
    ): ApiResponse<AiDto.CompleteResponse> =
        ApiResponse.success(aiService.complete(request))
}
