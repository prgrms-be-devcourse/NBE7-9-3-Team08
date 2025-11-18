// repository 도메인 API
import type { RepositoryResponse } from "@/types/history"
import type { CommunityResponse, PageResponse } from "@/types/history";

export async function fetchHistory(memberId: number): Promise<RepositoryResponse[]> {
  const res = await fetch(`http://localhost:8080/api/analysis/repositories`, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
    cache: "no-store",
    credentials: "include", 
  })

  if (!res.ok) throw new Error("히스토리 데이터를 불러오는 데 실패했습니다.")
  const result = await res.json();
  return result.data;
}

export async function fetchMyRepoSearch(
  page: number,
  size: number,
  content: string,
  sort: string
): Promise<PageResponse<CommunityResponse>> {
  
  const url = new URL("http://localhost:8080/api/analysis/search/myRepo");
  url.searchParams.set("page", String(page));
  url.searchParams.set("size", String(size));
  url.searchParams.set("content", content);
  url.searchParams.set("sort", sort);

  const res = await fetch(url.toString(), {
    method: "GET",
    headers: { "Content-Type": "application/json" },
    cache: "no-store",
    credentials: "include",
  });

  if (!res.ok) throw new Error("검색 데이터를 불러오는 데 실패했습니다.");

  const result = await res.json();
  return result.data; // ApiResponse.data => PageResponseDTO
}
