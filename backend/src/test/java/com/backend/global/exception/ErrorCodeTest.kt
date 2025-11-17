package com.backend.global.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ErrorCodeTest {

    @Test
    @DisplayName("ErrorCode 필드 값 검증")
    fun `ErrorCode 필드 값 검증`() {
        val code = ErrorCode.GITHUB_API_FAILED

        assertThat(code.code).isEqualTo("G006")
        assertThat(code.status.value()).isEqualTo(400)
        assertThat(code.message).isEqualTo("GitHub API 응답에 실패했습니다.")
    }

    @Test
    @DisplayName("모든 ErrorCode가 null 없이 초기화되어야 한다")
    fun `모든 ErrorCode 필드는 null 이 아니어야 한다`() {
        ErrorCode.values().forEach { ec ->
            assertThat(ec.code).isNotNull()
            assertThat(ec.message).isNotNull()
            assertThat(ec.status).isNotNull()
        }
    }
}
