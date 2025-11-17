package com.backend.domain.community.dto.response

import java.time.LocalDateTime

data class SearchResultDTO(
    val id: Long?,
    val name: String,
    val description: String?,
    val htmlUrl: String,
    val userName: String?,
    val languages: List<String>,
    val createDate: LocalDateTime?
)
