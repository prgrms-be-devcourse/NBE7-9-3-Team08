package com.backend.global.openai

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * OpenAIKeyPresentCondition / OpenAIKeyMissingCondition 이
 * OpenAIKeyUtil.findKey() 결과에 정확히 따라가는지 검증
 */
class OpenAIKeyConditionTest {

    @Test
    fun `PresentCondition과 MissingCondition은 OpenAIKeyUtil_findKey_결과와 일치한다`() {
        // given
        val context = mockk<ConditionContext>(relaxed = true)
        val metadata = mockk<AnnotatedTypeMetadata>(relaxed = true)

        val present = OpenAIKeyPresentCondition()
        val missing = OpenAIKeyMissingCondition()

        val hasKey = OpenAIKeyUtil.findKey() != null

        // when
        val presentResult = present.matches(context, metadata)
        val missingResult = missing.matches(context, metadata)

        // then
        assertThat(presentResult).isEqualTo(hasKey)
        assertThat(missingResult).isEqualTo(!hasKey)
    }
}
