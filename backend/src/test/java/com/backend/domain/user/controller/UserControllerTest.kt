package com.backend.domain.user.controller

import com.backend.domain.user.service.UserService
import com.backend.domain.user.util.RedisUtil
import jakarta.mail.MessagingException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
//@Tag("redis")
@Transactional
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestConfiguration
    internal class MailStubConfig {
        @Bean
        @Primary
        fun javaMailSender(): JavaMailSender? {
            // 모든 send(...)가 doNothing()인 목 객체
            return Mockito.mock(JavaMailSender::class.java)
        }

        @Bean
        @Primary
        fun redisUtil(): RedisUtil {
            val mock = Mockito.mock(RedisUtil::class.java)
            // 모든 이메일에 대해 인증 통과로 처리
            Mockito.`when`<String?>(mock.getData(ArgumentMatchers.anyString())).thenReturn("verified")
            Mockito.`when`<Boolean?>(mock.deleteData(ArgumentMatchers.anyString())).thenReturn(true)
            return mock
        }
    }

    @Test
    @DisplayName("회원가입")
    @Throws(MessagingException::class)
    fun t1() {
        val email = "test1234@example.com"
        val password = "raer12356@"
        val passwordCheck = "raer12356@"
        val name = "임병수"

        val user =
            userService.join(email, password, passwordCheck, name)

        assertThat(user).isNotNull()
        assertThat(user.email).isEqualTo(email)
        assertThat(user.name).isEqualTo(name)
    }

    @Test
    @DisplayName("회원가입 실패 - 필수값 누락")
    fun t2(){
        //given
        //이메일, 비밀번호, 이름 등 필수 필드가 비어 있을 때 (null 또는 빈 문자열) 유효성 검사 실패(400 Bad Request)를 확인합니다.
        val missingEmailRequestJson = """
        {
            "email": "", 
            "password": "validpassword123@",
            "passwordCheck": "validpassword123@",
            "name": "테스트사용자"
        }
        """.trimIndent()
        //when
        mockMvc.perform(
            // 💡 실제 회원가입 API 엔드포인트로 변경하세요. (예: /api/users/join)
            post("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missingEmailRequestJson)
        )
            //HTTP 상태 코드가 400 Bad Request인지 검증
            .andExpect(status().isBadRequest)
            //응답 JSON 본문에 validation 오류 코드가 포함되어 있는지 검증
            .andExpect(jsonPath("$.data.email").value("이메일은 필수 입력값 입니다."))

    }


    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호(테스트 데이터 활용 필수)")
    fun t3() {
        //given
        //유효한 이메일이지만 틀린 비밀번호로 시도했을 때, 로그인 실패 에러(401 Unauthorized)가 발생하는지 확인.
        val missingEmailRequestJson = """
        {
            "email": "alice@example.com", 
            "password": "asdf123456%"
        }
        """.trimIndent()
        //when
        mockMvc.perform(
            // 💡 실제 회원가입 API 엔드포인트로 변경하세요. (예: /api/users/join)
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missingEmailRequestJson)
        )
            //HTTP 상태 코드가 400 Bad Request인지 검증
            .andExpect(status().isBadRequest)
            //응답 JSON 본문에 validation 오류 코드가 포함되어 있는지 검증
            .andExpect(jsonPath("$.message").value("로그인에 실패했습니다."))

    }
/*
    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    fun t4() {
        //given
        //DB에 존재하지 않는 이메일로 로그인 시도 시, 실패 에러(401 Unauthorized)가 발생하는지 확인.

        //when

        //then
    }
    */
}