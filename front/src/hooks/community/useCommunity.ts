'use client'

import { useEffect, useState, useMemo } from 'react'
import { fetchRepositories } from '@/lib/api/community'
import type { RepositoryItem, PageResponse } from '@/types/community'

export function useCommunity() {
  const [repositories, setRepositories] = useState<RepositoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 페이징 관련
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(5)
  const [totalPages, setTotalPages] = useState(0)

  // 정렬 기준
  const [sortType, setSortType] = useState<'latest' | 'score'>('latest')

  // API 호출
  const loadRepositories = async (pageNum = 0) => {
    setLoading(true)
    try {
      const res: PageResponse<RepositoryItem> = await fetchRepositories(pageNum, size)
      setRepositories(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setPage(res.pageable?.pageNumber ?? 0)
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  // 페이지 변경 시 API 호출
  useEffect(() => {
    loadRepositories(page)
  }, [page, size])

  // 정렬 우선순위: 점수 > 최신
  const sortedRepositories = useMemo(() => {
    if (sortType === 'score') {
      return repositories.slice().sort((a, b) => (b.totalScore ?? 0) - (a.totalScore ?? 0))
    }

    // createDate 파싱 정규화
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
    size,
    setSize,
    loadRepositories,
  }
}
