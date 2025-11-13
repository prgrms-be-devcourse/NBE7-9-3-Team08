package com.backend.domain.user.service;

import com.backend.domain.user.entity.User;
import com.backend.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User 생성")
    void t1(){
        //given
        User user = new User("test@naver.com", "test1234", "test");

        //when
        userRepository.save(user);

        //then
        Assertions.assertThat(userRepository.findById(user.getId()).get()).isEqualTo(user);
    }


}
