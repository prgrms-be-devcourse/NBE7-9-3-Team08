package com.backend.domain.community

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.community.entity.Comment
import com.backend.domain.community.entity.Comment.Companion.create
import com.backend.domain.community.repository.CommentRepository
import com.backend.domain.community.service.CommunityService
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.user.entity.User
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import org.assertj.core.util.Lists.emptyList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class CommunityServiceTest {

    @Mock
    lateinit var repositoryJpaRepository: RepositoryJpaRepository

    @Mock
    lateinit var commentRepository: CommentRepository

    @Mock
    lateinit var analysisResultRepository: AnalysisResultRepository

    @InjectMocks
    lateinit var communityService: CommunityService


    // =======================================================
    // Reflection — ID 설정용
    // =======================================================
    private fun setId(target: Any, id: Long?) {
        val f = target.javaClass.getDeclaredField("id")
        f.isAccessible = true
        f.set(target, id)
    }

    private fun createAnalysisResult(id: Long?): AnalysisResult {

        val mockRepo = Mockito.mock(Repositories::class.java)

        val ar = AnalysisResult.create(
            repositories = mockRepo,
            summary = "sum",
            strengths = "str",
            improvements = "imp",
            createDate = LocalDateTime.now()
        )
        setId(ar, id)
        return ar
    }

    // =======================================================
    // 공개 Repository 조회
    // =======================================================
    @Test
    @DisplayName("커뮤니티 분석결과 조회 성공 - 공개된 리포지토리 목록 반환")
    fun getCommunityRepositoryList_success() {
        val tempUser = User("tester@test.com", "1234", "테스터")

        val repo1 = Repositories.create(null, "Repo1", "", "url1", true, "main")

        val repo2 = Repositories.create(null, "Repo2", "", "url2", true, "main")

        Mockito.`when`(repositoryJpaRepository.findByPublicRepository(true))
            .thenReturn(listOf(repo1, repo2))

        val result = communityService.repositoriesPublicTrue

        assertEquals(2, result.size)
        Mockito.verify(repositoryJpaRepository, Mockito.times(1))
            .findByPublicRepository(true)
    }

    @Test
    @DisplayName("공개 레포지토리가 없으면 빈 리스트 반환")
    fun getCommunityRepositoryList_empty() {
        Mockito.`when`(repositoryJpaRepository.findByPublicRepository(true))
            .thenReturn(emptyList())

        val result = communityService.repositoriesPublicTrue

        assertTrue(result.isEmpty())
    }

    // =======================================================
    // 댓글 조회 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 조회 성공 - 최신순 & SoftDelete 조건 적용")
    fun getComments_success() {
        val ar = createAnalysisResult(1L)
        val c1 = create(ar, 1L, "first", false)
        val c2 = create(ar, 1L, "second", false)

        Mockito.`when`(
            commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(
                1L, false
            )
        ).thenReturn(listOf(c2, c1) as MutableList<Comment>?)

        val result = communityService.getCommentsByAnalysisResult(1L)

        assertEquals(2, result.size)
        assertEquals("second", result[0].comment)
    }

    @Test
    @DisplayName("댓글 조회 - 없을 경우 빈 리스트")
    fun getComments_empty() {
        Mockito.`when`(
            commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(
                99L, false
            )
        ).thenReturn(emptyList())

        val result = communityService.getCommentsByAnalysisResult(99L)

        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("댓글 조회 - 페이징 정상 작동")
    fun getComments_paging_success() {
        val ar = createAnalysisResult(1L)
        val c1 = create(ar, 1L, "one", false)
        val c2 = create(ar, 2L, "two", false)

        val list = listOf(c2, c1)

        val pageable = PageRequest.of(0, 2, Sort.by("id").descending())
        val page = PageImpl(list, pageable, list.size.toLong())

        Mockito.`when`(
            commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(
                1L, false, pageable
            )
        ).thenReturn(page)

        val result = communityService.getPagedCommentsByAnalysisResult(1L, 0, 2)

        assertEquals(2, result.content.size)
        assertEquals("two", result.content[0].comment)
    }

    // =======================================================
    // 댓글 작성 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 작성 성공")
    fun writeComment_success() {
        val ar = createAnalysisResult(1L)

        Mockito.`when`(analysisResultRepository.findById(1L))
            .thenReturn(Optional.of(ar))

        val saved = create(ar, 1L, "hello", false)

        Mockito.`when`(commentRepository.save(Mockito.any(Comment::class.java)))
            .thenReturn(saved)

        val result = communityService.addComment(1L, 1L, "hello")

        assertEquals("hello", result.comment)
    }

    @Test
    @DisplayName("댓글 작성 실패 - 분석결과 없음")
    fun writeComment_noAnalysisResult_throws() {
        Mockito.`when`(analysisResultRepository.findById(1L))
            .thenReturn(Optional.empty())

        assertThrows<BusinessException> {
            communityService.addComment(1L, 1L, "내용")
        }
    }

    @Test
    @DisplayName("댓글 작성 실패 - 내용 없음")
    fun writeComment_emptyContent() {
        val ar = createAnalysisResult(1L)

        Mockito.`when`(analysisResultRepository.findById(1L))
            .thenReturn(Optional.of(ar))

        assertThrows<BusinessException> {
            communityService.addComment(1L, 1L, "")
        }
    }

    // =======================================================
    // 댓글 수정 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 수정 성공")
    fun modifyComment_success() {
        val ar = createAnalysisResult(1L)
        val comment = create(ar, 10L, "old", false)

        Mockito.`when`(commentRepository.findById(1L))
            .thenReturn(Optional.of(comment))

        communityService.modifyComment(1L, "new content", 10L)

        assertEquals("new content", comment.comment)
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 없음")
    fun modifyComment_notFound() {
        Mockito.`when`(commentRepository.findById(1L))
            .thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            communityService.modifyComment(1L, "new", 10L)
        }

        assertEquals(ErrorCode.COMMENT_NOT_FOUND, ex.errorCode)
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자 아님")
    fun modifyComment_notWriter() {
        val ar = createAnalysisResult(1L)
        val comment = create(ar, 10L, "old", false)

        Mockito.`when`(commentRepository.findById(1L))
            .thenReturn(Optional.of(comment))

        val ex = assertThrows<BusinessException> {
            communityService.modifyComment(1L, "new", 99L)
        }

        assertEquals(ErrorCode.NOT_WRITER, ex.errorCode)
    }

    @Test
    @DisplayName("댓글 수정 실패 - 빈 내용")
    fun modifyComment_emptyContent() {
        val ar = createAnalysisResult(1L)
        val comment = create(ar, 10L, "old", false)

        Mockito.`when`(commentRepository.findById(1L))
            .thenReturn(Optional.of(comment))

        val ex = assertThrows<BusinessException> {
            communityService.modifyComment(1L, "", 10L)
        }

        assertEquals(ErrorCode.EMPTY_COMMENT, ex.errorCode)
    }

    // =======================================================
    // 댓글 삭제 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 삭제 성공")
    fun deleteComment_success() {
        val ar = createAnalysisResult(1L)
        val comment = create(ar, 10L, "hello", false)

        Mockito.`when`(commentRepository.findByIdAndDeleted(1L, false))
            .thenReturn(Optional.of(comment))

        communityService.deleteComment(1L, 10L)

        Mockito.verify(commentRepository, Mockito.times(1)).delete(comment)
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    fun deleteComment_notFound() {
        Mockito.`when`(commentRepository.findByIdAndDeleted(1L, false))
            .thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            communityService.deleteComment(1L, 10L)
        }

        assertEquals(ErrorCode.COMMENT_NOT_FOUND, ex.errorCode)
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자 아님")
    fun deleteComment_notWriter() {
        val ar = createAnalysisResult(1L)
        val comment = create(ar, 10L, "hello", false)

        Mockito.`when`(commentRepository.findByIdAndDeleted(1L, false))
            .thenReturn(Optional.of(comment))

        val ex = assertThrows<BusinessException> {
            communityService.deleteComment(1L, 99L)
        }

        assertEquals(ErrorCode.NOT_WRITER, ex.errorCode)
        Mockito.verify(commentRepository, Mockito.never())
            .delete(Mockito.any(Comment::class.java))
    }

    // -------------------------------------------------------
    // 레포지토리 검색 테스트 추가
    // -------------------------------------------------------
    @Test
    @DisplayName("검색 - 레포지토리 이름 기준 검색 성공 (페이징 O)")
    fun searchPagedByRepoName_success() {

        // given
        val pageable = PageRequest.of(0, 5, Sort.by("createDate").descending())

        val repo1 = Repositories.create(null, "Spring Boot App", "desc", "url1", true, "main")
        val repo2 = Repositories.create(null, "Spring Boot Advanced", "desc2", "url2", true, "main")

        val mockPage = PageImpl(listOf(repo1, repo2), pageable, 2)

        Mockito.`when`(
            repositoryJpaRepository.findByNameContainingIgnoreCaseAndPublicRepositoryTrue(
                "spring",
                pageable
            )
        ).thenReturn(mockPage)

        // when
        val result = communityService.searchPagedByRepoName("spring", 0, 5)

        // then
        assertEquals(2, result.content.size)
        assertEquals("Spring Boot App", result.content[0].name)
        Mockito.verify(repositoryJpaRepository, Mockito.times(1))
            .findByNameContainingIgnoreCaseAndPublicRepositoryTrue("spring", pageable)
    }

    @Test
    @DisplayName("검색 - 작성자 이름 기준 검색 성공 (페이징 O)")
    fun searchPagedByUserName_success() {

        // given
        val pageable = PageRequest.of(0, 5, Sort.by("createDate").descending())

        val user = User("alice@test.com", "pw", "Alice")

        val repo1 = Repositories.create(user, "Repo1", "desc", "url1", true, "main")
        val repo2 = Repositories.create(user, "Repo2", "desc2", "url2", true, "main")

        val mockPage = PageImpl(listOf(repo1, repo2), pageable, 2)

        Mockito.`when`(
            repositoryJpaRepository.findByUser_NameContainingIgnoreCaseAndPublicRepositoryTrue(
                "Alice",
                pageable
            )
        ).thenReturn(mockPage)

        // when
        val result = communityService.searchPagedByUserName("Alice", 0, 5)

        // then
        assertEquals(2, result.content.size)
        assertEquals("Repo1", result.content[0].name)
        Mockito.verify(repositoryJpaRepository, Mockito.times(1))
            .findByUser_NameContainingIgnoreCaseAndPublicRepositoryTrue("Alice", pageable)
    }
}
