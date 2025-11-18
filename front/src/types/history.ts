// repository 도메인 타입
export interface RepositoryResponse {
  id: number
  name: string
  description: string | null
  htmlUrl: string
  publicRepository: boolean
  mainBranch: string
  languages: string[],
  createDate: string,
  latestScore?: number | null
  latestAnalysisDate?: string | null
  ownerId: number
}

// 
export interface CommunityResponse {
  userName: string | null;
  userImage: string | null;
  repositoryName: string;
  repositoryId: number;
  summary: string;
  description: string | null;
  language: string[];
  totalScore: number;
  createDate: string;
  publicStatus: boolean;
  htmlUrl: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  last: boolean;
}