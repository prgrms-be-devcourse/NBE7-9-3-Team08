package com.backend.global.openai

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OpenAIKeyProbe(
    @Value("\${OPENAI_API_KEY:}") private val rawEnv: String,    // OS env
    @Value("\${openai.api.key:}") private val mapped: String,   // yml 매핑
) {

    @PostConstruct
    fun log() {
        println("[PROBE] OPENAI_API_KEY=" + if (rawEnv.isEmpty()) "<EMPTY>" else "****")
        println("[PROBE] openai.api.key=" + if (mapped.isEmpty()) "<EMPTY>" else "****")
    }
}
