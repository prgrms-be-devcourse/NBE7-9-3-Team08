package com.backend.global.openai

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAIConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText(environment['openai.api.key'])")
    fun openAIClient(
        @Value("\${openai.api.key}") key: String,
    ): OpenAIClient {
        println("[OpenAI] Creating OpenAIClient bean")
        return OpenAIOkHttpClient.builder()
            .apiKey(key)
            .build()
    }
}
