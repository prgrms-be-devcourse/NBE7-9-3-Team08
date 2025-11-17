package com.backend.domain.repository.service.fetcher

import com.backend.domain.repository.dto.response.github.*
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import com.backend.global.github.GitHubApiClient
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
@Retryable(
    retryFor = [
        WebClientResponseException.ServiceUnavailable::class,
        WebClientResponseException.InternalServerError::class,
        WebClientRequestException::class
    ],
    maxAttempts = 2,
    backoff = Backoff(delay = 1000)
)
class GitHubDataFetcher(
    private val gitHubApiClient: GitHubApiClient
) {

    fun fetchRepositoryInfo(owner: String, repoName: String): RepoResponse {
        return gitHubApiClient.get(
            "/repos/{owner}/{repo}",
            RepoResponse::class.java,
            owner,
            repoName
        ) ?: throw BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND)
    }

    fun fetchReadmeContent(owner: String, repoName: String): String? {
        return try {
            val content = gitHubApiClient.getRaw(
                "/repos/{owner}/{repo}/readme",
                owner,
                repoName
            )

            if (content.isNullOrBlank()) null else content
        } catch (e: BusinessException) {
            if (e.errorCode == ErrorCode.GITHUB_REPO_NOT_FOUND) null
            else throw e
        }
    }

    fun fetchCommitInfo(owner: String, repoName: String, since: String): List<CommitResponse> {
        return try {
            gitHubApiClient.getList(
                "/repos/{owner}/{repo}/commits?since={since}&per_page=100",
                CommitResponse::class.java,
                owner,
                repoName,
                since
            )
        } catch (e: BusinessException) {
            if (e.errorCode == ErrorCode.GITHUB_REPO_NOT_FOUND) emptyList()
            else throw e
        }
    }

    fun fetchRepositoryTreeInfo(owner: String, repoName: String, defaultBranch: String): TreeResponse? {
        return try {
            val tree = gitHubApiClient.get(
                "/repos/{owner}/{repo}/git/trees/{sha}?recursive=1",
                TreeResponse::class.java,
                owner,
                repoName,
                defaultBranch
            )

            return tree?.takeIf { !it.tree.isNullOrEmpty() }
        } catch (e: BusinessException) {
            if (e.errorCode == ErrorCode.GITHUB_REPO_NOT_FOUND) null
            else throw e
        }
    }

    fun fetchIssueInfo(owner: String, repoName: String): List<IssueResponse> {
        return try {
            val allIssues = gitHubApiClient.getList(
                "/repos/{owner}/{repo}/issues?state=all&per_page=100",
                IssueResponse::class.java,
                owner,
                repoName
            )

            val threshold = sixMonthsAgo

            allIssues
                .filter { it.isPureIssue }
                .filter { issue ->
                    issue.created_at?.let { dateStr ->
                        parseGitHubDate(dateStr)?.isAfter(threshold) == true
                    } ?: false
                }

        } catch (e: BusinessException) {
            if (e.errorCode == ErrorCode.GITHUB_REPO_NOT_FOUND) emptyList()
            else throw e
        }
    }

    fun fetchPullRequestInfo(owner: String, repoName: String): List<PullRequestResponse> {
        return try {
            val allPullRequests = gitHubApiClient.getList(
                "/repos/{owner}/{repo}/pulls?state=all&per_page=100",
                PullRequestResponse::class.java,
                owner,
                repoName
            )

            val threshold = sixMonthsAgo

            allPullRequests.filter { pr ->
                pr.created_at?.let { dateStr ->
                    parseGitHubDate(dateStr)?.isAfter(threshold) == true
                } ?: false
            }

        } catch (e: BusinessException) {
            if (e.errorCode == ErrorCode.GITHUB_REPO_NOT_FOUND) emptyList()
            else throw e
        }
    }

    fun fetchLanguages(owner: String, repoName: String): Map<String, Int> {
        return try {
            @Suppress("UNCHECKED_CAST")
            gitHubApiClient.get(
                "/repos/{owner}/{repo}/languages",
                MutableMap::class.java,
                owner,
                repoName
            ) as? Map<String, Int> ?: emptyMap()
        } catch (e: BusinessException) {
            if (e.errorCode == ErrorCode.GITHUB_REPO_NOT_FOUND) emptyMap()
            else throw e
        }
    }

    private val sixMonthsAgo: LocalDateTime
        get() = LocalDateTime.now().minusMonths(COMMUNITY_ANALYSIS_MONTHS.toLong())

    private fun parseGitHubDate(dateString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            LocalDateTime.MIN
        }
    }

    companion object {
        private const val COMMUNITY_ANALYSIS_MONTHS = 6
        private val log = LoggerFactory.getLogger(GitHubDataFetcher::class.java)
    }
}
