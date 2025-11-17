package com.backend.domain.repository.dto.response.github

// issue 응답용 DTO
data class IssueResponse(
    val number: Long?,
    val title: String?,
    val state: String?,
    val created_at: String?,
    val closed_at: String?,
    val pull_request: PullRequest?
) {
    data class PullRequest(val url: String?)

    val isPureIssue: Boolean
        get() = pull_request == null

    val isClosed: Boolean
        get() = state == "closed"
}
