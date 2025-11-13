package com.backend.domain.user.dto

import com.backend.domain.user.entity.User

@JvmRecord
data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val imageUrl: String?
) {
    constructor(user: User) : this(
        user.id,
        user.email,
        user.name,
        user.imageUrl
    )
}
