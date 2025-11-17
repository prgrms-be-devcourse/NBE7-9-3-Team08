import type { RepositoryItem, Comment, PageResponse } from "@/types/community"

const BACKEND_BASE =
  process.env.NEXT_PUBLIC_DEV_PROXY === "true"
    ? "/api/community"
    : `${process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080"}/api/community`

const withCommunityBase = (path: string) =>
  `${BACKEND_BASE}${path.startsWith("/") ? path : `/${path}`}`

// ✅ 공개 리포지토리 조회 (페이징)
export async function fetchRepositories(page = 0, sort = "latest") {
  const res = await fetch(
    `http://localhost:8080/api/community/repositories?page=${page}&sort=${sort}`,
    { cache: "no-store" }
  )
  return res.json()
}


// ✅ 댓글 조회 (페이징) — size 제거
export async function fetchComments(
  analysisResultId: number,
  page = 0,
): Promise<PageResponse<Comment>> {
  const res = await fetch(
    `http://localhost:8080/api/community/${analysisResultId}/comments?page=${page}`
  )
  if (!res.ok) throw new Error("댓글 조회 실패")
  return res.json()
}

// ✅ 댓글 작성
export async function postComment(analysisResultId: number, memberId: number, comment: string) {
  const res = await fetch(withCommunityBase(`/${analysisResultId}/write`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ memberId, comment }),
  })
  if (!res.ok) throw new Error("댓글 작성 실패")
  return res.json()
}

// ✅ 댓글 수정
export async function updateComment(commentId: number, newComment: string) {
  const res = await fetch(withCommunityBase(`/modify/${commentId}/comment`), {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ newComment }),
  })
  if (!res.ok) throw new Error("댓글 수정 실패")
  return res.text()
}

// ✅ 댓글 삭제
export async function deleteComment(commentId: number) {
  const res = await fetch(withCommunityBase(`/delete/${commentId}`), {
    method: "DELETE",
    credentials: "include",
  })
  if (!res.ok) throw new Error("댓글 삭제 실패")
  return res.text()
}
