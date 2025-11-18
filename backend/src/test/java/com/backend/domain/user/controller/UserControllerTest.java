package com.backend.domain.user.controller;

import com.backend.domain.user.entity.User;
import com.backend.domain.user.service.UserService;
import com.backend.domain.user.util.RedisUtil;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Tag("redis")
@Transactional
class UserControllerTest {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;

    @TestConfiguration
    static class MailStubConfig {
        @Bean @Primary
        JavaMailSender javaMailSender() {
            // 모든 send(...)가 doNothing()인 목 객체
            return mock(JavaMailSender.class);
        }

        @Bean @Primary
        RedisUtil redisUtil() {
            RedisUtil mock = mock(RedisUtil.class);
            // 모든 이메일에 대해 인증 통과로 처리
            when(mock.getData(anyString())).thenReturn("verified");
            when(mock.deleteData(anyString())).thenReturn(true);
            return mock;
        }
    }

    @Test
    @DisplayName("회원가입")
    void t1() throws MessagingException {
        String email = "test+" + System.currentTimeMillis() + "@example.com";
        String password = "raer12356@";
        String passwordCheck = "raer12356@";
        String name = "임병수";

        User user = userService.join(email, passwordEncoder.encode(password), passwordEncoder.encode(passwordCheck), name);

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("로그인 성공")
    void t2() throws Exception {
        //given
        //유효한 이메일과 정확한 비밀번호로 로그인 시도 시, accessToken과 refreshToken이 정상적으로 발급되는지 확인.
/*
        String email = "test+"  + "@example.com";
        String password = "raer12356@";
        String passwordCheck = "raer12356@";
        String name = "임병수";

        User user = userService.join(email, passwordEncoder.encode(password), passwordEncoder.encode(passwordCheck), name);
*/
        /*String loginRequestJson = "{\"email\": \"testuser@example.com\", \"password\": \"validpassword123\"}";

        MvcResult result = mockMvc.perform(
                        post("/api/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson)
                )
                // 2. 응답 상태 코드가 200 OK인지 검증
                .andExpect(status().isOk())
                // 응답 전문 전체 출력 (디버깅에 유용)
                .andDo(print())
                .andReturn(); // 결과 객체를 얻어옴

        // 3. Access Token 쿠키 검증 및 값 추출
        Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");

        // 쿠키가 존재하는지 확인
        assertNotNull(accessTokenCookie, "Access Token 쿠키가 응답에 없습니다.");
        // 쿠키 값이 비어있지 않은지 확인
        assertTrue(accessTokenCookie.getValue() != null && !accessTokenCookie.getValue().isEmpty(),
                "Access Token 쿠키 값이 비어있습니다.");

        // 4. Refresh Token 쿠키 검증 및 값 추출
        Cookie refreshTokenCookie = result.getResponse().getCookie("refreshToken");

        // 쿠키가 존재하는지 확인
        assertNotNull(refreshTokenCookie, "Refresh Token 쿠키가 응답에 없습니다.");
        // 쿠키 값이 비어있지 않은지 확인
        assertTrue(refreshTokenCookie.getValue() != null && !refreshTokenCookie.getValue().isEmpty(),
                "Refresh Token 쿠키 값이 비어있습니다.");

        // 디버깅 목적으로 콘솔에 출력
        System.out.println("발급된 Access Token: " + accessTokenCookie.getValue());
        System.out.println("발급된 Refresh Token: " + refreshTokenCookie.getValue());

*/
        //when
        //발급된 accessToken이 유효한 형식인지, 클레임(claims)에 사용자 정보(ID, 이메일 등)가 올바르게 담겨 있는지 확인.


        //then
        //refreshToken이 Redis나 DB에 정상적으로 저장되는지 확인.
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void t3(){
        //given
        //유효한 이메일이지만 틀린 비밀번호로 시도했을 때, 로그인 실패 에러(401 Unauthorized)가 발생하는지 확인.

        //when

        //then
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void t4(){
        //given
        //DB에 존재하지 않는 이메일로 로그인 시도 시, 실패 에러(401 Unauthorized)가 발생하는지 확인.

        //when

        //then
    }
}
