package com.backend.domain.repository.dto.response.github

// Pull Request 응답용 DTO
data class PullRequestResponse(
    val number: Long?,
    val title: String?,
    val state: String?,
    val created_at: String?,
    val merged_at: String?
)
