package com.backend.global.response

import com.backend.global.exception.ErrorCode
import lombok.AllArgsConstructor
import lombok.Getter

@AllArgsConstructor
@Getter
class ApiResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null
) {
    companion object {

        @JvmStatic
        fun <T> success(data: T?): ApiResponse<T> =
            ApiResponse(
                code = ResponseCode.OK.code,
                message = ResponseCode.OK.message,
                data = data
            )

        @JvmStatic
        fun <T> success(): ApiResponse<T> =
            ApiResponse(
                code = ResponseCode.OK.code,
                message = ResponseCode.OK.message,
                data = null
            )

        @JvmStatic
        fun <T> error(code: ResponseCode): ApiResponse<T> =
            ApiResponse(
                code = code.name,
                message = code.message,
                data = null
            )

        @JvmStatic
        fun <T> error(code: ResponseCode, message: String?): ApiResponse<T> =
            ApiResponse(
                code = code.name,
                message = message ?: code.message,
                data = null
            )

        @JvmStatic
        fun <T> error(code: ErrorCode): ApiResponse<T> =
            ApiResponse(
                code = code.name,
                message = code.message,
                data = null
            )
    }
}