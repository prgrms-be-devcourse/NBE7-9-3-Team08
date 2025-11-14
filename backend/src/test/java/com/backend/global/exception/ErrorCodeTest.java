package com.backend.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("ErrorCode 필드 값 검증")
    void errorCodeValues() {
        ErrorCode code = ErrorCode.GITHUB_API_FAILED;

        assertThat(code.getCode()).isEqualTo("G006");
        assertThat(code.getStatus().value()).isEqualTo(400);
        assertThat(code.getMessage()).isEqualTo("GitHub API 응답에 실패했습니다.");
    }

    @Test
    @DisplayName("모든 ErrorCode가 null 없이 초기화되어야 한다")
    void noNullFieldsInErrorCode() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ec.getCode()).isNotNull();
            assertThat(ec.getMessage()).isNotNull();
            assertThat(ec.getStatus()).isNotNull();
        }
    }
}
