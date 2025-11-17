package com.backend.domain.community.controller

import CommunityResponseDTO
import com.backend.domain.analysis.entity.Score
import com.backend.domain.analysis.service.AnalysisService
import com.backend.domain.community.dto.request.CommentRequestDTO
import com.backend.domain.community.dto.request.CommentUpdateRequestDTO
import com.backend.domain.community.dto.response.CommentResponseDTO
import com.backend.domain.community.dto.response.CommentWriteResponseDTO
import com.backend.domain.community.service.CommunityService
import com.backend.domain.user.service.UserService
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/community")
class CommunityController(
    val communityService: CommunityService,
    val analysisService: AnalysisService,
    val userService: UserService,
    val jwtUtil: JwtUtil
) {


    /*
    * 커뮤니티 - 레포지토리 관련 컨트롤러
    */

    // 레포지토리 조회
    @GetMapping("/repositories")
    fun getPublicRepositories(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int,
        @RequestParam(defaultValue = "latest") sort: String
    ): ResponseEntity<Page<CommunityResponseDTO>> {

        val publicRepository = communityService.getPagedRepositoriesPublicTrue(page, size)
        val communityRepositories = mutableListOf<CommunityResponseDTO>()

        publicRepository.forEach { repo ->
            val repoId = repo.id ?: return@forEach

            val analysisResult = analysisService
                .getAnalysisResultList(repoId)
                .firstOrNull() ?: return@forEach

            val score = analysisResult.score
                ?: Score.create(analysisResult, 0, 0, 0, 0)

            communityRepositories.add(
                CommunityResponseDTO(repo, analysisResult, score)
            )
        }

        // 정렬 분기
        when (sort) {
            "score" -> communityRepositories.sortByDescending { it.totalScore }
            else -> communityRepositories.sortByDescending { it.createDate }   // latest
        }

        val pageResponseDto = PageImpl(
            communityRepositories,
            publicRepository.pageable,
            publicRepository.totalElements
        )

        return ResponseEntity.ok(pageResponseDto)
    }

    @GetMapping("/search")
    fun searchRepository(
        @RequestParam content: String,
        @RequestParam(defaultValue = "repoName") searchSort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int,
        @RequestParam(defaultValue = "latest") sort: String
    ): ResponseEntity<Page<CommunityResponseDTO>> {

        val searchCommunityRepositories = mutableListOf<CommunityResponseDTO>()

        val publicRepository = when (searchSort) {
            "user" -> communityService.searchPagedByUserName(content, page, size)
            else -> communityService.searchPagedByRepoName(content, page, size)
        }

        publicRepository.forEach { repo ->
            val repoId = repo.id ?: return@forEach

            val analysisResult = analysisService
                .getAnalysisResultList(repoId)
                .firstOrNull() ?: return@forEach

            val score = analysisResult.score
                ?: Score.create(analysisResult, 0, 0, 0, 0)

            searchCommunityRepositories.add(
                CommunityResponseDTO(repo, analysisResult, score)
            )
        }

        when (sort) {
            "score" -> searchCommunityRepositories.sortByDescending { it.totalScore }
            else -> searchCommunityRepositories.sortByDescending { it.createDate }   // latest
        }

        val pageResponseDto = PageImpl(
            searchCommunityRepositories,
            publicRepository.pageable,
            publicRepository.totalElements
        )

        return ResponseEntity.ok(pageResponseDto)
    }




    /*
    * 댓글 관련 컨트롤러
    */
    @GetMapping("/{analysisResultId}/comments")
    fun getCommentsByAnalysisResult(
        @PathVariable analysisResultId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int
    ): ResponseEntity<Page<CommentResponseDTO>> {

        val comments = communityService.getPagedCommentsByAnalysisResult(analysisResultId, page, size)
        val commentList = mutableListOf<CommentResponseDTO>()

        comments.forEach { comment ->

            // 프로퍼티 사용
            val user = userService.getUserNameByUserId(comment.memberId)

            commentList.add(
                CommentResponseDTO(comment, user)
            )
        }

        commentList.sortByDescending { it.commentId }

        val pagingResponseDto = PageImpl(
            commentList,
            comments.pageable,
            comments.totalElements
        )

        return ResponseEntity.ok(pagingResponseDto)
    }


    // 댓글 작성
    @PostMapping("/{analysisResultId}/write")
    fun addComment(
        @PathVariable analysisResultId: Long,
        @RequestBody requestDto: CommentRequestDTO,
        httpRequest: HttpServletRequest
    ): ResponseEntity<CommentWriteResponseDTO> {

        jwtUtil.getUserId(httpRequest)
            ?: throw BusinessException(ErrorCode.NOT_LOGIN_USER)

        val saved = communityService.addComment(
            analysisResultId,
            requestDto.memberId,
            requestDto.comment
        )

        return ResponseEntity.ok(CommentWriteResponseDTO(saved))
    }

    // 댓글 삭제
    @DeleteMapping("/delete/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<String> {

        val jwtUserId = jwtUtil.getUserId(httpRequest)
            ?: throw BusinessException(ErrorCode.NOT_LOGIN_USER)

        communityService.deleteComment(commentId, jwtUserId)

        return ResponseEntity.ok("댓글 삭제 완료")
    }


    // 댓글 수정
    @PatchMapping("/modify/{commentId}/comment")
    fun modifyComment(
        @PathVariable commentId: Long,
        @RequestBody updateDto: CommentUpdateRequestDTO,
        httpRequest: HttpServletRequest
    ): ResponseEntity<String> {

        val jwtUserId = jwtUtil.getUserId(httpRequest)
            ?: throw BusinessException(ErrorCode.NOT_LOGIN_USER)

        communityService.modifyComment(commentId, updateDto.newComment, jwtUserId)

        return ResponseEntity.ok("댓글 수정 완료")
    }
}
