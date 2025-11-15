package com.backend.domain.repository.dto.response.github

// readme 응답용 DTO
data class ReadmeResponse(
    val name: String?,
    val path: String?,
    val sha: String?,
    val size: Int?,
    val url: String?,
    val htmlUrl: String?,
    val downloadUrl: String?,
    val type: String?,
    val content: String?,
    val encoding: String?
)
