package com.backend.domain.user.service;


import com.backend.domain.user.util.JwtUtil;
import com.backend.domain.user.util.RedisUtil;
import com.backend.global.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("redis")
public class JwtServiceTest {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RedisUtil redisUtil;


    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration-in-milliseconds}")
    private int tokenValidityMilliSeconds;

    //SecretKey를 Base64로 인코딩하여 SecretKey객체로 변환
    private SecretKey key;

    @PostConstruct  //의존성 주입이 될때 딱 1번만 실행되기 때문에 key값은 이후로 변하지 않음
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        System.out.println(key);
    }

    @Test
    @DisplayName("Jwt 생성")
    void t1(){
        //given
        Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.SUBJECT, "test1234@naver.com");
        claims.put("name", "LIM");
        claims.put("userId", 4L);
        Date now = new Date();
        Date expiration =  new Date(now.getTime() + tokenValidityMilliSeconds);

        //when
        String jwt = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Claims parsedClaims = jwtUtil.parseClaims(jwt);


        //then
        assertThat(parsedClaims.get("userId", Long.class)).isEqualTo(4);
        assertThat(parsedClaims.get("name", String.class)).isEqualTo("LIM");
        assertThat(parsedClaims.getSubject()).isEqualTo("test1234@naver.com");
        //assertThat(parsedClaims.getExpiration().getTime()).isEqualTo(expiration.getTime());
        //assertThat(parsedClaims.getIssuedAt().getTime()).isEqualTo(now.getTime());

    }

    @Test
    @DisplayName("refreshToken 생성")
    void t2(){
        //given
        Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.SUBJECT, String.valueOf(4L));
        String jti = UUID.randomUUID().toString();
        claims.put(Claims.ID, jti);

        Date now = new Date();
        Date expiration =  new Date(now.getTime() + 604800000);

        //when
        String refreshToken = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Claims parsedClaims = jwtUtil.parseClaims(refreshToken);

        //then
        assertThat(parsedClaims.getId()).isEqualTo(jti);
        assertThat(parsedClaims.getSubject()).isEqualTo("4");

    }


    @Test
    @DisplayName("쿠키 읽어오기")
    void t3() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //given
        MockHttpServletRequest request = new  MockHttpServletRequest();

        String expectedToken = "jwt";
        Cookie accessToken = new Cookie("accessToken", expectedToken);

        request.setCookies(accessToken);

        //when
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtService);
        // Reflection을 사용하여 private 메서드 접근
        Method method = JwtAuthenticationFilter.class.getDeclaredMethod("extractTokenFromCookie", HttpServletRequest.class);
        method.setAccessible(true); // private 접근 제한 해제

        String extractedToken = (String) method.invoke(filter, request);

        //then
        assertThat(extractedToken).isEqualTo(expectedToken);
    }

    @Test
    @DisplayName("로그인 성공")
    void t4(){
        //given
        //유효한 이메일과 정확한 비밀번호로 로그인 시도 시, accessToken과 refreshToken이 정상적으로 발급되는지 확인합니다.

        //when
        //발급된 accessToken이 유효한 형식인지, 클레임(claims)에 사용자 정보(ID, 이메일 등)가 올바르게 담겨 있는지 확인합니다.

        //then
        //refreshToken이 Redis나 DB에 정상적으로 저장되는지 확인합니다.
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void t5(){
        //given
        //유효한 이메일이지만 틀린 비밀번호로 시도했을 때, 로그인 실패 에러(401 Unauthorized)가 발생하는지 확인합니다.

        //when

        //then
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void t6(){
        //given
        //DB에 존재하지 않는 이메일로 로그인 시도 시, 실패 에러(401 Unauthorized)가 발생하는지 확인합니다.

        //when

        //then
    }
}

