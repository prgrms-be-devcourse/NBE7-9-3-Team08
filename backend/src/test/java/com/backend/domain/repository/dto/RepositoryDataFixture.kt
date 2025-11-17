package com.backend.domain.repository.dto

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.RepositoryData.*
import java.time.LocalDateTime

/**
 * RepositoryData 테스트용 Fixture
 * EvaluationServiceTest, AnalysisServiceTest 등에서 공통으로 사용
 */
object RepositoryDataFixture {

    @JvmStatic
    fun createMinimal(): RepositoryData {
        return RepositoryData(
            "test-repo",
            "https://github.com/owner/repo",
            "Test repository",
            "Java",
            LocalDateTime.now().minusMonths(6),

            null,
            0,
            0,
            mutableListOf<CommitInfo>(),

            false,
            0,
            0,
            mutableListOf<String>(),
            "",

            false,
            mutableListOf<String>(),
            false,
            mutableListOf<String>(),

            false,
            0,
            0,
            0.0,

            false,
            mutableListOf<String>(),
            false,

            0,
            0,
            0,
            0,
            mutableListOf<IssueInfo>(),
            mutableListOf<PullRequestInfo>()
        )
    }

    fun createComplete(): RepositoryData {
        return RepositoryData(
            "complete-test-repo",
            "https://github.com/owner/complete-test-repo",
            "Complete test repository for evaluation",
            "Java",
            LocalDateTime.now().minusMonths(6),

            LocalDateTime.now().minusDays(1),
            1,
            50,
            listOf(
                createCommitInfo("feat: Add new feature", LocalDateTime.now().minusDays(1)),
                createCommitInfo("fix: Bug fix", LocalDateTime.now().minusDays(2))
            ),

            true,
            500,
            3,
            listOf("Introduction", "Installation", "Usage"),
            "# Test Repository\n\n## Introduction\nThis is a test.\n\n## Installation\nRun npm install.\n\n## Usage\nRun npm start.",

            false,
            listOf(),
            true,
            listOf("pom.xml"),

            true,
            10,
            20,
            0.5,

            true,
            listOf(".github/workflows/ci.yml"),
            true,

            5,
            3,
            10,
            8,
            listOf(
                createIssueInfo(
                    "Bug fix needed",
                    "open",
                    LocalDateTime.now().minusDays(2),
                    null
                )
            ),
            listOf(
                createPullRequestInfo(
                    "Add feature X",
                    "merged",
                    LocalDateTime.now().minusDays(3),
                    LocalDateTime.now().minusDays(1)
                )
            )
        )
    }

    // ===== 헬퍼 메서드들 =====

    fun createCommitInfo(message: String, date: LocalDateTime): CommitInfo {
        return CommitInfo(message, date)
    }

    fun createIssueInfo(
        title: String,
        state: String,
        createdAt: LocalDateTime?,
        closedAt: LocalDateTime?
    ): IssueInfo {
        return IssueInfo(title, state, createdAt, closedAt)
    }

    fun createPullRequestInfo(
        title: String,
        state: String,
        createdAt: LocalDateTime,
        mergedAt: LocalDateTime?
    ): PullRequestInfo {
        return PullRequestInfo(title, state, createdAt, mergedAt)
    }
}
