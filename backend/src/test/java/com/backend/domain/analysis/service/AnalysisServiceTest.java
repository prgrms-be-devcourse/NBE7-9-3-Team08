package com.backend.domain.analysis.service;

import com.backend.domain.analysis.lock.RedisLockManager;
import com.backend.domain.evaluation.service.EvaluationService;
import com.backend.domain.repository.dto.RepositoryDataFixture;
import com.backend.domain.repository.dto.response.RepositoryData;
import com.backend.domain.repository.entity.Repositories;
import com.backend.domain.repository.repository.RepositoryJpaRepository;
import com.backend.domain.repository.service.RepositoryService;
import com.backend.domain.user.util.JwtUtil;
import com.backend.global.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AnalysisServiceTest {

    @Autowired
    private AnalysisService analysisService;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private EvaluationService evaluationService;

    @MockitoBean
    private SseProgressNotifier sseProgressNotifier;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RedisLockManager lockManager;

    @MockitoBean
    private RepositoryJpaRepository repositoryJpaRepository;

    private MockHttpServletRequest createAuthenticatedRequest(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie jwtCookie = new Cookie("accessToken", "fake-jwt-token");
        request.setCookies(jwtCookie);
        given(jwtUtil.getUserId(request)).willReturn(userId);
        return request;
    }

    @Test
    @DisplayName("analyze → 수집 후 evaluateAndSave 한 번 호출")
    void analyze_callsEvaluateOnce() {
        // given
        String url = "https://github.com/owner/repo";
        Long userId = 1L;

        MockHttpServletRequest request = createAuthenticatedRequest(userId);
        given(lockManager.tryLock(anyString())).willReturn(true);

        RepositoryData fake = RepositoryDataFixture.createMinimal();

        given(repositoryService.fetchAndSaveRepository("owner", "repo", userId))
                .willReturn(fake);

        Repositories mockRepo = mock(Repositories.class);
        given(mockRepo.getId()).willReturn(10L);

        // Kotlin version → Repositories?
        given(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), eq(userId)))
                .willReturn(mockRepo);

        // when
        analysisService.analyze(url, request);

        // then
        ArgumentCaptor<RepositoryData> captor = ArgumentCaptor.forClass(RepositoryData.class);

        then(evaluationService).should(times(1))
                .evaluateAndSave(captor.capture(), eq(userId));

        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("analyze → 잘못된 URL이면 evaluateAndSave 호출 안 함")
    void analyze_invalidUrl_doesNotCallEvaluate() {
        Long userId = 1L;
        MockHttpServletRequest request = createAuthenticatedRequest(userId);

        assertThatThrownBy(() ->
                analysisService.analyze("https://notgithub.com/owner/repo", request)
        ).isInstanceOf(BusinessException.class);

        then(repositoryService).shouldHaveNoInteractions();
        then(evaluationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("analyze → 인증되지 않은 사용자는 분석 불가")
    void analyze_unauthenticated_throwsException() {
        String url = "https://github.com/owner/repo";

        MockHttpServletRequest request = new MockHttpServletRequest();
        given(jwtUtil.getUserId(request)).willReturn(null);

        assertThatThrownBy(() -> analysisService.analyze(url, request))
                .isInstanceOf(BusinessException.class);

        then(repositoryService).shouldHaveNoInteractions();
        then(evaluationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("analyze → 중복 분석 요청 시 락 획득 실패")
    void analyze_duplicateRequest_throwsException() {
        String url = "https://github.com/owner/repo";
        Long userId = 1L;
        MockHttpServletRequest request = createAuthenticatedRequest(userId);

        given(lockManager.tryLock(anyString())).willReturn(false);

        assertThatThrownBy(() -> analysisService.analyze(url, request))
                .isInstanceOf(BusinessException.class);

        then(repositoryService).shouldHaveNoInteractions();
        then(evaluationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("analyze → Repository 수집 실패 시에도 락 해제")
    void analyze_repositoryFetchFails_releasesLock() {
        String url = "https://github.com/owner/repo";
        Long userId = 1L;
        MockHttpServletRequest request = createAuthenticatedRequest(userId);

        given(lockManager.tryLock(anyString())).willReturn(true);

        given(repositoryService.fetchAndSaveRepository("owner", "repo", userId))
                .willThrow(new RuntimeException("GitHub API 실패"));

        assertThatThrownBy(() -> analysisService.analyze(url, request))
                .isInstanceOf(RuntimeException.class);

        then(lockManager).should().releaseLock(anyString());
    }

    @Test
    @DisplayName("analyze → Evaluation 실패 시에도 락 해제")
    void analyze_evaluationFails_releasesLock() {
        String url = "https://github.com/owner/repo";
        Long userId = 1L;
        MockHttpServletRequest request = createAuthenticatedRequest(userId);

        given(lockManager.tryLock(anyString())).willReturn(true);

        RepositoryData fake = RepositoryDataFixture.createMinimal();

        given(repositoryService.fetchAndSaveRepository("owner", "repo", userId))
                .willReturn(fake);

        Repositories fakeRepo = mock(Repositories.class);
        given(fakeRepo.getId()).willReturn(1L);

        given(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), eq(userId)))
                .willReturn(fakeRepo);

        doThrow(new RuntimeException("OpenAI API 실패"))
                .when(evaluationService).evaluateAndSave(any(), eq(userId));

        assertThatThrownBy(() -> analysisService.analyze(url, request))
                .isInstanceOf(RuntimeException.class);

        then(lockManager).should().releaseLock(anyString());
    }
}
