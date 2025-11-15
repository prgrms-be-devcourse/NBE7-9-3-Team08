package com.backend.domain.analysis.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SseProgressNotifier(
    private val progressService: AnalysisProgressService
) : ProgressNotifier {

    private val log = LoggerFactory.getLogger(SseProgressNotifier::class.java)

    override fun notify(userId: Long, event: String, message: String) {
        log.debug("[SSE] userId={}, event={}, message={}", userId, event, message)
        progressService.sendEvent(userId, event, message)
    }
}
