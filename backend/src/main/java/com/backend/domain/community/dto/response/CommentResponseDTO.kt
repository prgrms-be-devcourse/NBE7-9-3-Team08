package com.backend.domain.community.dto.response

import com.backend.domain.community.entity.Comment
import java.time.LocalDateTime

data class CommentResponseDTO(
    val commentId: Long?,
    val memberId: Long,
    val name: String,
    val comment: String,
    val createDate: LocalDateTime,
    val deleted: Boolean
) {
    constructor(comment: Comment, userName: String) : this(
        comment.id,
        comment.memberId,
        userName,
        comment.comment,
        comment.getCreateDate(),
        comment.deleted
    )
}
