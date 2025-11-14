package com.backend.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCodeTest {

    @Test
    @DisplayName("ResponseCode의 필드들은 모두 null이 아니어야 한다")
    void responseCodeFieldsNotNull() {
        for (ResponseCode code : ResponseCode.values()) {
            assertThat(code.getCode()).isNotNull();
            assertThat(code.getMessage()).isNotNull();
            assertThat(code.getStatus()).isNotNull();
        }
    }

    @Test
    @DisplayName("OK ResponseCode 필드값 검증")
    void validateOkCode() {
        ResponseCode ok = ResponseCode.OK;

        assertThat(ok.getCode()).isEqualTo("200");
        assertThat(ok.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getMessage()).isEqualTo("정상적으로 완료되었습니다.");
    }

    @Test
    @DisplayName("BAD_REQUEST ResponseCode 필드값 검증")
    void validateBadRequest() {
        ResponseCode bad = ResponseCode.BAD_REQUEST;

        assertThat(bad.getCode()).isEqualTo("400");
        assertThat(bad.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getMessage()).isEqualTo("잘못된 요청입니다.");
    }
}
