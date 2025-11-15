package com.backend.domain.analysis.service

import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Service
class AnalysisProgressService(
    private val jwtUtil: JwtUtil
) {

    private val log = LoggerFactory.getLogger(AnalysisProgressService::class.java)

    // userId → Emitter
    private val emitters: MutableMap<Long, SseEmitter> = ConcurrentHashMap()

    /**
     * SSE 연결 설정
     */
    fun connect(userId: Long, request: HttpServletRequest): SseEmitter {
        val requestUserId = jwtUtil.getUserId(request)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        if (requestUserId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        val emitter = SseEmitter(DEFAULT_TIMEOUT)
        emitters[userId] = emitter

        // emitter 종료 시 remove
        emitter.onCompletion { emitters.remove(userId) }
        emitter.onTimeout { emitters.remove(userId) }
        emitter.onError { emitters.remove(userId) }

        sendEvent(userId, "connected", "SSE 연결 완료")

        return emitter
    }

    /**
     * SSE 이벤트 전송
     */
    fun sendEvent(userId: Long, eventName: String, data: String) {
        val emitter = emitters[userId]
        if (emitter == null) {
            log.debug("SSE Emitter 없음: userId={}", userId)
            return
        }

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data))
            log.debug("SSE 전송 성공: userId={}, event={}", userId, eventName)
        } catch (e: IOException) {
            log.debug("SSE 연결 끊김: userId={}, event={}", userId, eventName)
            emitters.remove(userId)
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 30 * 60 * 1000L // 30분
    }
}
