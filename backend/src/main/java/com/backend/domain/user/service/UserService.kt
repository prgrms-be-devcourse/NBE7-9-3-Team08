package com.backend.domain.user.service

import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.util.RedisUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService (
    private val userRepository: UserRepository,
    private val redisUtil: RedisUtil,
    private val passwordEncoder: PasswordEncoder
) {

    fun join(
        email: String,
        password: String,
        passwordCheck: String,
        name: String
    ): User {

        //email이 인증을 통과했는지 검증
        val verified = redisUtil.getData("VERIFIED_EMAIL:${email}")
        if (verified == null) {
            println("이메일 인증을 받지않은 이메일입니다.")
            throw BusinessException(ErrorCode.VALIDATION_FAILED)
        }

        //email중복검증
        if (userRepository.findByEmail(email) != null) {
            println("이미 등록된 이메일입니다.")
            throw BusinessException(ErrorCode.VALIDATION_FAILED)
        }

        //passwordCheck 검증
        if (password != passwordCheck) {
            println("비밀번호 확인이 비밀번호와 같지않습니다.")
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }


        //검증이 완료되었다면 해당 email을 redis에서 삭제
        if (redisUtil.deleteData("VERIFIED_EMAIL:${email}")) {
            println("VERIFIED_EMAIL:${email}은 email검증이 완료되어서 redis에서 삭제가 됐습니다.")
        } else {
            println("redis 삭제 실패입니다.")
        }


        //암호화 된 비밀번호를 저장
        val encodedPassword = passwordEncoder.encode(password)

        val user = User(email, encodedPassword, name)
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: @NotBlank String): User {
        return userRepository.findByEmail(email)
            ?: throw BusinessException(ErrorCode.VALIDATION_FAILED)
    }

    @Transactional(readOnly = true)
    fun findByAll(): List<User> {
        return userRepository.findAll()
    }

    fun softdelete(email: String): User {
        val user = userRepository.findByEmail(email)
            ?: throw BusinessException(ErrorCode.VALIDATION_FAILED)

        user.delete()
        return user

    }

    /*

    public User restore(@NotBlank String email) {
        System.out.println("email : " + email);
        User user = userRepository.findByEmailIncludeDeleted(email).orElse(null);
        if(user != null){
            user.restore();
            System.out.println("user restore ");
            return user;
        }else{
            System.out.println("restore 실패");
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
*/
    fun modifyName(email: String, name: String): User {
        val user: User = userRepository.findByEmail(email)
                ?: throw BusinessException(ErrorCode.VALIDATION_FAILED)
        user.changeName(name)
        return user
    }

    fun modifyPassword(
        email: String,
        password: String,
        passwordCheck: String
    ): User {
        val user: User =
            userRepository.findByEmail(email)
                ?: throw BusinessException(ErrorCode.VALIDATION_FAILED)

        if (password == passwordCheck) {
            user.changePassword(passwordEncoder.encode(password))
        } else {
            throw BusinessException(ErrorCode.PASSWORD_NOT_EQUAL)
        }

        return user
    }

    fun getUserNameByUserId(userId: Long): User {
        return userRepository.findNameById(userId)
            ?: throw BusinessException(ErrorCode.VALIDATION_FAILED)
    }
}
