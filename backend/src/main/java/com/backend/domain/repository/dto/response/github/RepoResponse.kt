package com.backend.domain.repository.dto.response.github

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class RepoResponse(
    val name: String?,
    @JsonProperty("full_name")
    val fullName: String?,
    val isPrivate: Boolean,
    val description: String?,

    @JsonProperty("html_url")
    val htmlUrl: String?,
    val language: String?,

    @JsonProperty("default_branch")
    val defaultBranch: String?,

    @JsonProperty("created_at")
    val createdAt: OffsetDateTime?,
    val size: Int?
)
