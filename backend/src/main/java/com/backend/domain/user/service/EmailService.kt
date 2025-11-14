package com.backend.domain.user.service

import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.util.RedisUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.*

@Service
@Transactional
class EmailService(
    private val userRepository: UserRepository,
    private val javaMailSender: JavaMailSender,
    private val redisUtil: RedisUtil
) {
    companion object {
        private const val EXPIRE_TIME = 300L  //인증 코드 만료 시간
        private const val JOIN_TIME = 600L  //인증 후 회원가입 시간
    }

    /**
     * 인증코드 생성
     *
     */
    private fun createCode(): String {
        val random = Random()
        return "%06d".format(random.nextInt(1000000))
    }

    /**
     * 인증코드를 생성하고 Redis에 저장하며 이메일로 전송합니다.
     */
    fun sendEmail(email: String) {
        //이미 회원가입된 email이면 예외 발생
        val existsByEmail = userRepository.existsByEmail(email)
        if (existsByEmail) {
            throw BusinessException(ErrorCode.ALREADY_REGISTERED_EMAIL)
        }

        val authCode = createCode()
        val mimeMessage : MimeMessage = javaMailSender.createMimeMessage()
        // true: 멀티파트 메시지(HTML 등) 활성화, "utf-8": 인코딩 설정
        val helper = MimeMessageHelper(mimeMessage, true, "utf-8")

        helper.setTo(email)
        helper.setSubject("[PortfolioIQ] 회원가입 인증 코드입니다.")

        // 이메일 본문 (HTML 형식으로 보냄)
        val content = ("<h2>안녕하세요. [PortfolioIQ]입니다.</h2>"
                + "<p>아래 6자리 인증 코드를 인증 창에 입력해 주세요.</p>"
                + "<div style='font-size: 24px; font-weight: bold; color: #1e88e5;'>"
                + authCode
                + "</div>"
                + "<p style='color: #888;'>이 코드는 " + (EXPIRE_TIME / 60) + "분 후에 만료됩니다.</p>")

        helper.setText(content, true) // true: HTML 형식 사용 명시

        javaMailSender.send(mimeMessage)

        // Redis에 [이메일]:[인증코드]를 EXPIRE_TIME 동안 저장
        redisUtil.setData(email, authCode, EXPIRE_TIME)
        println("set data정보 : ")
        println("email : " + email)
        println("authCode : " + authCode)
        println("expireTime : " + EXPIRE_TIME)
    }

    /**
     * Redis에저장된 코드와 사용자가 입력한 코드를 비교합니다.
     * @param email 사용자 이메일 주소
     * @param code 사용자 입력 인증 코드
     * @return 인증 성공 여부
     */
    fun verifyAuthCode(email: String, code: String): Boolean {
        val storedCode = redisUtil.getData(email)

        if (storedCode == null || storedCode != code) {
            println("stored code is : ${storedCode}" )
            println("verifyAuthCode메서드이며 storedCode가 없거나 입력값이 잘못됐을경우")
            return false
        }

        //인증 성공시 해당 redis는 삭제하고
        redisUtil.deleteData(email)

        //해당 이메일은 인증이 성공했다는 플래그를 남김
        val verifiedKey = "VERIFIED_EMAIL:${email}"
        redisUtil.setData(verifiedKey, "Y", JOIN_TIME)
        println("생성된 플래그")
        println(redisUtil.getData(verifiedKey))
        return true
    }


}
