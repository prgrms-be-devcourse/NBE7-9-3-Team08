package com.backend.domain.user.service

import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.util.RedisUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("redis")
@DisplayName("UserService 테스트")
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var redisUtil: RedisUtil

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    private companion object {
        const val JOIN_TIME = 600L
    }

    @BeforeEach
    fun setUp() {
        // 테스트 전 Redis 상태 초기화
        redisTemplate.execute { connection ->
            connection.commands().flushDb()
            null
        }

    }

    @Test
    @DisplayName("User 생성")
    fun t1() {
        //given
        val email = "test@naver.com"
        val password = "test1234"
        val name = "test"
        val user = User(email, password, name)

        //when
        userRepository.save(user)

        //then
        val result = userRepository.findById(user.id!!)
        assertThat(result.isPresent).isTrue
        assertThat(result.get()).isEqualTo(user)
    }


    //성공 케이스: 이메일 인증이 완료되었고, 이메일이 중복되지 않으며, 비밀번호가 일치할 때 회원가입이 성공
    @Test
    @DisplayName("회원가입 성공")
    fun t2() {
        //given
        //이메일 인증을 받은 이메일이어야함.
        val email = "dlaqudtn1107@naver.com"
        val password = "1234"
        val passwordCheck = "1234"
        val name = "임병수"

        //임시 이메일 인증
        redisUtil.setData("VERIFIED_EMAIL:${email}", "Y", JOIN_TIME)

        //when
        val joinUser = userService.join(email, password, passwordCheck, name)

        //then
        //회원가입 후 DB에서 회원 찾기
        val result = userRepository.findByEmail(email)
        assertThat(joinUser).isEqualTo(result)
    }

    //실패 케이스 1: 이메일 인증을 받지 않았을 때 회원가입이 실패
    @Test
    @DisplayName("회원가입 실패 - 인증 받지 않은 이메일")
    fun t3() {
        //given
        val email = "test@naver.com"
        val password = "test1234"
        val passwordCheck = "test1234"
        val name = "Lim"

        //when
        //검증되지 않은 이메일로 회원가입
        //val verified = redisUtil!!.getData("VERIFIED_EMAIL:" + email)
        val exception = assertThrows<BusinessException> {
            userService.join(email, password, passwordCheck, name)
        }

        //then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.VALIDATION_FAILED)
    }

    //실패 케이스 2: 이미 등록된 이메일일 때 회원가입이 실패
    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    fun t4() {
        //given
        val email = "test@naver.com"
        val password = "test1234"
        val passwordCheck = "test1234"
        val name = "Lim"

        val encodedPassword = passwordEncoder.encode(password)
        val user = User(email, encodedPassword, name)

        // 임시 이메일 인증
        redisUtil.setData("VERIFIED_EMAIL:$email", "Y", JOIN_TIME)
        userRepository.save(user)

        //when
        val exception = assertThrows<BusinessException> {
            userService.join(email, password, passwordCheck, name)
        }

        //then
        //assertThat으로 Exception이 뜨는 것을 확인
        assertThat(exception.errorCode).isEqualTo(ErrorCode.VALIDATION_FAILED)
    }

}
