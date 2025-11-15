package com.backend.domain.repository.dto.response

import java.time.LocalDateTime

// === Analysis로 넘길 핵심 DTO ===
data class RepositoryData(
    // ===== 메타정보 (기본) =====
    var repositoryName: String = "",
    var repositoryUrl: String = "",
    var description: String? = null,
    var primaryLanguage: String? = null,
    var repositoryCreatedAt: LocalDateTime = LocalDateTime.now(),

    // ===== 1. 유지보수성 (Maintainability) =====
    var lastCommitDate: LocalDateTime? = null,
    var daysSinceLastCommit: Int = 0,
    var commitCountLast90Days: Int = 0,
    var recentCommits: List<CommitInfo> = emptyList(),

    // ===== 2. 문서화 품질 (Documentation) =====
    var hasReadme: Boolean = false,
    var readmeLength: Int = 0,
    var readmeSectionCount: Int = 0,
    var readmeSectionTitles: List<String> = emptyList(),
    var readmeContent: String? = null,

    // ===== 3. 보안 (Security) =====
    var hasSensitiveFile: Boolean = false,
    var sensitiveFilePaths: List<String> = emptyList(),
    var hasBuildFile: Boolean = false,
    var buildFiles: List<String> = emptyList(),

    // ===== 4. 테스트 구성 (Testing) =====
    var hasTestDirectory: Boolean = false,
    var testFileCount: Int = 0,
    var sourceFileCount: Int = 0,
    var testCoverageRatio: Double = 0.0,

    // ===== 5. CI/CD (Build & Deployment) =====
    var hasCICD: Boolean = false,
    var cicdFiles: List<String> = emptyList(),
    var hasDockerfile: Boolean = false,

    // ===== 6. 커뮤니티 활성도 (Community) =====
    var issueCountLast6Months: Int = 0,
    var closedIssueCountLast6Months: Int = 0,
    var pullRequestCountLast6Months: Int = 0,
    var mergedPullRequestCountLast6Months: Int = 0,
    var recentIssues: List<IssueInfo> = emptyList(),
    var recentPullRequests: List<PullRequestInfo> = emptyList()
) {

    // ===== 내부 클래스 (상세 정보) =====
    data class CommitInfo(
        var message: String = "",
        var committedDate: LocalDateTime = LocalDateTime.now()
    )

    data class IssueInfo(
        var title: String = "",
        var state: String = "",
        var createdAt: LocalDateTime = LocalDateTime.now(),
        var closedAt: LocalDateTime? = null
    )

    data class PullRequestInfo(
        var title: String = "",
        var state: String = "",
        var createdAt: LocalDateTime = LocalDateTime.now(),
        var mergedAt: LocalDateTime? = null
    )
}
