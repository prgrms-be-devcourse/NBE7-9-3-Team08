package com.backend.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ExternalApiClientConfig(
    @Value("\${github.api.base-url}")
    private val githubBaseUrl: String,

    @Value("\${github.api.token}")
    private val githubToken: String
) {
    @Bean
    fun githubWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(githubBaseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader(HttpHeaders.USER_AGENT, "PortpolioIQ-App")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $githubToken")
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
            }
            .build()
    }
}
