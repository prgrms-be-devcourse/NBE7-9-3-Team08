package com.backend.global.response;

import com.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("success(data) 호출 시 ResponseCode.OK 기반 응답 생성")
    void successWithData() {
        String data = "test-value";

        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.getCode()).isEqualTo(ResponseCode.OK.getCode());
        assertThat(response.getMessage()).isEqualTo(ResponseCode.OK.getMessage());
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("success() 호출 시 data=null이고 OK 코드가 반환됨")
    void successWithoutData() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.getCode()).isEqualTo(ResponseCode.OK.getCode());
        assertThat(response.getMessage()).isEqualTo(ResponseCode.OK.getMessage());
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(ResponseCode) 호출 시 코드가 enum name()으로 반환됨")
    void errorWithResponseCode() {
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.BAD_REQUEST);

        // ResponseCode.BAD_REQUEST.name() → "BAD_REQUEST"
        assertThat(response.getCode()).isEqualTo(ResponseCode.BAD_REQUEST.name());
        assertThat(response.getMessage()).isEqualTo(ResponseCode.BAD_REQUEST.getMessage());
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(ResponseCode, message) 호출 시 message override 가능")
    void errorWithResponseCodeAndCustomMessage() {
        String customMessage = "사용자 지정 에러 메시지";

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.UNAUTHORIZED, customMessage);

        assertThat(response.getCode()).isEqualTo(ResponseCode.UNAUTHORIZED.name());
        assertThat(response.getMessage()).isEqualTo(customMessage);
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode) 호출 시 ErrorCode.name()이 code로 반환됨")
    void errorWithErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.GITHUB_API_FAILED);

        assertThat(response.getCode()).isEqualTo(ErrorCode.GITHUB_API_FAILED.name());
        assertThat(response.getMessage()).isEqualTo(ErrorCode.GITHUB_API_FAILED.getMessage());
        assertThat(response.getData()).isNull();
    }
}
