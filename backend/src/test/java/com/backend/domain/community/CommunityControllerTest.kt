package com.backend.domain.community

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.analysis.repository.ScoreRepository
import com.backend.domain.analysis.service.AnalysisService
import com.backend.domain.community.entity.Comment
import com.backend.domain.community.repository.CommentRepository
import com.backend.domain.community.service.CommunityService
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.repository.RepositoryLanguageRepository
import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import com.backend.domain.user.service.UserService
import com.backend.domain.user.util.JwtUtil
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest(properties = ["spring.security.enabled=false"])
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CommunityControllerTest(

    @Autowired val mockMvc: MockMvc,
    @Autowired val userRepository: UserRepository,
    @Autowired val repositoryJpaRepository: RepositoryJpaRepository,
    @Autowired val analysisResultRepository: AnalysisResultRepository,
    @Autowired val commentRepository: CommentRepository,
    @Autowired val scoreRepository: ScoreRepository,
    @Autowired val repositoryLanguageRepository: RepositoryLanguageRepository,
    @Autowired val communityService: CommunityService,
    @Autowired val analysisService: AnalysisService
) {

    @MockkBean
    lateinit var jwtUtil: JwtUtil

    @MockkBean
    lateinit var userService: UserService

    lateinit var user: User
    lateinit var repo: Repositories
    lateinit var analysis: AnalysisResult

    @BeforeEach
    fun setup() {
        // DB 초기화
        commentRepository.deleteAllInBatch()
        scoreRepository.deleteAllInBatch()
        repositoryLanguageRepository.deleteAllInBatch()
        analysisResultRepository.deleteAllInBatch()
        repositoryJpaRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()

        // 사용자 생성
        val tempUser = User("tester@test.com", "1234", "테스터")
        val imageField = User::class.java.getDeclaredField("imageUrl")
        imageField.isAccessible = true
        imageField.set(tempUser, "test.png")
        user = userRepository.save(tempUser)

        // 🔥 JWT Mocking — 로그인된 사용자처럼 설정
        every { jwtUtil.getUserId(any()) } returns user.id
        every { userService.getUserNameByUserId(any()) } returns
                User("mock@test.com", "1234", "mock-user")

        // Repository 생성
        repo = repositoryJpaRepository.save(
            Repositories.create(user, "test-repo", "github.com", "설명", true, "main")
        )

        // Analysis 생성
        analysis = analysisResultRepository.save(
            AnalysisResult.create(repo, "요약", "강점", "개선", LocalDateTime.now())
        )

        // Score 생성
        scoreRepository.save(
            Score.create(analysis, 10, 20, 30, 40)
        )
    }

    // -------------------------------
    // 1. 공개 리포지토리 조회
    // -------------------------------
    @Test
    @DisplayName("공개 리포지토리 조회 성공")
    fun getPublicRepositories_success() {
        mockMvc.perform(get("/api/community/repositories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].repositoryName").value("test-repo"))
            .andExpect(jsonPath("$.content[0].publicStatus").value(true))
    }

    // -------------------------------
    // 2. 댓글 작성
    // -------------------------------
    @Test
    @DisplayName("댓글 작성 성공")
    fun writeComment_success() {

        val body = """
            {
              "memberId": ${user.id},
              "comment": "첫 댓글"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/community/${analysis.id}/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.comment").value("첫 댓글"))
    }

    // -------------------------------
    // 3. 댓글 조회
    // -------------------------------
    @Test
    @DisplayName("댓글 조회 성공")
    fun getComments_success() {

        commentRepository.save(Comment.create(analysis, user.id, "보이는 댓글", false))
        commentRepository.save(Comment.create(analysis, user.id, "삭제된 댓글", true))

        mockMvc.perform(get("/api/community/${analysis.id}/comments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].comment").value("보이는 댓글"))
    }

    // -------------------------------
    // 4. 댓글 페이징 조회
    // -------------------------------
    @Test
    @DisplayName("댓글 페이징 조회 성공")
    fun getComments_paging_success() {

        for (i in 1..7) {
            commentRepository.save(Comment.create(analysis, user.id, "댓글 $i", false))
        }

        mockMvc.perform(
            get("/api/community/${analysis.id}/comments")
                .param("page", "0")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.totalElements").value(7))
    }

    // -------------------------------
    // 5. 댓글 수정
    // -------------------------------
    @Test
    @DisplayName("댓글 수정 성공")
    fun modifyComment_success() {

        val comment = commentRepository.save(Comment.create(analysis, user.id, "기존", false))

        val body = """{"newComment":"수정됨"}"""

        mockMvc.perform(
            patch("/api/community/modify/${comment.id}/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("댓글 수정 완료"))
    }

    // -------------------------------
    // 6. 댓글 삭제
    // -------------------------------
    @Test
    @DisplayName("댓글 삭제 성공")
    fun deleteComment_success() {

        val comment = commentRepository.save(Comment.create(analysis, user.id, "삭제 대상", false))

        mockMvc.perform(delete("/api/community/delete/${comment.id}"))
            .andExpect(status().isOk)
            .andExpect(content().string("댓글 삭제 완료"))

        val deleted = commentRepository.findByIdAndDeleted(comment.id, true).orElseThrow()

        assert(deleted.deleted)
    }
}
