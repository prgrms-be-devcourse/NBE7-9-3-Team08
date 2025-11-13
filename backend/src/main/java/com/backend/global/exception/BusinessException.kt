package com.backend.global.exception

class BusinessException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)