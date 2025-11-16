'use client'

import { useEffect, useState, useRef } from 'react'
import { fetchRepositories } from '@/lib/api/community'
import type { RepositoryItem, PageResponse } from '@/types/community'

export function useCommunity() {
  const [repositories, setRepositories] = useState<RepositoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  // 🔥 정렬 기준 (latest | score)
  const [sortType, setSortType] = useState<'latest' | 'score'>('latest')

  // 성능 측정
  const performanceStartRef = useRef(0)

  const loadRepositories = async (pageNum = 0) => {
    setLoading(true)

    performanceStartRef.current = performance.now()
    console.log("%c📡 리포지토리 API 요청 시작", "color: #03A9F4")

    try {
      const res: PageResponse<RepositoryItem> = await fetchRepositories(pageNum, sortType)

      console.log(
        `%c📥 리포지토리 API 응답 시간: ${
          (performance.now() - performanceStartRef.current).toFixed(2)
        } ms`,
        "color: #FF9800; font-weight: bold;"
      )

      setRepositories(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setPage(pageNum)

    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  // page 또는 sortType 변경 시 API 다시 호출
  useEffect(() => {
    loadRepositories(page)
  }, [page, sortType])

  return {
    repositories,
    loading,
    error,
    sortType,
    setSortType,
    page,
    setPage,
    totalPages,
    loadRepositories,
    performanceStartRef,
  }
}
