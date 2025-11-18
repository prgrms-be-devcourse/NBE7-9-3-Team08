// src/lib/api/analysis.ts
import { http, resolveApiUrl, buildBackendUrl } from "./client"
import { fetchEventSource } from "@microsoft/fetch-event-source"

import type {
  AnalysisRequest,
  AnalysisStartResponse, 
  RepositoryResponse,
  HistoryResponseDto,
  AnalysisResultResponseDto,
  RepositoryComparisonResponse
} from "@/types/analysis"

export const analysisApi = {
  requestAnalysis: (
    data: AnalysisRequest
  ): Promise<AnalysisStartResponse> =>
    http.post(`/analysis`, data),

  getUserRepositories: (): Promise<RepositoryResponse[]> =>
    http.get(`/analysis/repositories`),

  getRepositoryHistory: (
    repoId: number
  ): Promise<HistoryResponseDto> =>
    http.get(`/analysis/repositories/${repoId}`),

  getAnalysisDetail: (
    repoId: number,
    analysisId: number
  ): Promise<AnalysisResultResponseDto> =>
    http.get(`/analysis/repositories/${repoId}/results/${analysisId}`),

  deleteRepository: (
    userId: number,
    repositoryId: number
  ): Promise<void> =>
    http.delete(`/analysis/${userId}/repositories/${repositoryId}`),

  deleteAnalysisResult: (
    userId: number,
    repositoryId: number,
    analysisId: number
  ): Promise<void> =>
    http.delete(
      `/analysis/${userId}/repositories/${repositoryId}/results/${analysisId}`
    ),

  updatePublicStatus: (
    userId: number,
    repositoryId: number
  ): Promise<void> =>
    http.put(`/analysis/${userId}/repositories/${repositoryId}/public`),

  getRepositoriesForComparison: (): Promise<RepositoryComparisonResponse[]> =>
    http.get(`/analysis/comparison`),

  connectStream: (userId: number) => {
    const controller = new AbortController();
    const streamUrl = buildBackendUrl(`/api/analysis/stream/${userId}`);

    fetchEventSource(streamUrl, {
      method: "GET",
      headers: { Accept: "text/event-stream" },
      credentials: "include",
      openWhenHidden: true,
      signal: controller.signal,
      onopen: async (response) => {
        if (!response.ok) {
          if ([401, 403].includes(response.status)) {
            window.dispatchEvent(new CustomEvent("SSE_AUTH_ERROR"));
          }
          throw new Error(`SSE 연결 실패: ${response.status}`);
        }
      },
      onmessage(event) {
        window.dispatchEvent(
          new CustomEvent(`SSE_${event.event.toUpperCase()}`, {
            detail: event.data,
          })
        );
      },
      onerror(err) {
        window.dispatchEvent(new CustomEvent("SSE_ERROR", { detail: err }));
        return 1000;
      },
    });

    return () => controller.abort();
  },
};