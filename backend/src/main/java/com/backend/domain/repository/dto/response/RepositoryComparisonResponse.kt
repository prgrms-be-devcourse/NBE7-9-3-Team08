package com.backend.domain.repository.dto.response

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.repository.entity.Repositories
import java.time.LocalDateTime

data class RepositoryComparisonResponse(
    val repositoryId: Long,
    val name: String,
    val htmlUrl: String,
    val languages: List<String>,
    val latestAnalysis: AnalysisInfo
) {

    data class AnalysisInfo(
        val analysisId: Long,
        val analyzedAt: LocalDateTime,
        val scores: CategoryScores
    ) {
        companion object {
            fun from(analysis: AnalysisResult): AnalysisInfo {
                val score = requireNotNull(analysis.score) {
                    "Analysis result must have a score"
                }

                return AnalysisInfo(
                    analysisId = analysis.id!!,
                    analyzedAt = analysis.createDate!!,
                    scores = CategoryScores(
                        readme = score.readmeScore,
                        test = score.testScore,
                        commit = score.commitScore,
                        cicd = score.cicdScore,
                        total = score.totalScore
                    )
                )
            }
        }
    }

    data class CategoryScores(
        val readme: Int,
        val test: Int,
        val commit: Int,
        val cicd: Int,
        val total: Int
    )

    companion object {
        @JvmStatic
        fun from(
            repository: Repositories,
            latestAnalysis: AnalysisResult
        ): RepositoryComparisonResponse {

            val languages = repository.getLanguages()
                .map { it.language.name }

            return RepositoryComparisonResponse(
                repositoryId = repository.id!!,
                name = repository.name,
                htmlUrl = repository.htmlUrl,
                languages = languages,
                latestAnalysis = AnalysisInfo.from(latestAnalysis)
            )
        }
    }
}
