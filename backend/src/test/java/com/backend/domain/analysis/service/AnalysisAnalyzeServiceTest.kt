package com.backend.domain.analysis.service

import com.backend.domain.analysis.lock.RedisLockManager
import com.backend.domain.evaluation.service.EvaluationService
import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.service.RepositoryService
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AnalysisAnalyzeServiceTest {

    @MockK lateinit var repositoryService: RepositoryService
    @MockK lateinit var evaluationService: EvaluationService
    @MockK lateinit var repositoryJpaRepository: RepositoryJpaRepository
    @MockK lateinit var sseProgressNotifier: SseProgressNotifier
    @MockK lateinit var lockManager: RedisLockManager

    @InjectMockKs
    lateinit var executionService: AnalysisAnalyzeService

    private val dummyRepoData = RepositoryData().apply {
        repositoryUrl = "https://github.com/test/repo"
    }

    @BeforeEach
    fun setup() {
        every { sseProgressNotifier.notify(any(), any(), any()) } just Runs
        every { lockManager.refreshLock(any()) } just Runs
        every { lockManager.releaseLock(any()) } just Runs
    }

    @Test
    @DisplayName("정상 흐름: Repository → Evaluation → SSE → 락 해제")
    fun testSuccessfulAsyncFlow() {
        every { lockManager.tryLock(any()) } returns true
        every { repositoryService.fetchAndSaveRepository(any(), any(), any()) } returns dummyRepoData

        val repoEntity = mockk<Repositories>()
        every { repoEntity.id } returns 10L
        every { repositoryJpaRepository.findByHtmlUrlAndUserId(any(), any()) } returns repoEntity

        every { evaluationService.evaluateAndSave(any(), any()) } returns 1L

        executionService.runAnalysisAsync(
            userId = 1L,
            githubUrl = dummyRepoData.repositoryUrl,
            owner = "test",
            repo = "repo"
        )

        verify(exactly = 1) { repositoryService.fetchAndSaveRepository("test", "repo", 1L) }
        verify(exactly = 1) { evaluationService.evaluateAndSave(dummyRepoData, 1L) }
        verify(atLeast = 1) { lockManager.refreshLock(any()) }
        verify(exactly = 1) { lockManager.releaseLock(any()) }
        verify(atLeast = 1) { sseProgressNotifier.notify(1L, any(), any()) }
    }

    @Test
    @DisplayName("락 획득 실패 시 바로 return (예외 X) → 어떤 작업도 수행하지 않음")
    fun testLockFailure() {
        every { lockManager.tryLock(any()) } returns false

        executionService.runAnalysisAsync(
            userId = 1L,
            githubUrl = "https://github.com/test/repo",
            owner = "test",
            repo = "repo"
        )

        verify { repositoryService wasNot Called }
        verify { evaluationService wasNot Called }
        verify(exactly = 0) { lockManager.releaseLock(any()) }
    }

    @Test
    @DisplayName("Repository 수집 실패 시 SSE error 전송 + 락 해제")
    fun testRepositoryFailure() {
        every { lockManager.tryLock(any()) } returns true
        every { repositoryService.fetchAndSaveRepository(any(), any(), any()) }
            .throws(BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND))

        executionService.runAnalysisAsync(
            userId = 1L,
            githubUrl = "https://github.com/test/repo",
            owner = "test",
            repo = "repo"
        )

        verify { sseProgressNotifier.notify(1L, "error", any()) }
        verify(exactly = 1) { lockManager.releaseLock(any()) }
    }

    @Test
    @DisplayName("Evaluation 실패 시 SSE error + 락 해제")
    fun testEvaluationFailure() {
        every { lockManager.tryLock(any()) } returns true
        every { repositoryService.fetchAndSaveRepository(any(), any(), any()) } returns dummyRepoData

        val repoEntity = mockk<Repositories>()
        every { repoEntity.id } returns 99L
        every { repositoryJpaRepository.findByHtmlUrlAndUserId(any(), any()) } returns repoEntity

        every { evaluationService.evaluateAndSave(any(), any()) }
            .throws(BusinessException(ErrorCode.ANALYSIS_FAIL))

        executionService.runAnalysisAsync(
            userId = 1L,
            githubUrl = dummyRepoData.repositoryUrl,
            owner = "test",
            repo = "repo"
        )

        verify { sseProgressNotifier.notify(1L, "error", any()) }
        verify(exactly = 1) { lockManager.releaseLock(any()) }
    }
}
