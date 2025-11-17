package com.backend.domain.repository.service

import com.backend.domain.analysis.service.SseProgressNotifier
import com.backend.domain.repository.dto.response.github.RepoResponse
import com.backend.domain.repository.entity.Language
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.entity.RepositoryLanguage
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.repository.RepositoryLanguageRepository
import com.backend.domain.repository.service.fetcher.GitHubDataFetcher
import com.backend.domain.repository.service.mapper.*
import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.time.OffsetDateTime

@ExtendWith(io.mockk.junit5.MockKExtension::class)
class RepositoryServiceUnitTest {

    @MockK lateinit var repositoryInfoMapper: RepositoryInfoMapper
    @MockK lateinit var commitInfoMapper: CommitInfoMapper
    @MockK lateinit var readmeInfoMapper: ReadmeInfoMapper
    @MockK lateinit var securityInfoMapper: SecurityInfoMapper
    @MockK lateinit var testInfoMapper: TestInfoMapper
    @MockK lateinit var cicdInfoMapper: CicdInfoMapper
    @MockK lateinit var issueInfoMapper: IssueInfoMapper
    @MockK lateinit var pullRequestInfoMapper: PullRequestInfoMapper

    @MockK lateinit var repositoryLanguageRepository: RepositoryLanguageRepository
    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var gitHubDataFetcher: GitHubDataFetcher
    @MockK lateinit var repositoryJpaRepository: RepositoryJpaRepository
    @MockK lateinit var repositoriesMapper: RepositoriesMapper
    @MockK lateinit var sseProgressNotifier: SseProgressNotifier
    @MockK lateinit var jwtUtil: JwtUtil

