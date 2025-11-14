package com.backend.global.exception;

import com.backend.global.response.ApiResponse;
import com.backend.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("단일 필드 Validation 실패 처리")
    void testSingleFieldValidation() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("testObject", "name", "name은 필수값입니다.");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // when
        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleMethodArgumentNotValid(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.BAD_REQUEST.getCode());
        assertThat(response.getBody().getData()).containsEntry("name", "name은 필수값입니다.");
    }

    @Test
    @DisplayName("다중 필드 Validation 실패 처리")
    void testMultipleFieldValidation() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> errors = List.of(
                new FieldError("testObject", "name", "이름은 필수값입니다."),
                new FieldError("testObject", "email", "이메일은 필수값입니다."),
                new FieldError("testObject", "password", "비밀번호는 8자 이상이어야 합니다.")
        );
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // when
        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleMethodArgumentNotValid(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData())
                .containsEntry("name", "이름은 필수값입니다.")
                .containsEntry("email", "이메일은 필수값입니다.")
                .containsEntry("password", "비밀번호는 8자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("동일 필드 다중 에러 메시지 병합")
    void testSameFieldMultipleErrors() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> errors = List.of(
                new FieldError("testObject", "email", "이메일은 필수값입니다."),
                new FieldError("testObject", "email", "이메일 형식이 올바르지 않습니다.")
        );
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // when
        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleMethodArgumentNotValid(ex);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().get("email"))
                .contains("이메일은 필수값입니다.")
                .contains("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("BusinessException 4xx 처리")
    void testBusinessException4xx() {
        // given
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("BusinessException 5xx 처리")
    void testBusinessException5xx() {
        // given
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR);

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
    }

    @Test
    @DisplayName("IOException 처리 - void 반환 (SSE 연결 종료)")
    void testIOException() {
        // given
        IOException ex = new IOException("Connection reset by peer");

        // when & then
        // void 반환이므로 예외가 발생하지 않으면 성공
        handler.handleClientAbort(ex);
    }

    @Test
    @DisplayName("SSE 요청 중 일반 예외 처리")
    void testSseRequestException() {
        // given
        RuntimeException ex = new RuntimeException("SSE 중 예외 발생");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/event-stream");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("일반 Exception 처리 → INTERNAL_ERROR")
    void testGeneralException() {
        // given
        RuntimeException ex = new RuntimeException("알 수 없는 예외");
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();

        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
    }
}