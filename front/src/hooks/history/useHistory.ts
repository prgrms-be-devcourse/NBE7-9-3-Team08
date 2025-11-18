"use client"

import { useEffect, useState, useMemo } from "react"
import { analysisApi } from "@/lib/api/analysis"
import type { RepositoryResponse as RepoBaseResponse } from "@/types/history"
import type { HistoryResponseDto } from "@/types/analysis"

export function useHistory(memberId: number) {
  const [repositories, setRepositories] = useState<RepoBaseResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 🔥 정렬 기준
  const [sortType, setSortType] = useState<"latest" | "score">("latest")

  // 🔥 검색: 입력값 / 실제 검색 값 분리
  const [keyword, setKeyword] = useState("")
  const [searchQuery, setSearchQuery] = useState("") // 버튼 클릭 시만 갱신

  /** 검색 버튼 누르면 실행됨 */
  function applySearch() {
    setSearchQuery(keyword)
  }

  // ============================================================
  //   전체 리스트 로딩
  // ============================================================
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


  // 🔥 날짜 파싱
  const parseDate = (date?: string | null) => {
    if (!date) return 0
    return Date.parse(date.split(".")[0] + "Z")
  }

  // ============================================================
  //   🔥 검색 적용 (입력값 X → searchQuery 기준)
  // ============================================================
  const filteredRepositories = useMemo(() => {
    if (!searchQuery.trim()) return repositories

    const q = searchQuery.toLowerCase()

    return repositories.filter((repo) =>
      repo.name.toLowerCase().includes(q) ||
      repo.description?.toLowerCase().includes(q)
    )
  }, [repositories, searchQuery])

  // ============================================================
  //   🔥 정렬 적용
  // ============================================================
  const sortedRepositories = useMemo(() => {
    if (sortType === "score") {
      return [...filteredRepositories].sort(
        (a, b) => (b.latestScore ?? 0) - (a.latestScore ?? 0)
      )
    }

    return [...filteredRepositories].sort(
      (a, b) =>
        parseDate(b.latestAnalysisDate ?? b.createDate) -
        parseDate(a.latestAnalysisDate ?? a.createDate)
    )
  }, [filteredRepositories, sortType])

  // ============================================================
  //   삭제 기능
  // ============================================================
  async function handleDelete(repoId: number) {
    try {
      await analysisApi.deleteRepository(memberId, repoId)
      setRepositories((prev) => prev.filter((repo) => repo.id !== repoId))
    } catch (err) {
      alert("삭제 중 오류가 발생했습니다.")
      console.error("삭제 실패:", err)
    }
  }

  return {
    repositories: sortedRepositories,
    loading,
    error,
    handleDelete,
    sortType,
    setSortType,

    /** 검색 */
    keyword,
    setKeyword,
    searchQuery,
    applySearch, // 🔥 버튼 눌렀을 때 검색 실행
  }
}
