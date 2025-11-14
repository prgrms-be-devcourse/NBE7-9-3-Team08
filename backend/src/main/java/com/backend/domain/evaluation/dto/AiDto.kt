package com.backend.domain.evaluation.dto

import jakarta.validation.constraints.NotBlank

/**
 * 요청/응답 DTO 묶음
 */
object AiDto {

    data class CompleteRequest(
        @field:NotBlank(message = "content는 비어 있을 수 없습니다.")
        val content: String,

        @field:NotBlank(message = "prompt는 비어 있을 수 없습니다.")
        val prompt: String,
    )

    data class CompleteResponse(
        val result: String,
    )
}
