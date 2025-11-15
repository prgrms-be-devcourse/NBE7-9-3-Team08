package com.backend.domain.repository.dto.response.github

// commits 응답용 DTO
data class CommitResponse(
    val commit: CommitDetails?
) {
    data class CommitDetails(
        val message: String?,
        val author: AuthorDetails?
    )

    data class AuthorDetails(
        val date: String?
    )
}
