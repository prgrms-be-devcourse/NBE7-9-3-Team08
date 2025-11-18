package com.backend.domain.user.controller;

import com.backend.domain.user.service.UserService;
import com.backend.domain.user.util.RedisUtil;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Tag("redis")
@Transactional
class UserControllerTest {

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

    @Autowired
    private UserService userService;

<<<<<<< HEAD
//    @Test
//    @DisplayName("회원가입")
//    void t1() throws MessagingException {
//        String email = "test+" + System.currentTimeMillis() + "@example.com";
//        String password = "123456";
//        String passwordCheck = "123456";
//        String name = "임병수";
//
//        User user = userService.join(email, password, passwordCheck, name);
//
//        assertThat(user).isNotNull();
//        assertThat(user.email).isEqualTo(email);
//        assertThat(user.name).isEqualTo(name);
//    }
=======
    @Test
    @DisplayName("회원가입")
    void t1() throws MessagingException {
        String email = "test+" + System.currentTimeMillis() + "@example.com";
        String password = "123456";
        String passwordCheck = "123456";
        String name = "임병수";

        User user = userService.join(email, password, passwordCheck, name);

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getName()).isEqualTo(name);
    }
>>>>>>> 3268aefe269180383b0a42603f2581f8cacd081b
}
