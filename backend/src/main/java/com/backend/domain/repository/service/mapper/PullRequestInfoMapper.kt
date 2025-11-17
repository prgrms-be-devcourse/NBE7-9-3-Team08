package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.PullRequestResponse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class PullRequestInfoMapper {

    fun mapPullRequestInfo(data: RepositoryData, response: List<PullRequestResponse>?) {
        if (response.isNullOrEmpty()) {
            setEmptyPullRequest(data)
            return
        }

        // null 제거
        val validPrs = response

        // 최근 6개월 PR 수
        data.pullRequestCountLast6Months = validPrs.size

        // 6개월 내 머지된 PR 수
        val mergedPRCount = validPrs.count { it.merged_at != null }
        data.mergedPullRequestCountLast6Months = mergedPRCount

        // 최근 10개 PR
        data.recentPullRequests = validPrs
            .take(10)
            .map { convertToPullRequestInfo(it) }
    }

    private fun setEmptyPullRequest(data: RepositoryData) {
        data.pullRequestCountLast6Months = 0
        data.mergedPullRequestCountLast6Months = 0
        data.recentPullRequests = emptyList()
    }

    private fun convertToPullRequestInfo(pr: PullRequestResponse): RepositoryData.PullRequestInfo {
        return RepositoryData.PullRequestInfo(
            title = pr.title ?: "",
            state = pr.state ?: "open",
            createdAt = parseGitHubDate(pr.created_at) ?: LocalDateTime.now(),
            mergedAt = parseGitHubDate(pr.merged_at)
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
