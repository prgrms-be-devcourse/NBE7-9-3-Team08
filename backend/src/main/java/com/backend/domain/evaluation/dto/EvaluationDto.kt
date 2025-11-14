package com.backend.domain.evaluation.dto

object EvaluationDto {

    /** AI가 반환해야 할 점수 묶음 */
    data class Scores(
        val readme: Int,   // 0~25
        val test: Int,     // 0~25
        val commit: Int,   // 0~25
        val cicd: Int,     // 0~25
    )

    /** AI가 반환해야 할 전체 결과 */
    data class AiResult(
        val summary: String,
        val strengths: List<String>,
        val improvements: List<String>,
        val scores: Scores,
    )
}
