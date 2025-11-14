package com.backend.domain.user.service

import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.util.JwtUtil
import com.backend.domain.user.util.RedisUtil
import com.backend.domain.user.util.RefreshTokenUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import lombok.RequiredArgsConstructor
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@RequiredArgsConstructor
class JwtService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val redisUtil: RedisUtil,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenUtil: RefreshTokenUtil
) {

    fun login(
        email: String,
        password: String
    ): List<String>? {
        val user: User = userRepository.findByEmail(email)
                ?:throw BusinessException(ErrorCode.EMAIL_NOT_FOUND)

        var userId = user.id ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        //비밀번호 체크
        if (checkPassword(email, password)) {
            //email에 대응하는 비밀번호가 맞다면 jwt, refreshToken 발급
            val jwtToken = jwtUtil.createToken(user.email, user.name, userId)
            val refreshToken = refreshTokenUtil.createToken(userId)
            return listOf(jwtToken, refreshToken)
        } else {
            throw BusinessException(ErrorCode.LOGIN_FAILED)
        }
    }

    fun logout(token: String, expiration: Long): Boolean {
        //jtw받아서 redis 블랙리스트에 추가
        val key = "jwt:blacklist:${token}"

        redisUtil.setData(key, "logout", expiration)

        return true
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     *
     */
    fun isBlacklisted(token: String): Boolean {
        val key = "jwt:blacklist:${token}"
        return redisUtil.hasKey(key)
    }

    //암호화된 비밀번호를 체크
    fun checkPassword(email: String, password: String): Boolean {
        val user: User = userRepository.findByEmail(email)
                ?: throw BusinessException(ErrorCode.EMAIL_NOT_FOUND)

        return passwordEncoder.matches(password, user.password)
    }
}
