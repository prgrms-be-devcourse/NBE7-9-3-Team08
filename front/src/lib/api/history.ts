// repository 도메인 API
import { http } from './client';
import type { RepositoryResponse, CommunityResponse, PageResponse } from '@/types/history';

export async function fetchHistory(memberId: number): Promise<RepositoryResponse[]> {
  void memberId;
  return http.get<RepositoryResponse[]>('/analysis/repositories');
}

export async function fetchMyRepoSearch(
  page: number,
  size: number,
  content: string,
  sort: string
): Promise<PageResponse<CommunityResponse>> {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
    content,
    sort,
  }).toString();

  return http.get<PageResponse<CommunityResponse>>(`/analysis/search/myRepo?${query}`);
}
