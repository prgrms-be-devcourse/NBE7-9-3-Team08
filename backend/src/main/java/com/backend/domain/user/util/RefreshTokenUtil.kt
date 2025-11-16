package com.backend.domain.user.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
@RequiredArgsConstructor
class RefreshTokenUtil(
    private val redisUtil: RedisUtil
) {


    @Value("\${jwt.secret}")
    private lateinit var  secretKey: String

    @Value("\${jwt.refresh-token-expiration-in-milliseconds}")
    private var refreshTokenMilliSeconds: Long = 0L

    //SecretKey를 Base64로 인코딩하여 SecretKey객체로 변환
    private lateinit var key: SecretKey

    //key값 초기화
    @PostConstruct //의존성 주입이 될때 딱 1번만 실행되기 때문에 key값은 이후로 변하지 않음
    fun init() {
        this.key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
        println(key)
    }

    //refreshToken생성
    fun createToken(userId: Long): String {
        val claims: MutableMap<String, Any> = mutableMapOf()
        claims[Claims.SUBJECT] = userId.toString() // Claims.SUBJECT = "sub"이다.
        val jti = UUID.randomUUID().toString()
        claims[Claims.ID] = jti //Claims.ID = "jti"

        val now = Date()
        println("현재 시간 : ${now}")
        val expiration = Date(now.time + refreshTokenMilliSeconds)
        println("만료 시간 : ${expiration}")

        val refreshToken = Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()

        redisUtil.setData(jti, refreshToken, refreshTokenMilliSeconds / 1000L)

        return refreshToken
    }
}
