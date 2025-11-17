package com.backend.domain.repository.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.PullRequestResponse
import com.backend.domain.repository.service.mapper.PullRequestInfoMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class PullRequestInfoMapperTest {

    private val mapper = PullRequestInfoMapper()

    @Test
    @DisplayName("created_at이 null이더라도 merged_at이 있으면 정상 처리")
    fun prWithNullCreatedAt_shouldBeHandled() {
        val data = RepositoryData()

        val pr = PullRequestResponse(
            number = 1L,
            title = "PR test",
            state = "closed",
            created_at = null,
            merged_at = "2025-01-01T00:00:00Z"
        )

        mapper.mapPullRequestInfo(data, listOf(pr))

        assertThat(data.recentPullRequests)
            .hasSize(1)

        assertThat(data.recentPullRequests[0].mergedAt)
            .isNotNull()
    }
}
