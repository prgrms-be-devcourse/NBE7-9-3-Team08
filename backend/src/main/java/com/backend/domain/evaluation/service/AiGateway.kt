package com.backend.domain.evaluation.service

interface AiGateway {
    fun complete(content: String, prompt: String): String
}
