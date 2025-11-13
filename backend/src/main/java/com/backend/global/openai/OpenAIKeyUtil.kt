package com.backend.global.openai

import io.github.cdimascio.dotenv.Dotenv

object OpenAIKeyUtil {

    fun findKey(): String? {
        // 1) OS 환경변수 → 2) JVM 시스템 프로퍼티 → 3) .env
        var k: String? = System.getenv("OPENAI_API_KEY")
        if (k.isNullOrBlank()) {
            k = System.getProperty("OPENAI_API_KEY")
        }
        if (k.isNullOrBlank()) {
            try {
                k = Dotenv.configure()
                    .ignoreIfMissing()
                    .load()
                    .get("OPENAI_API_KEY")
            } catch (_: Throwable) {
                // .env 없으면 무시
            }
        }
        return if (k.isNullOrBlank()) null else k
    }
}
