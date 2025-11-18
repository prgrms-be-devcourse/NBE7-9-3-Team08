package com.backend.domain.user.service

import com.backend.domain.user.util.JwtUtil
import com.backend.global.security.JwtAuthenticationFilter
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.*


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("redis")
class JwtServiceTest {
    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var jwtService: JwtService

    @Value("\${jwt.secret}")
    private lateinit var secretKey: String

    @Value("\${jwt.access-token-expiration-in-milliseconds}")
    private var tokenValidityMilliSeconds: Long = 0

    //SecretKey를 Base64로 인코딩하여 SecretKey객체로 변환
    private lateinit var key: Key

    @PostConstruct
    //의존성 주입이 될때 딱 1번만 실행되기 때문에 key값은 이후로 변하지 않음
    fun init() {
        this.key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
        println(key)
    }

    @Test
    @DisplayName("Jwt 생성")
    fun t1() {
        //given
        val claims: Map<String, Any> = mapOf(
            Claims.SUBJECT to "test1234@naver.com",
            "name" to "LIM",
            "userId" to "4"
        )
        val now = Date()
        val expiration = Date(now.time + tokenValidityMilliSeconds)

        //when
        val jwt = Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()

        val parsedClaims = jwtUtil.parseClaims(jwt)


        //then
        Assertions.assertThat(parsedClaims.get("userId", String::class.java)).isEqualTo("4")
        Assertions.assertThat(parsedClaims.get("name", String::class.java)).isEqualTo("LIM")
        Assertions.assertThat(parsedClaims.subject).isEqualTo("test1234@naver.com")

        //assertThat(parsedClaims.getExpiration().getTime()).isEqualTo(expiration.getTime());
        //assertThat(parsedClaims.getIssuedAt().getTime()).isEqualTo(now.getTime());
    }

    @Test
    @DisplayName("refreshToken 생성")
    fun t2() {
        //given
        val jti: String = UUID.randomUUID().toString()
        val claims: Map<String?, Any?> = mapOf(
            Claims.SUBJECT to 4L.toString(),
            Claims.ID to jti
        )

        val now = Date()
        val expiration = Date(now.time + 604800000L)

        //when
        val refreshToken = Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()

        val parsedClaims = jwtUtil.parseClaims(refreshToken)

        //then
        Assertions.assertThat(parsedClaims.id).isEqualTo(jti)
        Assertions.assertThat(parsedClaims.subject).isEqualTo("4")
    }


    @Test
    @DisplayName("쿠키 읽어오기")
    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    fun t3() {
        //given
        val request = MockHttpServletRequest()

        val expectedToken = "jwt"
        val accessToken = Cookie("accessToken", expectedToken)

        request.setCookies(accessToken)

        //when
        val filter = JwtAuthenticationFilter(jwtUtil, jwtService)
        // Reflection을 사용하여 private 메서드 접근
        val method = JwtAuthenticationFilter::class.java.getDeclaredMethod(
            "extractTokenFromCookie",
            HttpServletRequest::class.java
        )
        method.setAccessible(true) // private 접근 제한 해제

        val extractedToken = method.invoke(filter, request) as String?

        //then
        Assertions.assertThat(extractedToken).isEqualTo(expectedToken)
    }
}

