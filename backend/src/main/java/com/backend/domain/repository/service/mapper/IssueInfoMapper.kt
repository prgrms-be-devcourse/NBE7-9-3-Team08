package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.RepositoryData.IssueInfo
import com.backend.domain.repository.dto.response.github.IssueResponse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class IssueInfoMapper {

    fun mapIssueInfo(data: RepositoryData, response: List<IssueResponse?>?) {
        if (response.isNullOrEmpty()) {
            setEmptyIssue(data)
            return
        }

        // null 제거
        val validIssues = response.filterNotNull()

        // 최근 6개월 이슈 수
        data.issueCountLast6Months = validIssues.size

        // 6개월 내 해결된 이슈 수
        data.closedIssueCountLast6Months = validIssues.count { it.isClosed }

        // 최근 10개 이슈
        data.recentIssues = validIssues
            .take(10)
            .map { convertToIssueInfo(it) }
    }

    private fun setEmptyIssue(data: RepositoryData) {
        data.issueCountLast6Months = 0
        data.closedIssueCountLast6Months = 0
        data.recentIssues = emptyList()
    }

    private fun convertToIssueInfo(issue: IssueResponse): IssueInfo {
        return IssueInfo(
            title = issue.title ?: "",
            state = issue.state ?: "open",
            createdAt = parseGitHubDate(issue.created_at) ?: LocalDateTime.now(),
            closedAt = parseGitHubDate(issue.closed_at)
        )
    }

    private fun parseGitHubDate(dateString: String?): LocalDateTime? {
        if (dateString.isNullOrBlank()) return null

        return try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            null
        }
    }
}
