package com.backend.domain.analysis.dto.response

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.repository.dto.response.RepositoryResponse
import java.time.LocalDateTime

/**
 * Repository 상세 정보 + 분석 버전 목록 응답 DTO
 * 특정 Repository의 모든 분석 버전을 조회할 때 사용
 */
data class HistoryResponseDto(
    val repository: RepositoryResponse,              // Repository 기본 정보
    val analysisVersions: List<AnalysisVersionDto>,
) {

    /**
     * Java에서 쓰던 정적 메서드:
     * HistoryResponseDto.of(repositoryResponse, versions);
     */
    companion object {
        @JvmStatic
        fun of(
            repository: RepositoryResponse,
            versions: List<AnalysisVersionDto>,
        ): HistoryResponseDto =
            HistoryResponseDto(repository, versions)
    }

    data class AnalysisVersionDto(
        val analysisId: Long,
        val analysisDate: LocalDateTime,
        val totalScore: Int,
        val versionLabel: String,
    ) {

        /**
         * Java에서 쓰던 정적 메서드:
         * HistoryResponseDto.AnalysisVersionDto.from(analysis, versionNumber);
         */
        companion object {
            @JvmStatic
            fun from(
                analysis: AnalysisResult,
                versionNumber: Int,
            ): AnalysisVersionDto =
                AnalysisVersionDto(
                    analysisId = analysis.id
                        ?: throw IllegalStateException("AnalysisResult.id is null"),
                    analysisDate = analysis.createDate,
                    totalScore = analysis.score?.totalScore ?: 0,
                    versionLabel = "v$versionNumber (${analysis.createDate.toLocalDate()})",
                )
        }
    }
}
