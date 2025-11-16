package com.backend.domain.evaluation.service.impl

import com.backend.domain.evaluation.service.AiGateway
import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.responses.*
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service
import java.lang.reflect.Method
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText(environment['openai.api.key'])")
class OpenAiGateway(
    private val client: OpenAIClient,
) : AiGateway {

    @PostConstruct
    fun logActive() {
        println("[AiGateway] OpenAiGateway ACTIVE")
    }

    override fun complete(content: String, prompt: String): String {
        val input = """
            [SYSTEM PROMPT]
            $prompt

            [USER CONTENT]
            $content

            [RESPONSE RULE]
            - 항상 한국어 텍스트만 출력하세요.
        """.trimIndent()

        val params = ResponseCreateParams.builder()
            .model(ChatModel.GPT_5_NANO)
            .input(input)
            .build()

        val res: Response = client.responses().create(params)

        // 1. 새로운 outputText() 헬퍼가 있을 경우 반영 (SDK 버전 대응)
        try {
            val m: Method = res.javaClass.getMethod("outputText")
            val out: Any? = m.invoke(res)
            if (out is String && out.trim().isNotEmpty()) {
                return out.trim()
            }
        } catch (_: Throwable) {
            // 무시하고 아래 fallback 로직 사용
        }

        // 2. responses().create() 기본 output 파싱
        val joined = extractOutputText(res)
        if (joined.isNotEmpty()) return joined

        // 3. 마지막 fallback
        return res.toString()
    }

    /**
     * OpenAI Response 객체에서 텍스트만 뽑아서 하나의 문자열로 합친다.
     */
    private fun extractOutputText(res: Response?): String {
        if (res == null) return ""

        val output = res.output() ?: return ""

        return output.stream()
            .map(ResponseOutputItem::message)
            .flatMap { opt: Optional<ResponseOutputMessage> -> opt.stream() }
            .flatMap { msg ->
                val cs = msg.content()
                if (cs == null) {
                    Stream.empty<ResponseOutputMessage.Content>()
                } else {
                    cs.stream()
                }
            }
            .map { c ->
                try {
                    val t: ResponseOutputText? = c.asOutputText()
                    t?.text()
                } catch (_: Exception) {
                    null
                }
            }
            .filter(Objects::nonNull)                     // null 제거
            .map { (it as String).trim() }                // ✅ 명시적으로 String 캐스팅 후 trim
            .filter { it.isNotEmpty() }
            .collect(Collectors.joining())
    }
}
