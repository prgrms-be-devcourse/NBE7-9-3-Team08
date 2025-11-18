import type {
  RepositoryItem,
  Comment,
  PageResponse,
} from "@/types/community";
import { resolveApiUrl } from "./client";

const withCommunityBase = (path: string) => {
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return resolveApiUrl(`/community${suffix}`);
};

export async function fetchRepositories(page = 0, sort = "latest") {
  const res = await fetch(withCommunityBase(`/repositories?page=${page}&sort=${sort}`), {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("리포지토리 조회 실패");
  return res.json();
}

export async function fetchComments(
  analysisResultId: number,
  page = 0
): Promise<PageResponse<Comment>> {
  const res = await fetch(withCommunityBase(`/${analysisResultId}/comments?page=${page}`));
  if (!res.ok) throw new Error("댓글 조회 실패");
  return res.json();
}

export async function postComment(analysisResultId: number, memberId: number, comment: string) {
  const res = await fetch(withCommunityBase(`/${analysisResultId}/write`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ memberId, comment }),
  });
  if (!res.ok) throw new Error("댓글 작성 실패");
  return res.json();
}

export async function updateComment(commentId: number, newComment: string) {
  const res = await fetch(withCommunityBase(`/modify/${commentId}/comment`), {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ newComment }),
  });
  if (!res.ok) throw new Error("댓글 수정 실패");
  return res.text();
}

export async function deleteComment(commentId: number) {
  const res = await fetch(withCommunityBase(`/delete/${commentId}`), {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) throw new Error("댓글 삭제 실패");
  return res.text();
}

export async function searchRepositories(params: {
  content: string
  searchSort?: "repoName" | "user"
  page?: number
  size?: number
  sort?: "latest" | "score"; 
}) {
  const {
    content,
    searchSort = "repoName",
    page = 0,
    size = 5,
    sort = "latest"
  } = params

  const query = new URLSearchParams({
    content,
    searchSort,
    page: String(page),
    size: String(size),
    sort
  })

  const res = await fetch(
    withCommunityBase(`/search?${query.toString()}`),
    { method: "GET", cache: "no-store" }
  )

  if (!res.ok) throw new Error("검색 조회 실패")

  return res.json()
}