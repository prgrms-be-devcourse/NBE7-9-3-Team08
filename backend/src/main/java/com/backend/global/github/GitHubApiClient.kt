package com.backend.global.github

import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class GitHubApiClient(
    private val githubWebClient: WebClient
) {

    // === 1. 기본 GET ===
    fun <T> get(uri: String, responseType: Class<T>, vararg uriVariables: Any): T {
        return executeRequest {
            githubWebClient.get()
                .uri(uri, *uriVariables)
                .retrieve()
                .toEntity(responseType)
                .map { response ->
                    checkRateLimit(response.headers)
                    response.body ?: throw BusinessException(ErrorCode.GITHUB_API_FAILED)
                }
        }
    }

    // === 2. Accept 헤더 지원 GET ===
    fun <T> getWithAcceptHeader(
        uri: String,
        responseType: Class<T>,
        acceptHeader: String,
        vararg uriVariables: Any
    ): T {
        return executeRequest {
            githubWebClient.get()
                .uri(uri, *uriVariables)
                .header("Accept", acceptHeader)
                .retrieve()
                .toEntity(responseType)
                .map { response ->
                    checkRateLimit(response.headers)
                    response.body ?: throw BusinessException(ErrorCode.GITHUB_API_FAILED)
                }
        }
    }

    // === 3. Raw GET (README Base64 디코딩) ===
    fun getRaw(uri: String, vararg uriVariables: Any): String {
        return executeRequest {
            githubWebClient.get()
                .uri(uri, *uriVariables)
                .header("Accept", "application/vnd.github.raw")
                .retrieve()
                .toEntity(String::class.java)
                .map { response ->
                    checkRateLimit(response.headers)
                    response.body ?: ""
                }
        }
    }

    // === 4. 리스트 GET ===
    fun <T> getList(uri: String, elementType: Class<T>, vararg uriVariables: Any): List<T> {
        return executeRequest {
            githubWebClient.get()
                .uri(uri, *uriVariables)
                .retrieve()
                .toEntityList(elementType)
                .map { response ->
                    checkRateLimit(response.headers)
                    response.body ?: emptyList()
                }
        }
    }

    // === 공통 실행 함수 ===
    private fun <T> executeRequest(block: () -> Mono<T>): T {
        return block()
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                handleWebClientError(ex)
            }
            .block()!!
    }

    // === GitHub API 에러 처리 ===
    private fun <T> handleWebClientError(ex: WebClientResponseException): Mono<T> {
        log.error("GitHub API 호출 실패: {}", ex.message)

        val status = HttpStatus.valueOf(ex.statusCode.value())

        return when {
            status.is4xxClientError -> handle4xx(status)
            status.is5xxServerError -> Mono.error(BusinessException(ErrorCode.GITHUB_API_SERVER_ERROR))
            else -> Mono.error(BusinessException(ErrorCode.GITHUB_API_FAILED))
        }
    }


    private fun <T> handle4xx(status: HttpStatus): Mono<T> {
        return when (status) {
            HttpStatus.BAD_REQUEST -> Mono.error(BusinessException(ErrorCode.GITHUB_API_FAILED))
            HttpStatus.UNAUTHORIZED -> Mono.error(BusinessException(ErrorCode.GITHUB_INVALID_TOKEN))
            HttpStatus.FORBIDDEN -> Mono.error(BusinessException(ErrorCode.FORBIDDEN))
            HttpStatus.NOT_FOUND -> Mono.error(BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND))
            HttpStatus.GONE -> Mono.error(BusinessException(ErrorCode.GITHUB_API_FAILED))
            HttpStatus.UNPROCESSABLE_ENTITY -> Mono.error(BusinessException(ErrorCode.GITHUB_API_FAILED))
            HttpStatus.TOO_MANY_REQUESTS -> Mono.error(BusinessException(ErrorCode.GITHUB_RATE_LIMIT_EXCEEDED))
            else -> Mono.error(BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND))
        }
    }

    // === Rate Limit 체크 ===
    private fun checkRateLimit(headers: HttpHeaders) {
        val remainingStr = headers["X-RateLimit-Remaining"]?.firstOrNull()
        val resetStr = headers["X-RateLimit-Reset"]?.firstOrNull()

        if (remainingStr == null || resetStr == null) {
            log.debug("Rate Limit 헤더를 찾을 수 없습니다.")
            return
        }

        runCatching {
            val remaining = remainingStr.toInt()
            val resetTime = resetStr.toLong()
            val currentTime = System.currentTimeMillis() / 1000
            val minutes = (resetTime - currentTime) / 60

            when {
                remaining <= RATE_LIMIT_CRITICAL_THRESHOLD ->
                    log.error("🚨 GitHub API Rate Limit - 남은 요청: {}, 초기화까지: {}분", remaining, minutes)
                remaining <= RATE_LIMIT_WARNING_THRESHOLD ->
                    log.warn("⚠️ GitHub API Rate Limit - 남은 요청: {}, 초기화까지: {}분", remaining, minutes)
                else ->
                    log.info("GitHub API Rate Limit - 남은 요청: {}, 초기화까지: {}분", remaining, minutes)
            }
        }.onFailure {
            log.error("Rate Limit 헤더 파싱 실패", it)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GitHubApiClient::class.java)
        private const val RATE_LIMIT_WARNING_THRESHOLD = 100
        private const val RATE_LIMIT_CRITICAL_THRESHOLD = 10
    }
}
