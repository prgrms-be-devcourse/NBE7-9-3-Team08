package com.backend.domain.user.dto

@JvmRecord
data class LoginResponse(
    val user: UserDto
)