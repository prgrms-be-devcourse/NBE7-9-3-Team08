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

    // ------------------------------------------------------
    // 커뮤니티 내 분석결과 조회 테스트
    // ------------------------------------------------------

    @Test
    @DisplayName("커뮤니티 분석결과 조회 성공 - 공개된 리포지토리 목록 반환")
    void getCommunityRepositoryList_success() {
        // given
        Repositories repo1 = Repositories.builder()
                .name("Repo1").htmlUrl("url1").publicRepository(true).build();
        Repositories repo2 = Repositories.builder()
                .name("Repo2").htmlUrl("url2").publicRepository(true).build();

        when(repositoryJpaRepository.findByPublicRepository(true))
                .thenReturn(List.of(repo1, repo2));

        // when
        List<Repositories> result = communityService.getRepositoriesPublicTrue();

        // then
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
        verify(repositoryJpaRepository, times(1)).findByPublicRepository(true);
    }

    // ------------------------------------------------------
    // 댓글 조회 테스트
    // ------------------------------------------------------

    @Test
    @DisplayName("댓글 조회 성공 - SoftDelete 적용 (deleted=false) 조건으로 최신순 반환")
    void getComments_success() {
        // given
        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();

        Comment comment1 = Comment.create(analysisResult, 1L, "first", false);
        Comment comment2 = Comment.create(analysisResult, 1L, "second", false);

        when(commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(1L, false))
                .thenReturn(List.of(comment2, comment1)); // 최신순

        // when
        List<Comment> result = communityService.getCommentsByAnalysisResult(1L);

        // then
        assertEquals(2, result.size());
        assertEquals("second", result.get(0).getComment());
        assertEquals("first", result.get(1).getComment());
        verify(commentRepository, times(1))
                .findByAnalysisResultIdAndDeletedOrderByIdDesc(1L, false);
    }

    @Test
    @DisplayName("댓글 조회 (SoftDelete 적용) - 존재하지 않는 분석결과 ID일 경우 빈 리스트 반환")
    void getComments_empty() {
        when(commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(99L, false))
                .thenReturn(Collections.emptyList());

        List<Comment> result = communityService.getCommentsByAnalysisResult(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(commentRepository, times(1))
                .findByAnalysisResultIdAndDeletedOrderByIdDesc(99L, false);
    }

    @Test
    @DisplayName("댓글 조회 - 페이징 적용, deleted=false 조건으로 조회")
    void getComments_paging_success() {
        // given
        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();

        Comment c1 = Comment.create(analysisResult, 1L, "one", false);
        Comment c2 = Comment.create(analysisResult, 2L, "two", false);
        List<Comment> list = List.of(c2, c1);

        Page<Comment> page = new PageImpl<>(list);
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());

        when(commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(1L, false, pageable))
                .thenReturn(page);

        // when
        Page<Comment> result = communityService.getPagedCommentsByAnalysisResult(1L, 0, 2);

        // then
        assertEquals(2, result.getContent().size());
        assertEquals("two", result.getContent().getFirst().getComment());
        verify(commentRepository, times(1))
                .findByAnalysisResultIdAndDeletedOrderByIdDesc(1L, false, pageable);
    }

    // ------------------------------------------------------
    // 댓글 작성 테스트
    // ------------------------------------------------------

    @Test
    @DisplayName("댓글 작성 성공 - 정상 데이터로 댓글 저장")
    void writeComment_success() {
        // given
        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();
        when(analysisResultRepository.findById(1L))
                .thenReturn(Optional.of(analysisResult));

        Comment saved = Comment.create(analysisResult, 1L, "write ok", false);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        // when
        Comment result = communityService.addComment(1L, 1L, "write ok");

        // then
        assertNotNull(result);
        assertEquals("write ok", result.getComment());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 작성 실패 - AnalysisResult 없음")
    void writeComment_noAnalysisResult_throws() {
        when(analysisResultRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> communityService.addComment(1L, 1L, "내용"));
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글 작성 실패 - 내용 비어있음")
    void writeComment_emptyContent_throws() {
        AnalysisResult ar = AnalysisResult.builder().id(1L).build();
        when(analysisResultRepository.findById(1L))
                .thenReturn(Optional.of(ar));

        assertThrows(BusinessException.class,
                () -> communityService.addComment(1L, 1L, ""));
    }

    // ------------------------------------------------------
    // 댓글 수정 테스트
    // ------------------------------------------------------

    @Test
    @DisplayName("댓글 수정 성공 - 작성자 본인일 때 수정됨")
    void modifyComment_success() {

        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();
        Comment comment = Comment.create(analysisResult, 10L, "old", false);

        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment));

        communityService.modifyComment(1L, "new content", 10L);

        assertEquals("new content", comment.getComment());
    }


    @Test
    @DisplayName("댓글 수정 실패 - 댓글이 존재하지 않음")
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
    @DisplayName("댓글 수정 실패 - 작성자가 아님")
    void modifyComment_notWriter() {

        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();
        Comment comment = Comment.create(analysisResult, 10L, "old", false);

        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.modifyComment(1L, "new", 99L)
        );

        assertEquals(ErrorCode.NOT_WRITER, ex.getErrorCode());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 내용이 비어있음")
    void modifyComment_emptyContent() {

        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();
        Comment comment = Comment.create(analysisResult, 10L, "old", false);

        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.modifyComment(1L, "", 10L)
        );

        assertEquals(ErrorCode.EMPTY_COMMENT, ex.getErrorCode());
    }


    // ------------------------------------------------------
    // 댓글 삭제 테스트 (Soft Delete)
    // ------------------------------------------------------

    @Test
    @DisplayName("댓글 삭제 성공 - 작성자 본인일 때 삭제됨")
    void deleteComment_success() {
        // given
        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();
        Comment comment = Comment.create(analysisResult, 10L, "hello", false);

        when(commentRepository.findByIdAndDeleted(1L, false))
                .thenReturn(Optional.of(comment));

        // when
        communityService.deleteComment(1L, 10L);

        // then
        verify(commentRepository, times(1)).delete(comment);
    }


    @Test
    @DisplayName("댓글 삭제 실패 - commentId가 null")
    void deleteComment_invalidInput() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.deleteComment(null, 10L)
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글이 존재하지 않음")
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
    @DisplayName("댓글 삭제 실패 - 작성자가 아님")
    void deleteComment_notWriter() {
        AnalysisResult analysisResult = AnalysisResult.builder().id(1L).build();
        Comment comment = Comment.create(analysisResult, 10L, "hello", false);

        when(commentRepository.findByIdAndDeleted(1L, false))
                .thenReturn(Optional.of(comment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> communityService.deleteComment(1L, 99L)  // 다른 사용자
        );

        assertEquals(ErrorCode.NOT_WRITER, ex.getErrorCode());
        verify(commentRepository, never()).delete(any());
    }
}
