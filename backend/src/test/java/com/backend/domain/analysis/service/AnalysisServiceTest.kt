package com.backend.domain.analysis.service

import com.backend.domain.repository.service.RepositoryService
import com.backend.domain.user.service.EmailService
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
    lateinit var jwtUtil: JwtUtil

    @MockkBean
    lateinit var analysisAnalyzeService: AnalysisAnalyzeService

    @MockkBean
    lateinit var emailService: EmailService

    private fun request(userId: Long): MockHttpServletRequest {
        val req = MockHttpServletRequest()
        every { jwtUtil.getUserId(req) } returns userId
        return req
    }

    @Test
    @DisplayName("analyze → ensureRepository 호출 후 runAnalysisAsync 호출")
    fun testAnalyzeNormal() {
        every { repositoryService.ensureRepository(1L, any(), any()) } returns 10L
        every { analysisAnalyzeService.runAnalysisAsync(any(), any(), any(), any()) } just Runs

        val id = analysisService.analyze(
            "https://github.com/owner/repo",
            request(1L)
        )

        assertThat(id).isEqualTo(10L)

        verify { repositoryService.ensureRepository(1L, "https://github.com/owner/repo", "repo") }
        verify { analysisAnalyzeService.runAnalysisAsync(1L, "https://github.com/owner/repo", "owner", "repo") }
    }

    @Test
    @DisplayName("잘못된 URL → ensureRepository 호출 안 함")
    fun testInvalidUrl() {
        assertThatThrownBy {
            analysisService.analyze("https://notgithub.com/owner/repo", request(1L))
        }.isInstanceOf(BusinessException::class.java)

        verify { repositoryService wasNot Called }
    }

    @Test
    @DisplayName("인증 실패 → ensureRepository 호출 안 함")
    fun testUnauthorized() {
        val req = MockHttpServletRequest()
        every { jwtUtil.getUserId(req) } returns null

        assertThatThrownBy {
            analysisService.analyze("https://github.com/owner/repo", req)
        }.isInstanceOf(BusinessException::class.java)

        verify { repositoryService wasNot Called }
        verify { analysisAnalyzeService wasNot Called }
    }
}
