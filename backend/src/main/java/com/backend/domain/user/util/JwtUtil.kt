package com.backend.domain.user.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil {
    @Value("\${jwt.secret}")
    private lateinit var secretKey: String

    @Value("\${jwt.access-token-expiration-in-milliseconds}")
    private var tokenValidityMilliSeconds: Int =0

    //SecretKey를 Base64로 인코딩하여 SecretKey객체로 변환
    private lateinit var key: SecretKey

    //key값 초기화
    @PostConstruct //의존성 주입이 될때 딱 1번만 실행되기 때문에 key값은 이후로 변하지 않음
    fun init() {
        this.key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
        println(key)
    }

    // AccessToken생성
    fun createToken(email: String, name: String, userId: Long): String {
        val claims: MutableMap<String, Any> = mutableMapOf()
        claims[Claims.SUBJECT] = email   // Claims.SUBJECT = "sub"이다.
        claims["name"] = name
        claims["userId"] = userId   // 수정: 추가

        val now = Date()
        println("현재 시간 : ${now}")
        val expiration = Date(now.time + tokenValidityMilliSeconds)
        println("만료 시간 : ${expiration}")
        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }

    //JWT 검증 및 Claims 추출
    fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    //만료시간을 계산하기 위해 서명 검증은 하지 않고 Claims만 읽어오기
    private fun getClaimsWithoutVerification(token: String): Claims {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) { //만료된 토큰인 경우에도 Claims반환
            return e.claims
        }
    }

    //블랙리스트 용 만료시간(현재 시간 - 만료시간) 추출
    fun getExpiration(token: String): Long {
        val claims = getClaimsWithoutVerification(token)

        val expiration = claims.expiration

        val now = Date().time
        val expirationTime = expiration.time
        val remain = expirationTime - now

        //만료되었을 경우(현재시간이 만료시간을 넘은경우) 0 리턴
        if (remain < 0) {
            return 0
        }
        return remain
    }

    //JWT에서 email추출
    //HttpServletRequest가 매개변수로 들어갑니다.
    fun getEmail(request: HttpServletRequest): String? {
        val jwt = getJwtToken(request)
        if (jwt != null) {
            val claims = getClaimsWithoutVerification(jwt)
            return claims.subject
        }
        return null
    }

    fun getName(request: HttpServletRequest): String? {
        val jwt = getJwtToken(request)
        if (jwt != null) {
            val claims = getClaimsWithoutVerification(jwt)
            return claims.get("name", String::class.java)
        }
        return null
    }

    // 추가
    fun getUserId(request: HttpServletRequest): Long? {
        val jwt = getJwtToken(request)
        if (jwt != null) {
            val claims = getClaimsWithoutVerification(jwt)
            val userIdNum = claims.get("userId", Number::class.java)
            return userIdNum?.toLong()
        }
        return null
    }

    fun getJwtToken(request: HttpServletRequest): String? {
        val cookies = request.cookies //
        if (cookies != null) {
            for (c in cookies) {
                if (c.name == "accessToken") {
                    return c.value
                }
            }
        }
        return null
    }
}
