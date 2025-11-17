package com.backend.domain.analysis.service

import com.backend.domain.analysis.lock.RedisLockManager
import com.backend.domain.evaluation.service.EvaluationService
import com.backend.domain.repository.dto.RepositoryDataFixture
import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.service.RepositoryService
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AnalysisServiceTest {

    @Autowired
    lateinit var analysisService: AnalysisService

    @MockkBean
    lateinit var repositoryService: RepositoryService

    @MockkBean
    lateinit var evaluationService: EvaluationService

    @MockkBean
    lateinit var sseProgressNotifier: SseProgressNotifier

    @MockkBean
    lateinit var jwtUtil: JwtUtil

    @MockkBean
    lateinit var lockManager: RedisLockManager

    @MockkBean
    lateinit var repositoryJpaRepository: RepositoryJpaRepository

    @BeforeEach
    fun setupMocks() {
        every { jwtUtil.getUserId(any()) } returns 1L

        every { lockManager.tryLock(any()) } returns true
        every { lockManager.refreshLock(any()) } just runs
        every { lockManager.releaseLock(any()) } just runs

        every { sseProgressNotifier.notify(any(), any(), any()) } just runs
    }


    private fun createAuthenticatedRequest(userId: Long): MockHttpServletRequest {
        val request = MockHttpServletRequest()
        request.setCookies(Cookie("accessToken", "fake-token"))

        every { jwtUtil.getUserId(request) } returns userId
        return request
    }

    @Test
    fun `evaluateAndSave 한번 호출`() {
        val userId = 1L

        val slotData = slot<RepositoryData>()

        every { lockManager.tryLock(any()) } returns true

        every { repositoryService.fetchAndSaveRepository("owner", "repo", userId) }
            .returns(RepositoryDataFixture.createMinimal())

        val fakeRepo = mockk<Repositories>()
        every { fakeRepo.id } returns 10L
        every { repositoryJpaRepository.findByHtmlUrlAndUserId(any(), eq(userId)) } returns fakeRepo

        every { evaluationService.evaluateAndSave(capture(slotData), eq(userId)) } returns 99L

        val request = createAuthenticatedRequest(userId)

        analysisService.analyze("https://github.com/owner/repo", request)

        assertThat(slotData.captured).isNotNull
    }

    @Test
    @DisplayName("analyze → 잘못된 URL이면 evaluateAndSave 호출 안 함")
    fun `잘못된 URL이면 evaluate 호출 안 함`() {
        val request = createAuthenticatedRequest(1L)

        assertThatThrownBy {
            analysisService.analyze("https://notgithub.com/owner/repo", request)
        }.isInstanceOf(BusinessException::class.java)

        verify { repositoryService wasNot Called }
        verify { evaluationService wasNot Called }
    }

    @Test
    @DisplayName("analyze → 인증되지 않은 사용자는 분석 불가")
    fun `인증되지 않은 사용자는 예외`() {
        val url = "https://github.com/owner/repo"

        val request = MockHttpServletRequest()
        every { jwtUtil.getUserId(request) } returns null

        assertThatThrownBy {
            analysisService.analyze(url, request)
        }.isInstanceOf(BusinessException::class.java)

        verify { repositoryService wasNot Called }
        verify { evaluationService wasNot Called }
    }

    @Test
    @DisplayName("analyze → 중복 분석 요청 시 락 획득 실패")
    fun `중복 분석 시 락 실패`() {
        val url = "https://github.com/owner/repo"
        val userId = 1L
        val request = createAuthenticatedRequest(userId)

        every { lockManager.tryLock(any()) } returns false

        assertThatThrownBy {
            analysisService.analyze(url, request)
        }.isInstanceOf(BusinessException::class.java)

        verify { repositoryService wasNot Called }
        verify { evaluationService wasNot Called }
    }

    @Test
    @DisplayName("analyze → Repository 수집 실패 시에도 락 해제")
    fun `수집 실패해도 락 해제`() {
        val url = "https://github.com/owner/repo"
        val userId = 1L
        val request = createAuthenticatedRequest(userId)

        every { lockManager.tryLock(any()) } returns true
        every {
            repositoryService.fetchAndSaveRepository("owner", "repo", userId)
        } throws RuntimeException("GitHub API 실패")

        assertThatThrownBy {
            analysisService.analyze(url, request)
        }.isInstanceOf(RuntimeException::class.java)

        verify(exactly = 1) { lockManager.releaseLock(any()) }
    }

    @Test
    @DisplayName("analyze → Evaluation 실패 시에도 락 해제")
    fun `Evaluation 실패해도 락 해제`() {
        val url = "https://github.com/owner/repo"
        val userId = 1L
        val request = createAuthenticatedRequest(userId)

        every { lockManager.tryLock(any()) } returns true

        val fakeRepoData = RepositoryDataFixture.createMinimal()
        every { repositoryService.fetchAndSaveRepository("owner", "repo", userId) } returns fakeRepoData

        val repoEntity = mockk<Repositories>()
        every { repoEntity.id } returns 1L
        every { repositoryJpaRepository.findByHtmlUrlAndUserId(any(), eq(userId)) } returns repoEntity

        every { evaluationService.evaluateAndSave(any(), eq(userId)) } throws RuntimeException("OpenAI API 실패")

        assertThatThrownBy {
            analysisService.analyze(url, request)
        }.isInstanceOf(RuntimeException::class.java)

        verify(exactly = 1) { lockManager.releaseLock(any()) }
    }
}
