'use client'

import { useEffect, useState, useMemo, useRef } from 'react'
import { fetchRepositories } from '@/lib/api/community'
import type { RepositoryItem, PageResponse } from '@/types/community'

export function useCommunity() {
  const [repositories, setRepositories] = useState<RepositoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const [sortType, setSortType] = useState<'latest' | 'score'>('latest')

  const performanceStartRef = useRef(0)

  const loadRepositories = async (pageNum = 0) => {
    setLoading(true)

    performanceStartRef.current = performance.now()
    console.log("%c📡 리포지토리 API 요청 시작", "color: #03A9F4")

    try {
      const res: PageResponse<RepositoryItem> = await fetchRepositories(pageNum)

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

  useEffect(() => {
    loadRepositories(page)
  }, [page])

  const sortedRepositories = useMemo(() => {
    if (sortType === 'score') {
      return repositories.slice().sort((a, b) => (b.totalScore ?? 0) - (a.totalScore ?? 0))
    }

    const parseDate = (d?: string) => {
      if (!d) return 0
      const trimmed = d.includes('.') ? d.split('.')[0] : d
      return Date.parse(trimmed + 'Z')
    }

    return repositories.slice().sort((a, b) => parseDate(b.createDate) - parseDate(a.createDate))
  }, [repositories, sortType])

  return {
    repositories: sortedRepositories,
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
