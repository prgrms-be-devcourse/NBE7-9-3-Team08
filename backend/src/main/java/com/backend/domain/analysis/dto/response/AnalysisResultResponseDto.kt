package com.backend.domain.analysis.dto.response

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import java.time.LocalDateTime

/**
 * 특정 분석 결과의 상세 정보 응답 DTO
 * 분석 점수, 피드백 등을 포함
 */
data class AnalysisResultResponseDto(
    val totalScore: Int,
    val readmeScore: Int,
    val testScore: Int,
    val commitScore: Int,
    val cicdScore: Int,
    val summary: String,
    val strengths: String,
    val improvements: String,
    val createDate: LocalDateTime,
) {
    companion object {
        @JvmStatic
        fun from(analysisResult: AnalysisResult, score: Score): AnalysisResultResponseDto {
            return AnalysisResultResponseDto(
                totalScore = score.totalScore,
                readmeScore = score.readmeScore,
                testScore = score.testScore,
                commitScore = score.commitScore,
                cicdScore = score.cicdScore,
                summary = analysisResult.summary,
                strengths = analysisResult.strengths,
                improvements = analysisResult.improvements,
                createDate = analysisResult.createDate,
            )
        }
    }
}
