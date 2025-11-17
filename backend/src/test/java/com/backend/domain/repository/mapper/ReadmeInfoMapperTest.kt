package com.backend.domain.repository.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.service.mapper.ReadmeInfoMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class ReadmeInfoMapperTest {
    private val mapper = ReadmeInfoMapper()

    @Test
    @DisplayName("README가 null일 때 기본값 설정")
    fun mapNullReadme_shouldSetDefaultValues() {
        val data = RepositoryData()
        mapper.mapReadmeInfo(data, null)
        Assertions.assertThat(data.hasReadme).isFalse()
        Assertions.assertThat(data.readmeContent).isEmpty()
        Assertions.assertThat(data.readmeSectionCount).isZero()
    }

    @Test
    @DisplayName("README 내용이 공백만 있을 때 hasReadme=false 여야 함")
    fun mapWhitespaceOnlyReadme_shouldBeHandled() {
        val data = RepositoryData()
        mapper.mapReadmeInfo(data, "   \n  \n")
        Assertions.assertThat(data.hasReadme).isFalse()
        Assertions.assertThat(data.readmeLength).isZero()
    }

    @Test
    @DisplayName("README 내 코드 블록 안의 #은 섹션으로 인식하지 않아야 함")
    fun mapReadmeWithCodeBlock_shouldIgnoreHeadersInCode() {
        val data = RepositoryData()
        val readme = """
                # Main Title

                ```bash
                # not a header
                echo "hello"
                ```

                ## Section 2
                
                """.trimIndent()
        mapper.mapReadmeInfo(data, readme)
        Assertions.assertThat(data.hasReadme).isTrue()
        Assertions.assertThat<String>(data.readmeSectionTitles).containsExactly("Main Title", "Section 2")
        Assertions.assertThat(data.readmeSectionCount).isEqualTo(2)
    }

    @Test
    @DisplayName("README가 HTML 헤더를 포함하더라도 Markdown 헤더만 인식해야 함")
    fun readmeWithHtmlHeaders_shouldIgnoreThem() {
        val data = RepositoryData()
        val htmlReadme = """
                <h1>Main Title</h1>
                # Markdown Header
                <h2>Sub Title</h2>
                ## Markdown Sub
                
                """.trimIndent()
        mapper.mapReadmeInfo(data, htmlReadme)
        Assertions.assertThat<String>(data.readmeSectionTitles)
            .containsExactly("Markdown Header", "Markdown Sub")
    }
}
