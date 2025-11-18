package com.backend.domain.community.dto.request

data class SearchRepositoryDTO(
    private val type: String,
    private val content: String
) {}