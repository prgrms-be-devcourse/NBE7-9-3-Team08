package com.backend.domain.repository.service

import com.backend.domain.analysis.service.SseProgressNotifier
import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.RepositoryResponse
import com.backend.domain.repository.dto.response.github.RepoResponse
import com.backend.domain.repository.entity.Language
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.entity.RepositoryLanguage
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.repository.RepositoryLanguageRepository
import com.backend.domain.repository.service.fetcher.GitHubDataFetcher
import com.backend.domain.repository.service.mapper.*
import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class RepositoryService(
    private val userRepository: UserRepository,
    private val gitHubDataFetcher: GitHubDataFetcher,
    private val repositoriesMapper: RepositoriesMapper,
    private val repositoryInfoMapper: RepositoryInfoMapper,
    private val commitInfoMapper: CommitInfoMapper,
    private val readmeInfoMapper: ReadmeInfoMapper,
    private val securityInfoMapper: SecurityInfoMapper,
    private val testInfoMapper: TestInfoMapper,
    private val cicdInfoMapper: CicdInfoMapper,
    private val issueInfoMapper: IssueInfoMapper,
    private val pullRequestInfoMapper: PullRequestInfoMapper,
    private val repositoryJpaRepository: RepositoryJpaRepository,
    private val sseProgressNotifier: SseProgressNotifier,
    private val repositoryLanguageRepository: RepositoryLanguageRepository,
    private val jwtUtil: JwtUtil
) {
    private val log = LoggerFactory.getLogger(RepositoryService::class.java)

    @Transactional
    fun fetchAndSaveRepository(owner: String, repo: String, userId: Long): RepositoryData {
        return try {
            fetchCompleteRepositoryData(owner, repo, userId)
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("❌ Repository 수집 중 오류 발생", e)
            throw BusinessException(ErrorCode.INTERNAL_ERROR)
        }
    }

    @Transactional
    fun fetchCompleteRepositoryData(owner: String, repo: String, userId: Long): RepositoryData {
        val data = RepositoryData()

        try {
            // 1. 기본 정보 수집
            safeSendSse(userId, "status", "GitHub 연결 중")

            val repoInfo = gitHubDataFetcher.fetchRepositoryInfo(owner, repo)
                ?: throw BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND)

            validateRepositorySize(repoInfo.size)
            repositoryInfoMapper.mapBasicInfo(data, repoInfo)

            // 2. 커밋
            safeSendSse(userId, "status", "커밋 히스토리 분석")
            val since = ZonedDateTime.now(ZoneOffset.UTC)
                .minus(90, ChronoUnit.DAYS)
                .format(DateTimeFormatter.ISO_INSTANT)

            val commits = gitHubDataFetcher.fetchCommitInfo(owner, repo, since)
            commitInfoMapper.mapCommitInfo(data, commits)

            // 3. README
            safeSendSse(userId, "status", "문서화 품질 분석")
            val readme = gitHubDataFetcher.fetchReadmeContent(owner, repo) ?: ""
            readmeInfoMapper.mapReadmeInfo(data, readme)

            // 4. 트리 정보
            safeSendSse(userId, "status", "보안 구성 분석")
            val tree = gitHubDataFetcher.fetchRepositoryTreeInfo(
                owner,
                repo,
                repoInfo.defaultBranch ?: "main"
            )

            securityInfoMapper.mapSecurityInfo(data, tree)

            // 5. 테스트
            safeSendSse(userId, "status", "테스트 구성 분석")
            testInfoMapper.mapTestInfo(data, tree)

            // 6. CI/CD
            safeSendSse(userId, "status", "CI/CD 설정 분석")
            cicdInfoMapper.mapCicdInfo(data, tree)

            // 7. Issue/PR
            safeSendSse(userId, "status", "커뮤니티 활동 분석")

            val issues = gitHubDataFetcher.fetchIssueInfo(owner, repo)
            issueInfoMapper.mapIssueInfo(data, issues)

            val prs = gitHubDataFetcher.fetchPullRequestInfo(owner, repo)
            pullRequestInfoMapper.mapPullRequestInfo(data, prs)

            // 8. Entity 저장
            saveOrUpdateRepository(repoInfo, owner, repo, userId)

            return data

        } catch (e: BusinessException) {
            safeSendSse(userId, "error", "❌ ${e.errorCode.message}")
            throw e
        } catch (e: Exception) {
            safeSendSse(userId, "error", "❌ Repository 데이터 수집 실패: ${e.message}")
            throw e
        }
    }

    private fun safeSendSse(userId: Long, event: String, message: String) {
        try {
            sseProgressNotifier.notify(userId, event, message)
        } catch (e: Exception) {
            log.warn("SSE 전송 실패: userId={}, event={}, error={}", userId, event, e.message)
        }
    }

    private fun saveOrUpdateRepository(
        repoInfo: RepoResponse,
        owner: String,
        repo: String,
        userId: Long
    ) {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val languages = gitHubDataFetcher.fetchLanguages(owner, repo)

        val url = repoInfo.htmlUrl
            ?: throw BusinessException(ErrorCode.INVALID_GITHUB_URL)

        val existing = repositoryJpaRepository.findIncludingDeleted(url, userId)

        if (existing != null) {

            if (existing.deleted) {
                existing.deleted = false
                repositoryJpaRepository.save(existing)
            }

            existing.updateFrom(repoInfo)
            existing.updateLanguagesFrom(languages)

            return
        }

        val newRepo = repositoriesMapper.toEntity(repoInfo, user)
        newRepo.updateLanguagesFrom(languages)

        repositoryJpaRepository.save(newRepo)
    }

    fun findRepositoryByUser(userId: Long): List<Repositories> =
        repositoryJpaRepository.findByUserId(userId)

    fun findById(repositoriesId: Long) =
        repositoryJpaRepository.findById(repositoriesId)

    private fun validateRepositorySize(sizeInKb: Int?) {
        val max = 1_000_000
        if (sizeInKb != null && sizeInKb > max) {
            log.warn("저장소 크기 초과: {}KB (제한 {}KB)", sizeInKb, max)
            throw BusinessException(ErrorCode.GITHUB_REPO_TOO_LARGE)
        }
    }

    fun getLanguageByRepositoriesId(repositoriesId: Long): List<Language> =
        repositoryLanguageRepository.findByRepositories_Id(repositoriesId)
            .map(RepositoryLanguage::language)

    @Transactional(readOnly = true)
    fun getUserRepositories(request: HttpServletRequest): List<RepositoryResponse> {
        val userId = jwtUtil.getUserId(request)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        return findRepositoryByUser(userId)
            .map { RepositoryResponse.from(it) }
    }
}
