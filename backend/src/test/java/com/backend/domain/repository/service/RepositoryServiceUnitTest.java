package com.backend.domain.repository.service;

import com.backend.domain.analysis.service.SseProgressNotifier;
import com.backend.domain.repository.dto.response.RepositoryData;
import com.backend.domain.repository.dto.response.github.RepoResponse;
import com.backend.domain.repository.entity.Language;
import com.backend.domain.repository.entity.Repositories;
import com.backend.domain.repository.entity.RepositoryLanguage;
import com.backend.domain.repository.repository.RepositoryJpaRepository;
import com.backend.domain.repository.repository.RepositoryLanguageRepository;
import com.backend.domain.repository.service.fetcher.GitHubDataFetcher;
import com.backend.domain.repository.service.mapper.*;
import com.backend.domain.user.entity.User;
import com.backend.domain.user.repository.UserRepository;
import com.backend.domain.user.util.JwtUtil;
import com.backend.global.exception.BusinessException;
import com.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceUnitTest {

    @InjectMocks
    private RepositoryService repositoryService;

    @Mock private RepositoryInfoMapper repositoryInfoMapper;
    @Mock private CommitInfoMapper commitInfoMapper;
    @Mock private ReadmeInfoMapper readmeInfoMapper;
    @Mock private SecurityInfoMapper securityInfoMapper;
    @Mock private TestInfoMapper testInfoMapper;
    @Mock private CicdInfoMapper cicdInfoMapper;
    @Mock private IssueInfoMapper issueInfoMapper;
    @Mock private PullRequestInfoMapper pullRequestInfoMapper;
    @Mock private RepositoryLanguageRepository repositoryLanguageRepository;

    @Mock private UserRepository userRepository;
    @Mock private GitHubDataFetcher gitHubDataFetcher;
    @Mock private RepositoryJpaRepository repositoryJpaRepository;
    @Mock private RepositoriesMapper repositoriesMapper;
    @Mock private SseProgressNotifier sseProgressNotifier;
    @Mock private JwtUtil jwtUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
                "test" + System.currentTimeMillis() + "@test.com",
                "password123",
                "Test User"
        );
    }

    private RepoResponse dummyRepo(Integer size) {
        return new RepoResponse(
                "Test Repo",
                "test/test-repo",
                false,
                "Test Description",
                "https://github.com/test/repo",
                "Java",
                "main",
                OffsetDateTime.now(),
                size
        );
    }

    /** GitHubDataFetcher의 공통 모킹 */
    private void setupBasicGitHubMocks() {
        when(gitHubDataFetcher.fetchCommitInfo(anyString(), anyString(), anyString()))
                .thenReturn(List.of());
        when(gitHubDataFetcher.fetchReadmeContent(anyString(), anyString()))
                .thenReturn("");
        when(gitHubDataFetcher.fetchRepositoryTreeInfo(anyString(), anyString(), anyString()))
                .thenReturn(null);
        when(gitHubDataFetcher.fetchIssueInfo(anyString(), anyString()))
                .thenReturn(List.of());
        when(gitHubDataFetcher.fetchPullRequestInfo(anyString(), anyString()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("기존 저장소가 없으면 새로 저장됨")
    void saveNewRepository() {
        RepoResponse repo = dummyRepo(100);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(gitHubDataFetcher.fetchRepositoryInfo(anyString(), anyString())).thenReturn(repo);
        when(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), anyLong()))
                .thenReturn(null);
        when(gitHubDataFetcher.fetchLanguages(anyString(), anyString()))
                .thenReturn(Map.of("Java", 12345));

        setupBasicGitHubMocks();

        Repositories newRepo = Repositories.Companion.create(
                testUser,
                "test-repo",
                "Test Description",
                "https://github.com/test/repo",
                true,
                "main",
                List.of()
        );

        when(repositoriesMapper.toEntity(any(), any())).thenReturn(newRepo);
        when(repositoryJpaRepository.save(any())).thenReturn(newRepo);

        RepositoryData result = repositoryService.fetchAndSaveRepository("a", "b", 1L);

        verify(repositoryJpaRepository, times(1)).save(any());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("기존 저장소가 존재하면 save 없이 update만 실행")
    void updateExistingRepository() {
        RepoResponse repo = dummyRepo(100);

        // 먼저 Repositories 생성
        Repositories existing = Repositories.Companion.create(
                testUser,
                "existing-repo",
                "Existing Description",
                "https://github.com/test/existing-repo",
                true,
                "main",
                List.of()
        );

        // 그 다음 Language 추가 (addLanguage 메서드 사용)
        RepositoryLanguage lang = new RepositoryLanguage(existing, Language.JAVA);
        existing.addLanguage(lang);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(gitHubDataFetcher.fetchRepositoryInfo(anyString(), anyString())).thenReturn(repo);
        when(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), anyLong()))
                .thenReturn(existing);
        when(gitHubDataFetcher.fetchLanguages(anyString(), anyString()))
                .thenReturn(Map.of("Java", 999));

        setupBasicGitHubMocks();

        repositoryService.fetchAndSaveRepository("a", "b", 1L);

        verify(repositoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장소 크기가 null이면 검증 통과")
    void validateSizeNull() {
        RepoResponse repo = dummyRepo(null);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(gitHubDataFetcher.fetchRepositoryInfo(anyString(), anyString())).thenReturn(repo);
        when(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), anyLong()))
                .thenReturn(null);
        when(gitHubDataFetcher.fetchLanguages(anyString(), anyString()))
                .thenReturn(Map.of());

        setupBasicGitHubMocks();

        Repositories newRepo = Repositories.Companion.create(
                testUser,
                "test-repo",
                "Test Description",
                "https://github.com/test/repo",
                true,
                "main",
                List.of()
        );

        when(repositoriesMapper.toEntity(any(), any())).thenReturn(newRepo);
        when(repositoryJpaRepository.save(any())).thenReturn(newRepo);

        assertThatCode(() ->
                repositoryService.fetchAndSaveRepository("a", "b", 1L)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("저장소 크기가 제한(1MB) 이하이면 검증 통과")
    void validateSizeOk() {
        RepoResponse repo = dummyRepo(500_000);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(gitHubDataFetcher.fetchRepositoryInfo(anyString(), anyString())).thenReturn(repo);
        when(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), anyLong()))
                .thenReturn(null);
        when(gitHubDataFetcher.fetchLanguages(anyString(), anyString()))
                .thenReturn(Map.of());

        setupBasicGitHubMocks();

        Repositories newRepo = Repositories.Companion.create(
                testUser,
                "test-repo",
                "Test Description",
                "https://github.com/test/repo",
                true,
                "main",
                List.of()
        );

        when(repositoriesMapper.toEntity(any(), any())).thenReturn(newRepo);
        when(repositoryJpaRepository.save(any())).thenReturn(newRepo);

        assertThatCode(() ->
                repositoryService.fetchAndSaveRepository("a", "b", 1L)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("저장소 크기가 1MB 초과 시 GITHUB_REPO_TOO_LARGE 예외 발생")
    void validateSizeTooLarge() {
        RepoResponse repo = dummyRepo(2_000_000);

        when(gitHubDataFetcher.fetchRepositoryInfo(anyString(), anyString())).thenReturn(repo);

        assertThatThrownBy(() ->
                repositoryService.fetchAndSaveRepository("a", "b", 1L)
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GITHUB_REPO_TOO_LARGE);
    }

    @Test
    @DisplayName("SSE 전송 실패해도 메인 로직은 계속 진행됨")
    void safeSendSse_shouldNotInterruptFlow() {
        RepoResponse repo = dummyRepo(10);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(gitHubDataFetcher.fetchRepositoryInfo(anyString(), anyString())).thenReturn(repo);
        when(repositoryJpaRepository.findByHtmlUrlAndUserId(anyString(), anyLong()))
                .thenReturn(null);
        when(gitHubDataFetcher.fetchLanguages(anyString(), anyString()))
                .thenReturn(Map.of());

        setupBasicGitHubMocks();

        Repositories newRepo = Repositories.Companion.create(
                testUser,
                "test-repo",
                "Test Description",
                "https://github.com/test/repo",
                true,
                "main",
                List.of()
        );

        when(repositoriesMapper.toEntity(any(), any())).thenReturn(newRepo);
        when(repositoryJpaRepository.save(any())).thenReturn(newRepo);

        doThrow(new RuntimeException("SSE failed"))
                .when(sseProgressNotifier)
                .notify(anyLong(), anyString(), anyString());

        assertThatCode(() ->
                repositoryService.fetchAndSaveRepository("a", "b", 1L)
        ).doesNotThrowAnyException();

        verify(repositoryJpaRepository, times(1)).save(any());
    }

    // ------------------------------------------------------------
    // getUserRepositories()
    // ------------------------------------------------------------

    @Test
    @DisplayName("JWT userId로 해당 사용자 저장소 목록 반환")
    void getUserRepositories_ok() throws Exception {
        when(jwtUtil.getUserId(any())).thenReturn(1L);

        // 실제 User 객체 생성 및 id 설정
        User user1 = new User("test1@test.com", "password", "Test User 1");
        setField(user1, "id", 1L);

        // 실제 Repositories 객체 생성
        Repositories r1 = Repositories.Companion.create(
                user1,
                "repo1",
                "Description 1",
                "https://github.com/test/repo1",
                true,
                "main",
                List.of()
        );
        setField(r1, "id", 1L);
        setField(r1, "createDate", LocalDateTime.now());

        // 언어 추가
        RepositoryLanguage lang1 = new RepositoryLanguage(r1, Language.JAVA);
        r1.addLanguage(lang1);

        Repositories r2 = Repositories.Companion.create(
                user1,
                "repo2",
                "Description 2",
                "https://github.com/test/repo2",
                false,
                "main",
                List.of()
        );
        setField(r2, "id", 2L);
        setField(r2, "createDate", LocalDateTime.now());

        // 언어 추가
        RepositoryLanguage lang2 = new RepositoryLanguage(r2, Language.PYTHON);
        r2.addLanguage(lang2);

        when(repositoryJpaRepository.findByUserId(1L))
                .thenReturn(List.of(r1, r2));

        var result = repositoryService.getUserRepositories(mock());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("repo1");
        assertThat(result.get(0).getPublicRepository()).isTrue();
        assertThat(result.get(0).getLanguages()).containsExactly("JAVA");
        assertThat(result.get(1).getName()).isEqualTo("repo2");
        assertThat(result.get(1).getPublicRepository()).isFalse();
        assertThat(result.get(1).getLanguages()).containsExactly("PYTHON");

        verify(repositoryJpaRepository, times(1)).findByUserId(1L);
    }

    // Reflection 헬퍼 메서드 추가 (테스트 클래스 하단에)
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();

        // BaseEntity의 필드도 접근 가능하도록 상위 클래스까지 탐색
        while (clazz != null) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
    }

    @Test
    @DisplayName("JWT userId 추출 실패 시 UNAUTHORIZED 예외 발생")
    void getUserRepositories_unauthorized() {
        when(jwtUtil.getUserId(any())).thenReturn(null);

        assertThatThrownBy(() ->
                repositoryService.getUserRepositories(mock())
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(repositoryJpaRepository, never()).findByUserId(anyLong());
    }
}