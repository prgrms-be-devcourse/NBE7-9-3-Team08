package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class ReadmeInfoMapper {

    // ResponseData 문서화 품질 [README 관련]
    fun mapReadmeInfo(data: RepositoryData, readmeContent: String?) {
        if (readmeContent.isNullOrBlank()) {
            setEmptyReadmeData(data)
            return
        }

        // README.md 존재 여부
        data.hasReadme = true

        // README.md 전체 내용
        data.readmeContent = readmeContent

        // README.md 글자 수
        data.readmeLength = readmeContent.length

        val sectionTitles = extractSectionTitles(readmeContent)

        // README.md # 개수
        data.readmeSectionCount = sectionTitles.size

        // README.md 섹션 제목 리스트
        data.readmeSectionTitles = sectionTitles
    }

    private fun setEmptyReadmeData(data: RepositoryData) {
        data.hasReadme = false
        data.readmeLength = 0
        data.readmeSectionCount = 0
        data.readmeSectionTitles = emptyList()
        data.readmeContent = ""
    }

    private fun extractSectionTitles(content: String): List<String> {
        val headerPattern = Pattern.compile("^(#{1,6})\\s+(.+)$")
        var inCodeBlock = false
        val titles = mutableListOf<String>()

        content.split("\n").forEach { line ->
            val trimmed = line.trim()

            // 코드 블록 시작/종료 감지
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEach
            }

            // 코드 블록 외부에서만 섹션 제목 감지
            if (!inCodeBlock) {
                val matcher = headerPattern.matcher(trimmed)
                if (matcher.matches()) {
                    titles.add(matcher.group(2).trim())
                }
            }
        }

        return titles
    }
}
