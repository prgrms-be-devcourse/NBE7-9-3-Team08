package com.backend.domain.user.controller

import com.backend.domain.user.dto.LoginResponse
import com.backend.domain.user.dto.UserDto
import com.backend.domain.user.service.EmailService
import com.backend.domain.user.service.JwtService
import com.backend.domain.user.service.UserService
import com.backend.domain.user.util.JwtUtil
import com.backend.domain.user.util.RefreshTokenUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import com.backend.global.response.ApiResponse
import com.backend.global.response.ResponseCode
import jakarta.mail.MessagingException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*

@RestController
class AuthController(
    private val emailService: EmailService,
    private val jwtService: JwtService,
    private val userService: UserService,
    private val jwtUtil: JwtUtil,
    private val refreshTokenUtil: RefreshTokenUtil
) {

    @Value("\${jwt.access-token-expiration-in-milliseconds}")
    private var tokenValidityMilliSeconds: Int = 0

    @Value("\${jwt.refresh-token-expiration-in-milliseconds}")
    private var refreshTokenValidityMilliSeconds: Long = 0

    @Value("\${jwt.cookie-secure}")
    private var cookieSecure: Boolean = false

    @Value("\${jwt.same-site}")
    private var sameSite: String = "Lax"

    // SameSite=None + Secure 옵션 적용
    private fun sameSiteHeader(name: String, value: String?, maxAge: Int): String {
        val secureFlag = if (cookieSecure) "Secure" else ""
        return "$name=$value; Path=/; Max-Age=$maxAge; HttpOnly; $secureFlag; SameSite=$sameSite".trim()
    }

    //입력받은 이메일에 인증코드를 보냅니다.
    data class SendRequest(
        val email: String
    )

    @PostMapping("/api/auth")
    @Throws(MessagingException::class)
    fun sendAuthCode(@RequestBody sendRequest: SendRequest): ApiResponse<String> {
        emailService.sendEmail(sendRequest.email)
        return ApiResponse.success("이메일 인증 코드 발송 성공")
    }


    //인증코드 검증
    data class VerifyRequest(
        val email: String,
        val code: String
    )

    @PostMapping("/api/verify")
    fun verifyAuthCode(@RequestBody request: VerifyRequest): ApiResponse<String> {
        return if (emailService.verifyAuthCode(request.email, request.code)) {
            //인증 성공시
            ApiResponse.success("이메일 인증 성공")
        } else {
            ApiResponse.error(ErrorCode.EMAIL_VERIFY_FAILED)
        }
    }


    //로그인
    data class LoginRequest(
        @field:NotBlank(message = "이메일은 필수 입력값 입니다.")
        @field:Email(message = "이메일 형식이 아닙니다.")
        val email: String,

        @field:NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        val password: String
    )

    @PostMapping("/api/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
        response: HttpServletResponse
    ): ApiResponse<LoginResponse> {
        val tokens: List<String> = jwtService.login(loginRequest.email, loginRequest.password)
        println("===== jwt, refreshToken 발급 ==========")
        val accessToken: String? = tokens.getOrNull(0)
        val refreshToken: String? = tokens.getOrNull(1)
        //토큰이 유효한지 확인
        if (accessToken == null || refreshToken == null) {
            return ApiResponse.error(ResponseCode.UNAUTHORIZED)
        }

        val accessTokenMaxAge = tokenValidityMilliSeconds / 1000
        var refreshTokenMaxAge = 0
        try {
            refreshTokenMaxAge = Math.toIntExact(refreshTokenValidityMilliSeconds / 1000)
        } catch (e: ArithmeticException) {
            throw BusinessException(ErrorCode.EXPIRATION_ERROR)
        }

        val accessCookie = Cookie("accessToken", accessToken)
        accessCookie.isHttpOnly = true
        accessCookie.secure = cookieSecure
        accessCookie.path = "/"
        accessCookie.maxAge = accessTokenMaxAge

        val refreshCookie = Cookie("refreshToken", refreshToken)
        refreshCookie.isHttpOnly = true
        refreshCookie.secure = cookieSecure
        refreshCookie.path = "/"
        refreshCookie.maxAge = refreshTokenMaxAge

        // SameSite 보완 헤더 추가
        response.addHeader("Set-Cookie", sameSiteHeader("accessToken", accessToken, accessTokenMaxAge))
        response.addHeader("Set-Cookie", sameSiteHeader("refreshToken", refreshToken, refreshTokenMaxAge))

        val user = userService.findByEmail(loginRequest.email)
        return ApiResponse.success(LoginResponse(UserDto(user)))
    }

    /**
     * 로그아웃
     */
    @PostMapping("/api/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ApiResponse<String> {
        val expired = 0
        //쿠키 만료 명령
        val accessCookie = Cookie("accessToken", null)
        accessCookie.isHttpOnly = true
        accessCookie.secure = cookieSecure
        accessCookie.path = "/"
        accessCookie.maxAge = expired

        val refreshCookie = Cookie("refreshToken", null)
        refreshCookie.isHttpOnly = true
        refreshCookie.secure = cookieSecure
        refreshCookie.path = "/"
        refreshCookie.maxAge = expired

        response.addHeader("Set-Cookie", sameSiteHeader("accessToken", "", expired))
        response.addHeader("Set-Cookie", sameSiteHeader("refreshToken", "", expired))

        //redis에 블랙리스트로 등록
        val jwtToken = getJwtToken(request)
        if (jwtToken != null) {
            val expiration = jwtUtil.getExpiration(jwtToken)
            if (expiration > 0) {
                jwtService.logout(jwtToken, expiration)
            }
        }

        return ApiResponse.success("로그아웃 되었습니다.")
    }


    fun getJwtToken(request: HttpServletRequest): String? {
        val cookies: Array<Cookie> = request.cookies ?: return null

        return cookies.firstOrNull{it.name == "accessToken"}?.value
    }

    //토큰 재발급
    //jwt가 만료되었을때 호출됨
    //@PostMapping("/api/reissue")
    @RequestMapping(value = ["/api/reissue"], method = [RequestMethod.POST, RequestMethod.GET])
    fun reissue(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ApiResponse<String>{
        println("재발급 api 진입")
        val refreshToken: String? = refreshTokenUtil.getRefreshToken(request)
        if(refreshToken.isNullOrBlank()){
            throw BusinessException(ErrorCode.REFRESH_TOKEN_ERROR)
        }
        println("refreshToken확인 완료")

        //서명, 만료일 확인
        if(refreshTokenUtil.refreshTokenValidation(refreshToken)){
            //사용자 식별
            val userId = refreshTokenUtil.getId(request)
            if(userId == 0L){
                println("사용자 식별 실패")
                throw BusinessException(ErrorCode.REFRESH_TOKEN_ERROR)
            }

            //토큰 쌍 발급 + redis 업데이트
            val tokens: List<String> = jwtService.reissue(userId, refreshToken)
            println("===== jwt, refreshToken 재발급 ==========")
            val newAccessToken: String? = tokens.getOrNull(0)
            val newRefreshToken: String? = tokens.getOrNull(1)

            //토큰이 유효한지 확인
            if (newAccessToken == null || newRefreshToken == null) {
                return ApiResponse.error(ResponseCode.UNAUTHORIZED)
            }

            val accessTokenMaxAge = tokenValidityMilliSeconds / 1000

            //쿠키 업데이트 및 응답
            val accessCookie = Cookie("accessToken", newAccessToken)
            accessCookie.isHttpOnly = true // JavaScript 접근 방지 (XSS 공격 방어)
            accessCookie.secure = cookieSecure //HTTPS 통신에서만 전송
            accessCookie.path = "/"
            accessCookie.maxAge = accessTokenMaxAge //쿠키는 초단위

            val refreshCookie = Cookie("refreshToken", newRefreshToken)
            refreshCookie.isHttpOnly = true // JavaScript 접근 방지 (XSS 공격 방어)
            refreshCookie.secure = cookieSecure //HTTPS 통신에서만 전송
            refreshCookie.path = "/"
            var refreshTokenMaxAge = 0
            try {
                refreshTokenMaxAge = Math.toIntExact(refreshTokenValidityMilliSeconds / 1000)
            } catch (e: ArithmeticException) {
                throw BusinessException(ErrorCode.EXPIRATION_ERROR)
            }

            refreshCookie.maxAge = refreshTokenMaxAge

            response.addHeader("Set-Cookie", sameSiteHeader("accessToken", newAccessToken, accessTokenMaxAge))
            response.addHeader("Set-Cookie", sameSiteHeader("refreshToken", newRefreshToken, refreshTokenMaxAge))

        }else{
            throw BusinessException(ErrorCode.REFRESH_TOKEN_ERROR)
        }

        return ApiResponse.success("token 재발급 완료")
    }
}
