package com.backend.domain.community;

import com.backend.domain.analysis.entity.AnalysisResult;
import com.backend.domain.analysis.repository.AnalysisResultRepository;
import com.backend.domain.analysis.repository.ScoreRepository;
import com.backend.domain.community.entity.Comment;
import com.backend.domain.community.repository.CommentRepository;
import com.backend.domain.repository.entity.Repositories;
import com.backend.domain.repository.repository.RepositoryJpaRepository;
import com.backend.domain.repository.repository.RepositoryLanguageRepository;
import com.backend.domain.user.entity.User;
import com.backend.domain.user.repository.UserRepository;
import com.backend.domain.user.service.UserService;
import com.backend.domain.user.util.JwtUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.security.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private RepositoryLanguageRepository repositoryLanguageRepository;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private Repositories testRepo;
    private AnalysisResult testAnalysis;

    @BeforeEach
    void setup() {

        // 자식 먼저 삭제
        commentRepository.deleteAllInBatch();
        scoreRepository.deleteAllInBatch();
        repositoryLanguageRepository.deleteAllInBatch();
        analysisResultRepository.deleteAllInBatch();
        repositoryJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // 🌟 testUser 저장
        testUser = userRepository.save(new User("tester@example.com", "1234", "테스터"));

        // 🌟 Mock 설정 (userService 로직 안정화)
        when(jwtUtil.getUserId(any(HttpServletRequest.class))).thenReturn(1L);

        // userService 반환 타입 변경 가능성 → 이름만 필요 → 새 User로 Mock
        when(userService.getUserNameByUserId(anyLong()))
                .thenReturn(new User("mock@user.com", "1234", "mock-user"));

        // Repo 저장
        testRepo = repositoryJpaRepository.save(Repositories.builder()
                .user(testUser)
                .name("test-repo")
                .description("테스트용 리포지토리입니다.")
                .htmlUrl("https://github.com/test/test-repo")
                .mainBranch("main")
                .publicRepository(true)
                .build());

        // Analysis 저장
        testAnalysis = analysisResultRepository.save(AnalysisResult.builder()
                .repositories(testRepo)
                .summary("요약")
                .strengths("강점")
                .improvements("개선점")
                .createDate(LocalDateTime.now())
                .build());
    }

    // 댓글 작성 테스트
    @Test
    @DisplayName("댓글 작성 → DB 저장 성공")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void writeComment_success() throws Exception {

        String requestBody = """
                {
                  "memberId": 1,
                  "comment": "통합 테스트 댓글입니다."
                }
                """;

        mockMvc.perform(post("/api/community/" + testAnalysis.getId() + "/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("통합 테스트 댓글입니다."));

        Comment saved = commentRepository.findTopByOrderByIdDesc();
        assertThat(saved.getComment()).isEqualTo("통합 테스트 댓글입니다.");
        assertThat(saved.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("댓글 조회 → SoftDelete(false)만 반환")
    void getComments_success() throws Exception {

        commentRepository.save(Comment.create(testAnalysis, 1L, "첫 댓글", false));
        commentRepository.save(Comment.create(testAnalysis, 1L, "삭제된 댓글", true));

        mockMvc.perform(get("/api/community/" + testAnalysis.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].comment").value("첫 댓글"));
    }

    @Test
    @DisplayName("댓글 페이징 조회")
    void getComments_paging_success() throws Exception {

        for (int i = 1; i <= 7; i++) {
            commentRepository.save(Comment.create(testAnalysis, 1L, "댓글 " + i, false));
        }

        mockMvc.perform(get("/api/community/" + testAnalysis.getId() + "/comments")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(7));
    }

    @Test
    @DisplayName("댓글 수정")
    void modifyComment_success() throws Exception {

        Comment comment = commentRepository.save(Comment.create(testAnalysis, 1L, "기존 댓글", false));

        mockMvc.perform(patch("/api/community/modify/" + comment.getId() + "/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newComment\": \"수정된 댓글\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("댓글 수정 완료"));
    }

    @Test
    @DisplayName("댓글 삭제 → SoftDelete 반영")
    void deleteComment_success() throws Exception {

        Comment comment = commentRepository.save(Comment.create(testAnalysis, 1L, "삭제 대상 댓글", false));

        mockMvc.perform(delete("/api/community/delete/" + comment.getId()))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        Comment deleted = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(deleted.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("공개 리포지토리 조회")
    void getPublicRepositories_success() throws Exception {

        mockMvc.perform(get("/api/community/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].repositoryName").value("test-repo"))
                .andExpect(jsonPath("$.content[0].publicStatus").value(true));
    }

    @Test
    @DisplayName("deleted=true 댓글 제외됨")
    void getComments_excludeDeleted() throws Exception {

        commentRepository.save(Comment.create(testAnalysis, 1L, "보이는 댓글", false));
        commentRepository.save(Comment.create(testAnalysis, 1L, "삭제된 댓글", true));

        mockMvc.perform(get("/api/community/" + testAnalysis.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].comment").value("보이는 댓글"));
    }

    @Test
    @DisplayName("댓글 페이징 정상 작동")
    void getComments_pagination() throws Exception {

        for (int i = 1; i <= 3; i++) {
            commentRepository.save(Comment.create(testAnalysis, 1L, "댓글 " + i, false));
        }

        mockMvc.perform(get("/api/community/" + testAnalysis.getId() + "/comments?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
