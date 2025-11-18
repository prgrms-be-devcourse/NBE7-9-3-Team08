package com.backend.global.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ResponseCodeTest {

    @Test
    @DisplayName("ResponseCode의 필드들은 모두 null이 아니어야 한다")
    fun `ResponseCode의 필드들은 모두 null이 아니어야 한다`() {
        ResponseCode.values().forEach { code ->
            assertThat(code.code).isNotNull()
            assertThat(code.message).isNotNull()
            assertThat(code.status).isNotNull()
        }
    }

    @Test
    @DisplayName("OK ResponseCode 필드값 검증")
    fun `OK ResponseCode 필드값 검증`() {
        val ok = ResponseCode.OK

        assertThat(ok.code).isEqualTo("200")
        assertThat(ok.status).isEqualTo(HttpStatus.OK)
        assertThat(ok.message).isEqualTo("정상적으로 완료되었습니다.")
    }

    @Test
    @DisplayName("BAD_REQUEST ResponseCode 필드값 검증")
    fun `BAD_REQUEST ResponseCode 필드값 검증`() {
        val bad = ResponseCode.BAD_REQUEST

        assertThat(bad.code).isEqualTo("400")
        assertThat(bad.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(bad.message).isEqualTo("잘못된 요청입니다.")
    }
}
