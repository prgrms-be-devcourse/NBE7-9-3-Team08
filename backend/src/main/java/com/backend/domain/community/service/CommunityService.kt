package com.backend.domain.community.service

import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.community.entity.Comment
import com.backend.domain.community.entity.Comment.Companion.create
import com.backend.domain.community.repository.CommentRepository
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommunityService (
    val repositoryJpaRepository: RepositoryJpaRepository,
    val analysisResultRepository: AnalysisResultRepository,
    val commentRepository: CommentRepository
){
    val repositoriesPublicTrue: List<Repositories?>
        // 커뮤니티 - 리포지토리 조회
       get() = repositoryJpaRepository.findByPublicRepository(true)

    //
    fun getReposByScore(): List<Repositories> =
        repositoryJpaRepository.findAllOrderByScoreDesc()

    fun getReposByLatest(): List<Repositories> =
        repositoryJpaRepository.findAllOrderByLatestAnalysis()


    // 댓글 추가
    fun addComment(analysisResultId: Long, memberId: Long, content: String): Comment {
        val analysisResult = analysisResultRepository.findById(analysisResultId)
            .orElseThrow{ BusinessException(ErrorCode.ANALYSIS_NOT_FOUND) }

        if (content.trim { it <= ' ' }.isEmpty()) {
            throw BusinessException(ErrorCode.EMPTY_COMMENT)
        }

        val comment = create(analysisResult, memberId, content, false)

        return commentRepository.save<Comment>(comment)
    }

    // 댓글 조회
    fun getCommentsByAnalysisResult(analysisResultId: Long): MutableList<Comment> {
        // id 내림차순으로 정렬된 댓글 리스트 반환
        return commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(analysisResultId, false)
    }

    // 댓글 조회 - 페이징 추가
    fun getPagedCommentsByAnalysisResult(analysisResultId: Long, page: Int, size: Int): Page<Comment> {
        val pageable = PageRequest.of(page, size, Sort.by("id").descending())
        return commentRepository.findByAnalysisResultIdAndDeletedOrderByIdDesc(analysisResultId, false, pageable)
    }

    // ✅ 댓글 삭제 (본인만 가능)
    fun deleteComment(commentId: Long, jwtUserId: Long) {

        val targetComment = commentRepository.findByIdAndDeleted(commentId, false)
            .orElseThrow{ BusinessException(ErrorCode.COMMENT_NOT_FOUND) }

        // 🔒 작성자 본인 확인
        if (targetComment.memberId != jwtUserId) {
            throw BusinessException(ErrorCode.NOT_WRITER) // 권한 없음
        }

        commentRepository.delete(targetComment)
    }

    // 댓글 수정
    @Transactional // ✅ 트랜잭션 readOnly=false로 override
    fun modifyComment(commentId: Long, newContent: String, jwtUserId: Long) {
        val targetComment = commentRepository.findById(commentId)
            .orElseThrow{ BusinessException(ErrorCode.COMMENT_NOT_FOUND) }

        // 🔒 작성자 본인 확인
        if (targetComment.memberId != jwtUserId) {
            throw BusinessException(ErrorCode.NOT_WRITER)
        }

        if (newContent.isBlank()) {
            throw BusinessException(ErrorCode.EMPTY_COMMENT)
        }

        targetComment.updateComment(newContent) // ✅ 엔티티 변경 감지
    }

    // 커뮤니티 - 레포지토리  검색
    fun searchPagedByRepoName(content: String, page: Int, size: Int): Page<Repositories> {

        val pageable = PageRequest.of(page, size, Sort.by("createDate").descending())

        return repositoryJpaRepository.findByNameContainingIgnoreCaseAndPublicRepositoryTrue(content, pageable)
    }


    // 🔍 작성자 이름 기준 검색
    fun searchPagedByUserName(content: String, page: Int, size: Int): Page<Repositories> {

        val pageable = PageRequest.of(page, size, Sort.by("createDate").descending())

        return repositoryJpaRepository.findByUser_NameContainingIgnoreCaseAndPublicRepositoryTrue(content, pageable)
    }

}
