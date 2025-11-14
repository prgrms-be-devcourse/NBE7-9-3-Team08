package com.backend.global.exception

import com.backend.global.response.ApiResponse
import com.backend.global.response.ResponseCode
import jakarta.servlet.http.HttpServletRequest
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.io.IOException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 요청 DTO @Valid 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<MutableMap<String, String>>> {

        // 필드별로 에러 메시지를 Map으로 수집
        val errors: MutableMap<String, String> = mutableMapOf()

        ex.bindingResult.fieldErrors.forEach { error: FieldError ->
            val field = error.field
            val message = error.defaultMessage ?: "검증에 실패했습니다"

            // 동일 필드에 여러 에러 발생 시 ", "로 연결
            errors.merge(field, message) { existing, new -> "$existing, $new" }
        }

        log.warn("Validation failed: {}", errors)

        val response = ApiResponse(
            ResponseCode.BAD_REQUEST.code,
            ResponseCode.BAD_REQUEST.message,
            errors
        )

        return ResponseEntity(
            response,
            ResponseCode.BAD_REQUEST.status
        )
    }

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException
    ): ResponseEntity<ApiResponse<Void>> {

        val errorCode = ex.errorCode

        // HTTP 상태 코드에 따라 로깅 레벨 구분
        if (errorCode.status.is5xxServerError()) {
            // 5xx: 서버 오류 - error 레벨 (스택 트레이스 포함)
            log.error(
                "Business exception occurred: code={}, message={}",
                errorCode.code,
                errorCode.message,
                ex
            )
        } else {
            // 4xx: 클라이언트 오류 - warn 레벨 (스택 트레이스 제외)
            log.warn(
                "Business exception occurred: code={}, message={}",
                errorCode.code,
                errorCode.message
            )
        }

        return ResponseEntity(ApiResponse.error(errorCode), errorCode.status)
    }

    /**
     * SSE 연결 종료 관련 예외 무시
     */
    @ExceptionHandler(IOException::class, ClientAbortException::class)
    fun handleClientAbort(ex: Exception) {
        log.warn("⚠️ SSE 연결 종료로 인한 IOException 무시: {}", ex.message)
    }

    /**
     * 그 외 예기치 않은 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Void>> {
        val accept = request.getHeader("Accept")

        // SSE라면 예외 무시
        if (accept?.contains("text/event-stream") == true) {
            log.warn("⚠️ SSE 요청 중 발생한 예외 무시: {}", ex.message)
            return ResponseEntity.ok().build()
        }

        log.error("Unexpected exception occurred", ex)
        val response = ApiResponse.error<Void>(ErrorCode.INTERNAL_ERROR)

        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}