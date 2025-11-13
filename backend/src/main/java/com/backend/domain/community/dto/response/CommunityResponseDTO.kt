package com.backend.domain.community.dto.response

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.entity.RepositoryLanguage
import java.time.LocalDateTime
import java.util.stream.Collectors

data class CommunityResponseDTO(
    val userName: String,
    val userImage: String,
    val repositoryName: String,
    val repositoryId: Long,
    val summary: String,
    val description: String,
    val language: List<String>,
    val totalScore: Int,
    val createDate: LocalDateTime?,
    val publicStatus: Boolean,
    val htmlUrl: String?
) {
    constructor(repositories: Repositories, analysis: AnalysisResult, score: Score) : this(
        repositories.getUser().getName(),
        repositories.getUser().getImageUrl(),
        repositories.getName(),
        repositories.getId(),
        analysis.getSummary(),
        repositories.getDescription(),
        repositories.getLanguages().stream()
            .map<String> { language: RepositoryLanguage? -> language!!.getLanguage().name }
            .collect(Collectors.toList()),
        score.getTotalScore(),
        analysis.getCreateDate(),
        repositories.isPublicRepository(),
        repositories.getHtmlUrl()
    )
}
