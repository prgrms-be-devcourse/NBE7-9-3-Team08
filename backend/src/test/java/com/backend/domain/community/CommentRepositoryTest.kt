package com.backend.domain.community

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.AnalysisResult.Companion.builder
import com.backend.domain.community.entity.Comment
import com.backend.domain.community.entity.Comment.Companion.create
import com.backend.domain.community.repository.CommentRepository
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.*

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    lateinit var commentRepository: CommentRepository

    @Autowired
    lateinit var em: TestEntityManager

    /**
     * 테스트 기본 데이터 세팅
     */
    private fun setupData(): AnalysisResult {
        val user = User("tester@example.com", "1234", "테스터")
        em.persist(user)

        val repo = Repositories.create(user, "test-repo", "테스트용 레포지토리입니다.", "https://github.com/test/test-repo", true, "main")
        em.persist(repo)

        val analysisResult = builder()
            .repositories(repo)
            .summary("요약")
            .strengths("강점")
            .improvements("개선점")
            .createDate(LocalDateTime.now())
            .build()

        em.persist(analysisResult)
        em.flush()

        return analysisResult
    }

    @Test
    @DisplayName("analysisResultId 기준으로 삭제되지 않은 댓글을 ID 내림차순 정렬하여 조회한다")
    fun findByAnalysisResultIdAndDeletedOrderByIdDesc_success() {
        // given
        val analysisResult = setupData()

        val c1 = create(analysisResult, 10L, "첫 번째 댓글", false)
        val c2 = create(analysisResult, 10L, "두 번째 댓글", false)

        commentRepository.save(c1)
        commentRepository.save(c2)
        em.flush()

        // when
        val result: List<Comment> =
            commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(analysisResult.id!!, false)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].comment).isEqualTo("두 번째 댓글")
        assertThat(result[1].comment).isEqualTo("첫 번째 댓글")
    }

    @Test
    @DisplayName("페이징 기반으로 댓글을 조회한다 (삭제되지 않은 것만)")
    fun findByAnalysisResultIdAndDeletedOrderByIdDesc_paging() {
        // given
        val analysisResult = setupData()

        for (i in 1..5) {
            val comment = create(analysisResult, i.toLong(), "댓글 $i", false)
            commentRepository.save(comment)
        }
        em.flush()

        val pageable: Pageable = PageRequest.of(0, 3, Sort.by("id").descending())

        // when
        val pageResult: Page<Comment> =
            commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(
                analysisResult.id!!,
                false,
                pageable
            )

        // then
        assertThat(pageResult.totalElements).isEqualTo(5)
        assertThat(pageResult.content).hasSize(3)
        assertThat(pageResult.content[0].comment).isEqualTo("댓글 5")
        assertThat(pageResult.content[2].comment).isEqualTo("댓글 3")
    }

    @Test
    @DisplayName("댓글 ID와 deleted 상태로 단건 조회 및 soft delete 검증")
    fun findByIdAndDeleted_success() {
        // given
        val analysisResult = setupData()

        val comment = create(analysisResult, 99L, "삭제 테스트용 댓글", false)

        commentRepository.save(comment)
        em.flush()

        // when - 삭제 전 조회
        val found: Optional<Comment> =
            commentRepository.findByIdAndDeleted(comment.id!!, false)

        // then
        assertThat(found).isPresent
        assertThat(found.get().comment).isEqualTo("삭제 테스트용 댓글")

        // when - soft delete 수행
        commentRepository.delete(comment)
        em.flush()

        // then - 삭제 후 상태 확인
        val deleted: Optional<Comment> =
            commentRepository.findByIdAndDeleted(comment.id!!, true)

        assertThat(deleted).isPresent
        assertThat(deleted.get().deleted).isTrue()
    }
}
