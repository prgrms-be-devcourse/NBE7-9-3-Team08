package com.backend.domain.repository.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.CommitResponse
import com.backend.domain.repository.dto.response.github.CommitResponse.AuthorDetails
import com.backend.domain.repository.dto.response.github.CommitResponse.CommitDetails
import com.backend.domain.repository.service.mapper.CommitInfoMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

internal class CommitInfoMapperTest {

    private val mapper = CommitInfoMapper()

    @Test
    @DisplayName("커밋이 하나도 없는 저장소 - 기본값 세팅 확인")
    fun mapEmptyCommits_shouldSetDefaults() {
        val data = RepositoryData()

        mapper.mapCommitInfo(data, mutableListOf())

        assertThat(data.lastCommitDate).isNull()
        assertThat(data.commitCountLast90Days).isZero()
        assertThat(data.recentCommits).isEmpty()
    }

    @Test
    @DisplayName("Commit 내부 필드가 null이어도 NPE 없이 동작해야 함")
    fun mapCommitWithNullInnerFields_shouldBeSafe() {
        val data = RepositoryData()

        val commitResponse = CommitResponse(
            CommitDetails(message = "initial commit", author = null)
        )

        mapper.mapCommitInfo(data, listOf(commitResponse))

        assertThat(data.commitCountLast90Days).isEqualTo(1)
        assertThat(data.recentCommits).hasSize(1)
    }

    @Test
    @DisplayName("CommitResponse의 commit() 자체가 null일 때")
    fun mapCommitWithNullCommitField_shouldBeSafe() {
        val data = RepositoryData()

        val nullCommit = Mockito.mock(CommitResponse::class.java)
        `when`(nullCommit.commit).thenReturn(null)

        assertThatCode {
            mapper.mapCommitInfo(data, listOf(nullCommit))
        }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("100자 이상의 커밋 메시지는 잘려야 함")
    fun mapLongCommitMessage_shouldBeTruncated() {
        val data = RepositoryData()
        val longMessage = "a".repeat(150)

        val commit = CommitResponse(
            CommitDetails(
                message = longMessage,
                author = AuthorDetails("2025-01-01T00:00:00Z")
            )
        )

        mapper.mapCommitInfo(data, listOf(commit))

        val truncated = data.recentCommits[0].message
        assertThat(truncated)
            .hasSize(100)
            .endsWith("...")
    }

    @Test
    @DisplayName("여러 줄 커밋 메시지는 첫 줄만 사용해야 함")
    fun mapMultiLineCommitMessage_shouldUseFirstLineOnly() {
        val data = RepositoryData()

        val multiLineMessage = """
            feat: add new feature

            This is detailed description
        """.trimIndent()

        val commit = CommitResponse(
            CommitDetails(
                message = multiLineMessage,
                author = AuthorDetails("2025-01-01T00:00:00Z")
            )
        )

        mapper.mapCommitInfo(data, listOf(commit))

        assertThat(data.recentCommits[0].message)
            .isEqualTo("feat: add new feature")
    }

    @Test
    @DisplayName("CommitResponse 자체가 null이 리스트에 포함되어도 안전해야 함")
    fun mapCommitWithNullElement_shouldBeFiltered() {
        val data = RepositoryData()

        val validCommit = CommitResponse(
            CommitDetails(
                message = "valid commit",
                author = AuthorDetails("2025-01-01T00:00:00Z")
            )
        )

        val commits = mutableListOf<CommitResponse?>(
            validCommit,
            null
        )

        assertThatCode {
            mapper.mapCommitInfo(data, commits.filterNotNull())
        }.doesNotThrowAnyException()
    }
}
