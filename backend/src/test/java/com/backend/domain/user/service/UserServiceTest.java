package com.backend.domain.user.service;

import com.backend.domain.user.entity.User;
import com.backend.domain.user.repository.UserRepository;
import com.backend.domain.user.util.RedisUtil;
import com.backend.global.exception.BusinessException;
import com.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("redis")
@DisplayName("UserService 테스트")
public class UserServiceTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long JOIN_TIME = 600L;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 상태 초기화
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
    }

    @Test
    @DisplayName("User 생성")
    void t1(){
        //given
        User user = new User("test@naver.com", "test1234", "test");

        //when
        userRepository.save(user);

        //then
        assertThat(userRepository.findById(user.getId()).get()).isEqualTo(user);

    }


    //성공 케이스: 이메일 인증이 완료되었고, 이메일이 중복되지 않으며, 비밀번호가 일치할 때 회원가입이 성공
    @Test
    @DisplayName("회원가입 성공")
    void t2(){
        //given
        //이메일 인증을 받은 이메일이어야함.

        String email = "dlaqudtn1107@naver.com";
        String password = "1234";
        String passwordCheck = "1234";
        String name = "임병수";
        //임시 이메일 인증
        redisUtil.setData("VERIFIED_EMAIL:"+email, "Y", JOIN_TIME);

        //when
        User joinUser = userService.join(email, password, passwordCheck, name);

        //then
        //회원가입 후 DB에서 회원 찾기
        assertThat(joinUser).isEqualTo(userRepository.findByEmail(email));
    }

    //실패 케이스 1: 이메일 인증을 받지 않았을 때 회원가입이 실패
    @Test
    @DisplayName("회원가입 실패 - 인증 받지 않은 이메일")
    void t3(){
        //given
        String email = "test@naver.com";
        String password = "test1234";
        String passwordCheck = "test1234";
        String name = "Lim";

        //when
        String verified = redisUtil.getData("VERIFIED_EMAIL:"+email);

        //then
        assertThat(verified).isEqualTo(null);
    }

    //실패 케이스 2: 이미 등록된 이메일일 때 회원가입이 실패
    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void t4(){
        //given
        String email = "test@naver.com";
        String password = "test1234";
        String passwordCheck = "test1234";
        String name = "Lim";

        User user = new User(email, password, name);
        //임시 이메일 인증
        redisUtil.setData("VERIFIED_EMAIL:" + email, "Y", JOIN_TIME);
        userRepository.save(user);

        //when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.join(email, password, passwordCheck, name);
        });

        //then
        //assertThat으로 Exception이 뜨는 것을 확인
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);

    }

    @Test
    @DisplayName("회원가입 실패 - 필수값 누락")
    void t5(){
        //given
        //이메일, 비밀번호, 이름 등 필수 필드가 비어 있을 때 (null 또는 빈 문자열) 유효성 검사 실패(400 Bad Request)를 확인합니다.

        //when

        //then
    }

    // 우선순위 낮음
    @Test
    @DisplayName("이름 변경 성공")
    void t6(){
        //given

        //when

        //then
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void t7(){
        //given

        //when

        //then
    }

}
