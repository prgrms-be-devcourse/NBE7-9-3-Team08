package com.backend.domain.community;

import com.backend.domain.analysis.entity.AnalysisResult;
import com.backend.domain.analysis.repository.AnalysisResultRepository;
import com.backend.domain.community.entity.Comment;
import com.backend.domain.community.repository.CommentRepository;
import com.backend.domain.community.service.CommunityService;
import com.backend.domain.repository.entity.Repositories;
import com.backend.domain.repository.repository.RepositoryJpaRepository;
import com.backend.global.exception.BusinessException;
import com.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @InjectMocks
    private CommunityService communityService;

    // =======================================================
    // Reflection 유틸
    // =======================================================
    private void setId(Object target, Long id) {
        try {
            Field f = target.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AnalysisResult createAnalysisResult(Long id) {
        AnalysisResult ar = new AnalysisResult(
                mock(Repositories.class),
                "sum", "str", "imp",
                LocalDateTime.now()
        );
        setId(ar, id);
        return ar;
    }

    // =======================================================
    // 공개 레포지토리 조회
    // =======================================================
    @Test
    @DisplayName("커뮤니티 분석결과 조회 성공 - 공개된 리포지토리 목록 반환")
    void getCommunityRepositoryList_success() {

        Repositories repo1 = Repositories.builder()
                .name("Repo1").htmlUrl("url1").publicRepository(true)
                .user(null)
                .build();

        Repositories repo2 = Repositories.builder()
                .name("Repo2").htmlUrl("url2").publicRepository(true)
                .user(null)
                .build();

        when(repositoryJpaRepository.findByPublicRepository(true))
                .thenReturn(List.of(repo1, repo2));

        List<Repositories> result = communityService.getRepositoriesPublicTrue();

        assertEquals(2, result.size());
        verify(repositoryJpaRepository, times(1)).findByPublicRepository(true);
    }

    @Test
    @DisplayName("커뮤니티 분석결과 조회 - 공개 레포지토리가 없으면 빈 리스트 반환")
    void getCommunityRepositoryList_empty() {

        when(repositoryJpaRepository.findByPublicRepository(true))
                .thenReturn(Collections.emptyList());

        List<Repositories> result = communityService.getRepositoriesPublicTrue();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =======================================================
    // 댓글 조회 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 조회 성공 - SoftDelete 조건으로 최신순 반환")
    void getComments_success() {

        AnalysisResult ar = createAnalysisResult(1L);

        Comment c1 = Comment.create(ar, 1L, "first", false);
        Comment c2 = Comment.create(ar, 1L, "second", false);

        when(commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(1L, false))
                .thenReturn(List.of(c2, c1));

        List<Comment> result = communityService.getCommentsByAnalysisResult(1L);

        assertEquals(2, result.size());
        assertEquals("second", result.get(0).getComment());
    }

    @Test
    @DisplayName("댓글 조회 - 존재하지 않으면 빈 리스트 반환")
    void getComments_empty() {

        when(commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(99L, false))
                .thenReturn(Collections.emptyList());

        List<Comment> result = communityService.getCommentsByAnalysisResult(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("댓글 조회 - 페이징 정상 작동")
    void getComments_paging_success() {

        AnalysisResult ar = createAnalysisResult(1L);

        Comment c1 = Comment.create(ar, 1L, "one", false);
        Comment c2 = Comment.create(ar, 2L, "two", false);
        List<Comment> list = List.of(c2, c1);

        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
        Page<Comment> page = new PageImpl<>(list, pageable, list.size());

        when(commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(1L, false, pageable))
                .thenReturn(page);

        Page<Comment> result = communityService.getPagedCommentsByAnalysisResult(1L, 0, 2);

        assertEquals(2, result.getContent().size());
        assertEquals("two", result.getContent().get(0).getComment());
    }

    // =======================================================
    // 댓글 작성 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 작성 성공")
    void writeComment_success() {

        AnalysisResult ar = createAnalysisResult(1L);

        when(analysisResultRepository.findById(1L))
                .thenReturn(Optional.of(ar));

        Comment saved = Comment.create(ar, 1L, "hello", false);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        Comment result = communityService.addComment(1L, 1L, "hello");

        assertEquals("hello", result.getComment());
    }

    @Test
    @DisplayName("댓글 작성 실패 - 분석결과 없음")
    void writeComment_noAnalysisResult_throws() {

        when(analysisResultRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> communityService.addComment(1L, 1L, "내용"));
    }

    @Test
    @DisplayName("댓글 작성 실패 - 내용 없음")
    void writeComment_emptyContent() {

        AnalysisResult ar = createAnalysisResult(1L);

        when(analysisResultRepository.findById(1L))
                .thenReturn(Optional.of(ar));

        assertThrows(BusinessException.class,
                () -> communityService.addComment(1L, 1L, ""));
    }

    // =======================================================
    // 댓글 수정 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 수정 성공 - 작성자 본인")
    void modifyComment_success() {

        AnalysisResult ar = createAnalysisResult(1L);
        Comment comment = Comment.create(ar, 10L, "old", false);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        communityService.modifyComment(1L, "new content", 10L);

        assertEquals("new content", comment.getComment());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 없음")
    void modifyComment_notFound() {

        when(commentRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.modifyComment(1L, "new", 10L)
        );

        assertEquals(ErrorCode.COMMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자 아님")
    void modifyComment_notWriter() {

        AnalysisResult ar = createAnalysisResult(1L);
        Comment comment = Comment.create(ar, 10L, "old", false);

        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.modifyComment(1L, "new", 99L)
        );

        assertEquals(ErrorCode.NOT_WRITER, ex.getErrorCode());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 내용 없음")
    void modifyComment_emptyContent() {

        AnalysisResult ar = createAnalysisResult(1L);
        Comment comment = Comment.create(ar, 10L, "old", false);

        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.modifyComment(1L, "", 10L)
        );

        assertEquals(ErrorCode.EMPTY_COMMENT, ex.getErrorCode());
    }

    // =======================================================
    // 댓글 삭제 테스트
    // =======================================================
    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {

        AnalysisResult ar = createAnalysisResult(1L);
        Comment comment = Comment.create(ar, 10L, "hello", false);

        when(commentRepository.findByIdAndDeleted(1L, false))
                .thenReturn(Optional.of(comment));

        communityService.deleteComment(1L, 10L);

        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    void deleteComment_notFound() {

        when(commentRepository.findByIdAndDeleted(1L, false))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.deleteComment(1L, 10L)
        );

        assertEquals(ErrorCode.COMMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자 아님")
    void deleteComment_notWriter() {

        AnalysisResult ar = createAnalysisResult(1L);
        Comment comment = Comment.create(ar, 10L, "hello", false);

        when(commentRepository.findByIdAndDeleted(1L, false))
                .thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.deleteComment(1L, 99L)
        );

        assertEquals(ErrorCode.NOT_WRITER, ex.getErrorCode());
        verify(commentRepository, never()).delete(any());
    }
}
