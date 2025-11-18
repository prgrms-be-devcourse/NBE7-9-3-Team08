"use client"

import { useEffect, useState, useMemo } from "react"
import { analysisApi } from "@/lib/api/analysis"
import type { RepositoryResponse as RepoBaseResponse } from "@/types/history"
import type { HistoryResponseDto } from "@/types/analysis"

export function useHistory(memberId: number) {
  const [repositories, setRepositories] = useState<RepoBaseResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 🔥 기존 정렬 기준 유지
  const [sortType, setSortType] = useState<"latest" | "score">("latest")

  // 🔥 새로 추가: 검색어(content)
  const [keyword, setKeyword] = useState("")

  useEffect(() => {
    console.log("🧾 repositories:", repositories.map(r => ({
      id: r.id,
      createDate: r.createDate,
      latestScore: r.latestScore
    })))
  }, [repositories])

  
  // 전체 리스트 로드
  async function load() {
    try {
      setLoading(true)

      const baseRepos = await analysisApi.getUserRepositories()

      const enrichedRepos: RepoBaseResponse[] = await Promise.all(
        baseRepos.map(async (repo): Promise<RepoBaseResponse> => {
          try {
            const historyData: HistoryResponseDto = await analysisApi.getRepositoryHistory(repo.id)
            
            const versions = [...historyData.analysisVersions].sort(
              (a, b) => new Date(b.analysisDate).getTime() - new Date(a.analysisDate).getTime()
            )
            const latest = versions[0] ?? null

            return {
              ...repo,
              latestScore: latest?.totalScore ?? null,
              latestAnalysisDate: latest?.analysisDate ?? null,
            }
          } catch (err) {
            console.error(`❌ 점수 불러오기 실패 (repoId: ${repo.id})`, err)
            return repo
          }
        })
      )

      setRepositories(enrichedRepos)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])


  // 날짜 파싱 함수
  const parseDate = (date?: string | null) => {
    if (!date) return 0
    return Date.parse(date.split(".")[0] + "Z")
  }


  // 🔥 1) 검색 필터 적용
  const filteredRepositories = useMemo(() => {
    if (!keyword.trim()) return repositories

    const lower = keyword.toLowerCase()
    return repositories.filter(repo =>
      repo.name.toLowerCase().includes(lower) ||
      repo.description?.toLowerCase().includes(lower)
    )
  }, [repositories, keyword])


  // 🔥 2) 정렬 적용 (기존 로직 유지)
  const sortedRepositories = useMemo(() => {
    if (sortType === "score") {
      return [...filteredRepositories].sort((a, b) => (b.latestScore ?? 0) - (a.latestScore ?? 0))
    }

    return [...filteredRepositories].sort(
      (a, b) =>
        parseDate(b.latestAnalysisDate ?? b.createDate) -
        parseDate(a.latestAnalysisDate ?? a.createDate)
    )
  }, [filteredRepositories, sortType])



  // 삭제 기능 그대로 유지
  async function handleDelete(repoId: number) {
    try {
      await analysisApi.deleteRepository(memberId, repoId)
      setRepositories((prev) => prev.filter((repo) => repo.id !== repoId))
    } catch (err) {
      console.error("삭제 실패:", err)
      alert("삭제 중 오류가 발생했습니다.")
    }
  }

  return { 
    repositories: sortedRepositories,
    loading,
    error,
    handleDelete,
    sortType,
    setSortType,
    keyword,
    setKeyword,
  }
}
