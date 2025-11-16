package com.backend.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("BusinessException 생성 시 ErrorCode가 정확히 전달되어야 한다")
    void businessExceptionShouldReturnErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.INVALID_INPUT_VALUE);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}
