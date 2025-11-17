package com.backend.global.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BusinessExceptionTest {

    @Test
    @DisplayName("BusinessException 생성 시 ErrorCode가 정확히 전달되어야 한다")
    fun `BusinessException 생성 시 ErrorCode가 정확히 전달됨`() {
        val ex = BusinessException(ErrorCode.INVALID_INPUT_VALUE)

        assertThat(ex.errorCode).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        assertThat(ex.message).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.message)
    }
}
