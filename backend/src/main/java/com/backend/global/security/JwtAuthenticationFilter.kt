package com.backend.global.security

import com.backend.domain.user.service.JwtService
import com.backend.domain.user.util.JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.time.LocalDateTime

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    @JvmRecord
    private data class ExcludedRequest(val path: String, val method: String?)

    // 요청 경로 + 메서드가 일치하는 경우 필터 스킵
    private val pathMatcher = AntPathMatcher()

    //  JWT 검증이 필요 없는 URL
    private val excludedRequests = listOf(
        // 개발 도구 (모든 메서드 허용)
        ExcludedRequest("/h2-console/**", null),
        ExcludedRequest("/swagger-ui/**", null),
        ExcludedRequest("/v3/api-docs/**", null),
        ExcludedRequest("/swagger-resources/**", null),
        ExcludedRequest("/webjars/**", null),

        // 인증 관련 API
        ExcludedRequest("/api/login", "POST"),
        ExcludedRequest("/api/auth", "POST"),
        ExcludedRequest("/api/verify", "POST"),
        ExcludedRequest("/api/user", "POST"),
        ExcludedRequest("/api/reissue", null),

        // 커뮤니티 관련 API
        ExcludedRequest("/api/community/**", null),

        // 분석 및 리포지토리 관련 API
        ExcludedRequest("/api/analysis/repositories/{repositoriesId}", "GET"),
        ExcludedRequest("/api/analysis/repositories/{repositoryId}/results/{analysisId}", "GET"),
        ExcludedRequest("/api/analysis/stream/**", "GET"),
        ExcludedRequest("/api/repositories/**", null),
        ExcludedRequest("/api/ai/complete/**", null)
    )

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestURI = request.requestURI
        val method = request.method

        val excluded = excludedRequests.any { ex ->
                //요청 url이 List의 url과 일치하는지 확인(/**도 지원)
                val pathMatches = pathMatcher.match(ex.path, requestURI)

                // 메서드가 null (모든 메서드)이거나, 메서드가 일치하는 경우
                val methodMatches = ex.method == null || ex.method.equals(method, ignoreCase = true)
                pathMatches && methodMatches
            }
        if (excluded) {
            filterChain.doFilter(request, response)
            return
        }

        var token: String? = extractTokenFromCookie(request)

        //token이 null이거나 비어있다면 JWT가 입력되지 않은것으로 판단
        if (token == null || token.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token error : JWT가 입력되지 않았습니다.")
            return
        }

        //토큰이 있다면 검증 및 인증

        //블랙리스트를 조회하여 토큰이 무효화 되었는지 확인
        if (jwtService.isBlacklisted(token)) {
            sendErrorResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Token Expired : 토큰이 무효되어 있습니다.(로그아웃 상태입니다.)"
            )
            return  //요청 차단
        }
        try {
            val claims: Claims = jwtUtil.parseClaims(token)
            val email: String? = claims.subject //"sub"값 가져오기

            //추출된 정보로 Spring Security 인증 객체 생성 (파싱)
            var authentication: Authentication? = null
            if (email != null) {
                //나중에 권한을 추가하면 Collections.emptyList()대신 넣을것
                authentication = UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    AuthorityUtils.NO_AUTHORITIES  //권한이 없을 경우 빈 권한 리스트를 제공
                )
            }

            // SecurityContextHolder에 인증 정보 저장
            SecurityContextHolder.getContext().authentication = authentication
        } catch (e: ExpiredJwtException) {
            // 토큰 만료 시
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token Expired : 토큰의 유효기간이 지났습니다.")
            return
        } catch (e: JwtException) {
            // 서명 불일치, 토큰 형식 오류 등
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Invalid Token : 올바른 토큰 값이 아닙니다.")
            return
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromCookie(request: HttpServletRequest): String? {
        val cookies : Array<Cookie> = request.cookies ?: return null

        return cookies.firstOrNull{it.name == "accessToken"}?.value
    }

    //에러 발생시 메세지 처리
    @Throws(IOException::class)
    private fun sendErrorResponse(response: HttpServletResponse, status: Int, message: String) {
        response.contentType = "application/json;charset=UTF-8"
        response.status = status

        val errorDetails: MutableMap<String, Any> = mutableMapOf()
        errorDetails["status"] = status
        errorDetails["error"] = "Authentication Failed"
        errorDetails["message"] = message
        errorDetails["timestamp"] = LocalDateTime.now().toString()

        //Map을 JSON 문자열로 변환하여 응답 본문에 작성
        val objectMapper = ObjectMapper()
        response.writer.write(objectMapper.writeValueAsString(errorDetails))
    }
}