    @InjectMockKs
    lateinit var repositoryService: RepositoryService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "test${System.currentTimeMillis()}@test.com",
            password = "password123",
            name = "Test User"
        )
    }

    @BeforeEach
    fun initMappers() {
        every { repositoryInfoMapper.mapBasicInfo(any(), any()) } just runs
        every { commitInfoMapper.mapCommitInfo(any(), any()) } just runs
        every { readmeInfoMapper.mapReadmeInfo(any(), any()) } just runs
        every { securityInfoMapper.mapSecurityInfo(any(), any()) } just runs
        every { testInfoMapper.mapTestInfo(any(), any()) } just runs
        every { cicdInfoMapper.mapCicdInfo(any(), any()) } just runs
        every { issueInfoMapper.mapIssueInfo(any(), any()) } just runs
        every { pullRequestInfoMapper.mapPullRequestInfo(any(), any()) } just runs
        every { sseProgressNotifier.notify(any(), any(), any()) } just runs
    }


    private fun dummyRepo(size: Int?): RepoResponse =
        RepoResponse(
            name = "Test Repo",
            fullName = "test/test-repo",
            isPrivate = false,
            description = "Test Description",
            htmlUrl = "https://github.com/test/repo",
            language = "Java",
            defaultBranch = "main",
            createdAt = OffsetDateTime.now(),
            size = size
        )

    private fun setupBasicGitHubMocks() {
        every { gitHubDataFetcher.fetchCommitInfo(any(), any(), any()) } returns emptyList()
        every { gitHubDataFetcher.fetchReadmeContent(any(), any()) } returns ""
        every { gitHubDataFetcher.fetchRepositoryTreeInfo(any(), any(), any()) } returns null
        every { gitHubDataFetcher.fetchIssueInfo(any(), any()) } returns emptyList()
        every { gitHubDataFetcher.fetchPullRequestInfo(any(), any()) } returns emptyList()
    }

    @Test
    @DisplayName("기존 저장소가 없으면 새로 저장됨")
    fun `새로운 저장소는 save가 호출된다`() {
        val repo = dummyRepo(100)

        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)
        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo
        every { repositoryJpaRepository.findIncludingDeleted(any(), any()) } returns null
        every { gitHubDataFetcher.fetchLanguages(any(), any()) } returns mapOf("Java" to 12345)
        setupBasicGitHubMocks()

        val newRepo = Repositories.create(
            user = testUser,
            name = "test-repo",
            description = "Test Description",
            htmlUrl = "https://github.com/test/repo",
            publicRepository = true,
            mainBranch = "main",
            languages = emptyList()
        )

        every { repositoriesMapper.toEntity(any(), any()) } returns newRepo
        every { repositoryJpaRepository.save(any()) } returns newRepo

        val result = repositoryService.fetchAndSaveRepository("a", "b", 1L)

        verify(exactly = 1) { repositoryJpaRepository.save(any()) }
        assertThat(result).isNotNull
    }

    @Test
    @DisplayName("기존 저장소 존재 시 save 없이 update만 실행")
    fun `기존 저장소는 save를 호출하지 않고 업데이트만 한다`() {
        val repo = dummyRepo(100)

        val existing = Repositories.create(
            user = testUser,
            name = "existing-repo",
            description = "Existing Description",
            htmlUrl = "https://github.com/test/existing-repo",
            publicRepository = true,
            mainBranch = "main",
            languages = emptyList()
        )
        existing.addLanguage(RepositoryLanguage(existing, Language.JAVA))

        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)
        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo
        every { repositoryJpaRepository.findIncludingDeleted(any(), any()) } returns existing
        every { gitHubDataFetcher.fetchLanguages(any(), any()) } returns mapOf("Java" to 999)
        setupBasicGitHubMocks()

        repositoryService.fetchAndSaveRepository("a", "b", 1L)

        verify(exactly = 0) { repositoryJpaRepository.save(any()) }
    }

    @Test
    @DisplayName("삭제된 저장소가 다시 분석되면 deleted=false로 복구된다")
    fun `soft delete 저장소는 복구된다`() {
        val repo = dummyRepo(100)

        val existing = Repositories.create(
            user = testUser,
            name = "soft-deleted-repo",
            description = "Deleted Description",
            htmlUrl = "https://github.com/test/deleted-repo",
            publicRepository = true,
            mainBranch = "main",
            languages = emptyList()
        )
        existing.deleted = true

        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)
        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo
        every { repositoryJpaRepository.findIncludingDeleted(any(), any()) } returns existing
        every { gitHubDataFetcher.fetchLanguages(any(), any()) } returns mapOf("Java" to 1000)

        every { repositoryJpaRepository.save(any()) } answers { firstArg() }

        setupBasicGitHubMocks()

        repositoryService.fetchAndSaveRepository("a", "b", 1L)

        assertThat(existing.deleted).isFalse()
    }


    @Test
    @DisplayName("저장소 크기 null → 검증 통과")
    fun `저장소 크기 null 은 예외 없이 통과`() {
        val repo = dummyRepo(null)

        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)
        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo
        every { repositoryJpaRepository.findIncludingDeleted(any(), any()) } returns null
        every { gitHubDataFetcher.fetchLanguages(any(), any()) } returns emptyMap()
        setupBasicGitHubMocks()

        val newRepo = Repositories.create(
            user = testUser,
            name = "test-repo",
            description = "Test Description",
            htmlUrl = "https://github.com/test/repo",
            publicRepository = true,
            mainBranch = "main"
        )
        every { repositoriesMapper.toEntity(any(), any()) } returns newRepo
        every { repositoryJpaRepository.save(any()) } returns newRepo

        repositoryService.fetchAndSaveRepository("a", "b", 1L)
    }

    @Test
    @DisplayName("저장소 크기가 1MB 이하이면 통과")
    fun `저장소 크기 1MB 이하면 예외 없음`() {
        val repo = dummyRepo(500_000)

        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)
        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo
        every { repositoryJpaRepository.findIncludingDeleted(any(), any()) } returns null
        every { gitHubDataFetcher.fetchLanguages(any(), any()) } returns emptyMap()
        setupBasicGitHubMocks()

        val newRepo = Repositories.create(
            user = testUser,
            name = "test-repo",
            description = "Test Description",
            htmlUrl = "https://github.com/test/repo",
            publicRepository = true,
            mainBranch = "main"
        )

        every { repositoriesMapper.toEntity(any(), any()) } returns newRepo
        every { repositoryJpaRepository.save(any()) } returns newRepo

        repositoryService.fetchAndSaveRepository("a", "b", 1L)
    }

    @Test
    @DisplayName("저장소 크기 1MB 초과 시 예외 발생")
    fun `저장소 크기 초과 시 예외 발생`() {
        val repo = dummyRepo(2_000_000)

        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo

        assertThatThrownBy {
            repositoryService.fetchAndSaveRepository("a", "b", 1L)
        }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GITHUB_REPO_TOO_LARGE)
    }

    @Test
    @DisplayName("SSE 실패해도 흐름은 중단되지 않음")
    fun `SSE 실패는 로직을 방해하지 않는다`() {
        val repo = dummyRepo(10)

        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)
        every { gitHubDataFetcher.fetchRepositoryInfo(any(), any()) } returns repo
        every { repositoryJpaRepository.findIncludingDeleted(any(), any()) } returns null
        every { gitHubDataFetcher.fetchLanguages(any(), any()) } returns emptyMap()
        setupBasicGitHubMocks()

        val newRepo = Repositories.create(
            user = testUser,
            name = "test-repo",
            description = "Test Description",
            htmlUrl = "https://github.com/test/repo",
            publicRepository = true,
            mainBranch = "main"
        )

        every { repositoriesMapper.toEntity(any(), any()) } returns newRepo
        every { repositoryJpaRepository.save(any()) } returns newRepo

        every { sseProgressNotifier.notify(any(), any(), any()) } throws RuntimeException("SSE failed")

        repositoryService.fetchAndSaveRepository("a", "b", 1L)

        verify(exactly = 1) { repositoryJpaRepository.save(any()) }
    }

    @Test
    @DisplayName("JWT userId 기반으로 저장소 목록 반환")
    fun `userId 기반으로 저장소 목록 조회`() {
        every { jwtUtil.getUserId(any()) } returns 1L

        val user = User("test1@test.com", "password", "Test User 1").apply {
            setFieldValue("id", 1L)
        }

        val r1 = Repositories.create(
            user = user,
            name = "repo1",
            description = "Description 1",
            htmlUrl = "https://github.com/test/repo1",
            publicRepository = true,
            mainBranch = "main"
        ).apply {
            setFieldValue("id", 1L)
            setFieldValue("createDate", LocalDateTime.now())
            addLanguage(RepositoryLanguage(this, Language.JAVA))
        }

        val r2 = Repositories.create(
            user = user,
            name = "repo2",
            description = "Description 2",
            htmlUrl = "https://github.com/test/repo2",
            publicRepository = false,
            mainBranch = "main"
        ).apply {
            setFieldValue("id", 2L)
            setFieldValue("createDate", LocalDateTime.now())
            addLanguage(RepositoryLanguage(this, Language.PYTHON))
        }

        every { repositoryJpaRepository.findByUserId(1L) } returns listOf(r1, r2)

        val result = repositoryService.getUserRepositories(mockk())

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("repo1")
        assertThat(result[0].publicRepository).isTrue()
        assertThat(result[0].languages).containsExactly("JAVA")

        assertThat(result[1].name).isEqualTo("repo2")
        assertThat(result[1].publicRepository).isFalse()
        assertThat(result[1].languages).containsExactly("PYTHON")
    }

    @Test
    @DisplayName("JWT userId 없음 → UNAUTHORIZED")
    fun `userId 없으면 예외`() {
        every { jwtUtil.getUserId(any()) } returns null

        assertThatThrownBy {
            repositoryService.getUserRepositories(mockk())
        }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)

        verify(exactly = 0) { repositoryJpaRepository.findByUserId(any()) }
    }

    private fun Any.setFieldValue(field: String, value: Any?) {
        var clazz: Class<*>? = javaClass

        while (clazz != null) {
            try {
                val f = clazz.getDeclaredField(field)
                f.isAccessible = true
                f.set(this, value)
                return
            } catch (ignored: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        error("Field $field not found")
    }
}
