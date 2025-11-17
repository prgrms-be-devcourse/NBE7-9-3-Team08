package com.backend.domain.repository.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.IssueResponse
import com.backend.domain.repository.dto.response.github.IssueResponse.PullRequest
import com.backend.domain.repository.service.mapper.IssueInfoMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class IssueInfoMapperTest {

    private val mapper = IssueInfoMapper()

    @Test
    @DisplayName("IssueResponse가 null이 포함되어도 안전하게 처리해야 함")
    fun mapIssueWithNullElement_shouldBeFiltered() {
        val data = RepositoryData()

        val valid = IssueResponse(
            number = 1L,
            title = "Valid",
            state = "open",
            created_at = "2025-01-01T00:00:00Z",
            closed_at = null,
            pull_request = null
        )

        val list = mutableListOf<IssueResponse?>(
            valid,
            null
        )

        assertThatCode {
            mapper.mapIssueInfo(data, list)
        }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("IssueResponse의 pull_request 필드가 존재하면 필터링되어야 함")
    fun issueWithPullRequestField_shouldBeExcluded() {
        val issueWithPr = IssueResponse(
            number = 1L,
            title = "PR disguised",
            state = "open",
            created_at = "2025-01-01T00:00:00Z",
            closed_at = null,
            pull_request = PullRequest("url")
        )

        val pureIssue = IssueResponse(
            number = 2L,
            title = "Real issue",
            state = "open",
            created_at = "2025-01-01T00:00:00Z",
            closed_at = null,
            pull_request = null
        )

        val filtered = listOf(issueWithPr, pureIssue)
            .filter { it?.isPureIssue == true }

        assertThat(filtered).hasSize(1)
        assertThat(filtered[0].title).isEqualTo("Real issue")
    }

    @Test
    @DisplayName("날짜 필드가 null 또는 형식 이상해도 NPE 없이 null 반환")
    fun mapInvalidDate_shouldHandleGracefully() {
        val data = RepositoryData()

        val issue = IssueResponse(
            number = 1L,
            title = "broken",
            state = "open",
            created_at = "invalid",
            closed_at = null,
            pull_request = null
        )

        mapper.mapIssueInfo(data, listOf(issue))

        assertThat(data.recentIssues[0].createdAt).isNull()
    }
}
