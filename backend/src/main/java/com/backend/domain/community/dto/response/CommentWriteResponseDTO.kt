package com.backend.domain.community.dto.response

import com.backend.domain.community.entity.Comment
import java.time.LocalDateTime

data class CommentWriteResponseDTO(
    val commentId: Long?,
    val memberId: Long?,
    val comment: String,
    val createDate: LocalDateTime?
) {
    constructor(comment: Comment) : this(
        comment.id,
        comment.memberId,
        comment.comment,
        comment.createDate
    )
}