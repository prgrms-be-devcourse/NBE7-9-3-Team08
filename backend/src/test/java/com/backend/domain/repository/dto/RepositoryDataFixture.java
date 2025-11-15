package com.backend.domain.repository.dto;

import com.backend.domain.repository.dto.response.RepositoryData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RepositoryData 테스트용 Fixture
 * EvaluationServiceTest, AnalysisServiceTest 등에서 공통으로 사용
 */
public class RepositoryDataFixture {

    /**
     * 최소한의 필수 필드만 채운 RepositoryData
     */
    public static RepositoryData createMinimal() {
        // Kotlin data class는 기본값이 있으므로 파라미터 없이 생성 가능
        RepositoryData data = new RepositoryData(
                "test-repo",  // repositoryName
                "https://github.com/owner/test-repo",  // repositoryUrl
                "Test repository",  // description
                "Java",  // primaryLanguage
                LocalDateTime.now().minusMonths(6),  // repositoryCreatedAt
                null,  // lastCommitDate
                0,  // daysSinceLastCommit
                0,  // commitCountLast90Days
                List.of(),  // recentCommits
                false,  // hasReadme
                0,  // readmeLength
                0,  // readmeSectionCount
                List.of(),  // readmeSectionTitles
                "",  // readmeContent
                false,  // hasSensitiveFile
                List.of(),  // sensitiveFilePaths
                false,  // hasBuildFile
                List.of(),  // buildFiles
                false,  // hasTestDirectory
                0,  // testFileCount
                0,  // sourceFileCount
                0.0,  // testCoverageRatio
                false,  // hasCICD
                List.of(),  // cicdFiles
                false,  // hasDockerfile
                0,  // issueCountLast6Months
                0,  // closedIssueCountLast6Months
                0,  // pullRequestCountLast6Months
                0,  // mergedPullRequestCountLast6Months
                List.of(),  // recentIssues
                List.of()  // recentPullRequests
        );

        return data;
    }

    /**
     * 모든 필드가 채워진 완전한 RepositoryData
     */
    public static RepositoryData createComplete() {
        return new RepositoryData(
                // ===== 메타정보 =====
                "complete-test-repo",
                "https://github.com/owner/complete-test-repo",
                "Complete test repository for evaluation",
                "Java",
                LocalDateTime.now().minusMonths(6),

                // ===== 1. 유지보수성 =====
                LocalDateTime.now().minusDays(1),
                1,
                50,
                List.of(
                        createCommitInfo("feat: Add new feature", LocalDateTime.now().minusDays(1)),
                        createCommitInfo("fix: Bug fix", LocalDateTime.now().minusDays(2))
                ),

                // ===== 2. 문서화 품질 =====
                true,
                500,
                3,
                List.of("Introduction", "Installation", "Usage"),
                "# Test Repository\n\n## Introduction\nThis is a test.\n\n## Installation\nRun npm install.\n\n## Usage\nRun npm start.",

                // ===== 3. 보안 =====
                false,
                List.of(),
                true,
                List.of("pom.xml"),

                // ===== 4. 테스트 구성 =====
                true,
                10,
                20,
                0.5,

                // ===== 5. CI/CD =====
                true,
                List.of(".github/workflows/ci.yml"),
                true,

                // ===== 6. 커뮤니티 활성도 =====
                5,
                3,
                10,
                8,
                List.of(createIssueInfo("Bug fix needed", "open", LocalDateTime.now().minusDays(2), null)),
                List.of(createPullRequestInfo("Add feature X", "merged", LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(1)))
        );
    }

    // ===== 헬퍼 메서드들 =====

    public static RepositoryData.CommitInfo createCommitInfo(String message, LocalDateTime date) {
        return new RepositoryData.CommitInfo(message, date);
    }

    public static RepositoryData.IssueInfo createIssueInfo(String title, String state, LocalDateTime createdAt, LocalDateTime closedAt) {
        return new RepositoryData.IssueInfo(title, state, createdAt, closedAt);
    }

    public static RepositoryData.PullRequestInfo createPullRequestInfo(String title, String state, LocalDateTime createdAt, LocalDateTime mergedAt) {
        return new RepositoryData.PullRequestInfo(title, state, createdAt, mergedAt);
    }
}