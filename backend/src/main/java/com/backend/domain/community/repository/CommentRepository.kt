package com.backend.domain.community.repository

import com.backend.domain.community.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    // 기존 List로 댓글 조회
    fun findByAnalysisResultIdAndDeletedOrderByIdDesc(analysisResultId: Long, deleted: Boolean): MutableList<Comment>

    // 페이징 적용된 댓글 조회
    fun findByAnalysisResultIdAndDeletedOrderByIdDesc(
        analysisResultId: Long,
        deleted: Boolean,
        pageable: Pageable
    ): Page<Comment>


    // 댓글 작성 테스트에서 가장 최신 댓글을 확인하기 위해서 작성
    fun findTopByOrderByIdDesc(): Comment?

    // 댓글 삭제 시, 존재하는 댓글인지 확인 하기 위해서 먼저 조회
    fun findByIdAndDeleted(commentId: Long?, deleted: Boolean): Optional<Comment>
}