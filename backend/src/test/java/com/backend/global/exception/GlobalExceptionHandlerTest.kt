package com.backend.global.exception

import com.backend.global.response.ApiResponse
import com.backend.global.response.ResponseCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import java.io.IOException

class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    @Test
    @DisplayName("단일 필드 Validation 실패 처리")
    fun `단일 필드 Validation 실패 처리`() {
        // given
        val bindingResult = mock(BindingResult::class.java)
        val fieldError = FieldError("testObject", "name", "name은 필수값입니다.")
        given(bindingResult.fieldErrors).willReturn(listOf(fieldError))

        val methodParameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        // when
        val response: ResponseEntity<ApiResponse<MutableMap<String, String>>> =
            handler.handleMethodArgumentNotValid(ex)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        val body = response.body!!
        val data = body.data ?: error("data가 null이면 안 됩니다.")

        assertThat(body.code).isEqualTo(ResponseCode.BAD_REQUEST.code)
        assertThat(data["name"]).isEqualTo("name은 필수값입니다.")
    }

    @Test
    @DisplayName("다중 필드 Validation 실패 처리")
    fun `다중 필드 Validation 실패 처리`() {
        // given
        val bindingResult = mock(BindingResult::class.java)
        val errors = listOf(
            FieldError("testObject", "name", "이름은 필수값입니다."),
            FieldError("testObject", "email", "이메일은 필수값입니다."),
            FieldError("testObject", "password", "비밀번호는 8자 이상이어야 합니다.")
        )
        given(bindingResult.fieldErrors).willReturn(errors)

        val methodParameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        // when
        val response: ResponseEntity<ApiResponse<MutableMap<String, String>>> =
            handler.handleMethodArgumentNotValid(ex)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val body = response.body!!
        assertThat(body.data)
            .containsEntry("name", "이름은 필수값입니다.")
            .containsEntry("email", "이메일은 필수값입니다.")
            .containsEntry("password", "비밀번호는 8자 이상이어야 합니다.")
    }

    @Test
    @DisplayName("동일 필드 다중 에러 메시지 병합")
    fun `동일 필드 다중 에러 메시지 병합`() {
        // given
        val bindingResult = mock(BindingResult::class.java)
        val errors = listOf(
            FieldError("testObject", "email", "이메일은 필수값입니다."),
            FieldError("testObject", "email", "이메일 형식이 올바르지 않습니다.")
        )
        given(bindingResult.fieldErrors).willReturn(errors)

        val methodParameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        // when
        val response: ResponseEntity<ApiResponse<MutableMap<String, String>>> =
            handler.handleMethodArgumentNotValid(ex)

        // then
        val body = response.body!!
        val data = body.data ?: error("data가 null이면 안 됩니다.")

        val emailMessage = data["email"] ?: error("email 메시지가 존재해야 합니다.")

        assertThat(emailMessage).contains("이메일은 필수값입니다.")
        assertThat(emailMessage).contains("이메일 형식이 올바르지 않습니다.")
    }

    @Test
    @DisplayName("BusinessException 4xx 처리")
    fun `BusinessException 4xx 처리`() {
        // given
        val ex = BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

        // when
        val response: ResponseEntity<ApiResponse<Void>> = handler.handleBusinessException(ex)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        val body = response.body!!
        assertThat(body.code).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name)
        assertThat(body.message).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.message)
    }

    @Test
    @DisplayName("BusinessException 5xx 처리")
    fun `BusinessException 5xx 처리`() {
        // given
        val ex = BusinessException(ErrorCode.INTERNAL_ERROR)

        // when
        val response: ResponseEntity<ApiResponse<Void>> = handler.handleBusinessException(ex)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body!!
        assertThat(body.code).isEqualTo(ErrorCode.INTERNAL_ERROR.name)
    }

    @Test
    @DisplayName("IOException 처리 - void 반환 (SSE 연결 종료)")
    fun `IOException 처리 - void 반환 (SSE 연결 종료)`() {
        // given
        val ex = IOException("Connection reset by peer")

        // when & then (예외만 안 나면 통과)
        handler.handleClientAbort(ex)
    }

    @Test
    @DisplayName("SSE 요청 중 예외는 200 OK로 무시된다")
    fun `SSE 요청 중 예외는 200 OK로 무시된다`() {
        // given
        val ex = RuntimeException("SSE 중 예외")
        val request = MockHttpServletRequest().apply {
            addHeader("Accept", "text/event-stream")
        }

        // when
        val response: ResponseEntity<ApiResponse<Void>> = handler.handleException(ex, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()
    }

    @Test
    @DisplayName("일반 요청 중 예기치 않은 예외는 500으로 처리된다")
    fun `일반 요청 중 예기치 않은 예외는 500으로 처리된다`() {
        // given
        val ex = RuntimeException("알 수 없는 예외")
        val request = MockHttpServletRequest()

        // when
        val response: ResponseEntity<ApiResponse<Void>> = handler.handleException(ex, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body!!
        assertThat(body.code).isEqualTo(ErrorCode.INTERNAL_ERROR.name)
        assertThat(body.message).isEqualTo(ErrorCode.INTERNAL_ERROR.message)
    }
}
