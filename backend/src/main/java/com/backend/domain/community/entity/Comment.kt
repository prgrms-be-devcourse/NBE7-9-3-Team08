package com.backend.domain.community.entity

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.global.entity.BaseEntity
import jakarta.persistence.*
import lombok.AccessLevel
import lombok.Builder
import lombok.NoArgsConstructor
import org.hibernate.annotations.SQLDelete

@Entity
@Builder  // service는 java라서 추가, 추후 삭제 예정
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE Comment SET deleted = true WHERE id = ?") // 조회 시에 기본적으로 deleted = false인 것을만 조회하도록 설정 -> 관리자 조회 시에는 삭제 처리 된 것도 조회 필요
class Comment(
    // 댓글 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 분석결과 id
    @ManyToOne(optional = false)
    @JoinColumn(name = "analysis_result_id", nullable = false)
    val analysisResult: AnalysisResult,

    // 회원 id
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    // 댓글 내용
    @Column(columnDefinition = "TEXT", nullable = false)
    var comment: String = "",

    // 삭제 여부
    @Column(nullable = false)
    var deleted : Boolean = false

) : BaseEntity() {
    companion object {
        @JvmStatic
        fun create(
            analysisResult: AnalysisResult,
            memberId: Long,
            content: String,
            deleted: Boolean = false
        ): Comment {
            require(content.isNotBlank()) { "댓글 내용은 비어 있을 수 없습니다." }

            return Comment(
                analysisResult = analysisResult,
                memberId = memberId,
                comment = content,
                deleted = deleted
            )
        }
    }


    fun updateComment(newContent: String) {
        require(newContent.isBlank()) { "댓글 내용은 비어 있을 수 없습니다." }
        this.comment = newContent
    }
}
