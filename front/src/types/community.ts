// community/types.ts

// ✅ Repository 항목 - CommunityResponseDTO
export interface RepositoryItem {
  userName: string
  userImage: string | null
  repositoryName: string
  repositoryId: number
  summary: string
  description: string
  language: string[]
  totalScore: number
  createDate: string
  viewingStatus: boolean
  htmlUrl: string
}

// ✅ 댓글 타입 - commentResponseDTO
export interface Comment {
  commentId: number
  memberId: number
  name: string
  comment: string
  createDate: string
  deleted: boolean
  userImage: string | null
}

// ✅ 페이지네이션 응답 - PageResponseDTO
export interface PageResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
    offset: number
    paged: boolean
    unpaged: boolean
  }
  totalPages: number
  totalElements: number
  last: boolean
  first: boolean
  empty: boolean
}
