package com.backend.global.response

import com.backend.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ApiResponseTest {

    @Test
    @DisplayName("success(data) 호출 시 ResponseCode.OK 기반 응답 생성")
    fun `success(data) 호출 시 OK 응답 생성`() {
        val data = "test-value"

        val response = ApiResponse.success(data)

        assertThat(response.code).isEqualTo(ResponseCode.OK.code)
        assertThat(response.message).isEqualTo(ResponseCode.OK.message)
        assertThat(response.data).isEqualTo(data)
    }

    @Test
    @DisplayName("success() 호출 시 data=null이고 OK 코드가 반환됨")
    fun `success() 호출 시 data null 반환`() {
        val response: ApiResponse<Void> = ApiResponse.success()

        assertThat(response.code).isEqualTo(ResponseCode.OK.code)
        assertThat(response.message).isEqualTo(ResponseCode.OK.message)
        assertThat(response.data).isNull()
    }

    @Test
    @DisplayName("error(ResponseCode) 호출 시 코드가 enum name()으로 반환됨")
    fun `error(ResponseCode) 호출 시 enum name 반환`() {
        val response: ApiResponse<Void> = ApiResponse.error(ResponseCode.BAD_REQUEST)

        assertThat(response.code).isEqualTo(ResponseCode.BAD_REQUEST.name)
        assertThat(response.message).isEqualTo(ResponseCode.BAD_REQUEST.message)
        assertThat(response.data).isNull()
    }

    @Test
    @DisplayName("error(ResponseCode, message) 호출 시 message override 가능")
    fun `error(ResponseCode, message) 호출 시 custom message 반영`() {
        val customMessage = "사용자 지정 에러 메시지"

        val response: ApiResponse<Void> =
            ApiResponse.error(ResponseCode.UNAUTHORIZED, customMessage)

        assertThat(response.code).isEqualTo(ResponseCode.UNAUTHORIZED.name)
        assertThat(response.message).isEqualTo(customMessage)
        assertThat(response.data).isNull()
    }

    @Test
    @DisplayName("error(ErrorCode) 호출 시 ErrorCode.name()이 code로 반환됨")
    fun `error(ErrorCode) 호출 시 ErrorCode name 반환`() {
        val response: ApiResponse<Void> = ApiResponse.error(ErrorCode.GITHUB_API_FAILED)

        assertThat(response.code).isEqualTo(ErrorCode.GITHUB_API_FAILED.name)
        assertThat(response.message).isEqualTo(ErrorCode.GITHUB_API_FAILED.message)
        assertThat(response.data).isNull()
    }
}
