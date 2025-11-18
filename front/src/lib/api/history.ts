// repository 도메인 API
import { http } from './client';
import type { RepositoryResponse } from "@/types/history"

export async function fetchHistory(memberId: number): Promise<RepositoryResponse[]> {
  void memberId
  return http.get<RepositoryResponse[]>('/analysis/repositories')
}