package com.backend.domain.community.dto.request

data class CommentRequestDTO(
    val memberId: Long,
    val comment: String
){}