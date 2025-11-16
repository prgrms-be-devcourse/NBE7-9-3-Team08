package com.backend.domain.community.dto.response

import com.backend.domain.community.entity.Comment
import com.backend.domain.user.entity.User
import java.time.LocalDateTime

data class CommentResponseDTO(
    val commentId: Long?,
    val memberId: Long?,
    val name: String,
    val comment: String,
    val createDate: LocalDateTime?,
    val deleted: Boolean,
    val userImage: String?
) {
    constructor(comment: Comment, user: User) : this(
        comment.id,
        comment.memberId,
        user.name,
        comment.comment,
        comment.createDate,
        comment.deleted,
        user.imageUrl
    )
}