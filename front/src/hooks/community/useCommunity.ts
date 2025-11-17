'use client'

import { useEffect, useState, useRef } from 'react'
import {
  fetchRepositories,
  searchRepositories
} from '@/lib/api/community'
import type { RepositoryItem, PageResponse } from '@/types/community'

export function useCommunity() {
  const [repositories, setRepositories] = useState<RepositoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  // 🔥 최신순 / 점수순
  const [sortType, setSortType] = useState<'latest' | 'score'>('latest')

  // 🔍 검색어 + 검색 타입
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchType, setSearchType] = useState<'repoName' | 'user'>('repoName')

  // 검색 모드 여부
  const [isSearching, setIsSearching] = useState(false)

  // 성능 측정
  const performanceStartRef = useRef(0)

  /** 🔵 기본 공개 레포지토리 조회 */
  const loadRepositories = async (pageNum = 0) => {
    setLoading(true)
    setIsSearching(false)

    performanceStartRef.current = performance.now()

    try {
      const res: PageResponse<RepositoryItem> = await fetchRepositories(
        pageNum,
        sortType
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

  /** 🔍 검색 실행 */
  const fetchSearchResults = async (pageNum?: number) => {
    const page = pageNum ?? 0

    // 검색어 없으면 전체 조회로 전환
    if (!searchKeyword.trim()) {
      loadRepositories(0)
      return
    }

    setLoading(true)
    setIsSearching(true)

    try {
      const res: PageResponse<RepositoryItem> = await searchRepositories({
        content: searchKeyword,
        searchSort: searchType,
        sort: sortType,    // ⭐ 반드시 포함
        page: page,
        size: 5
      })

      setRepositories(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setPage(page)
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  /** 🔄 페이지 or 정렬 변경 시 재조회 */
  useEffect(() => {
    if (isSearching) {
      fetchSearchResults(page)
    } else {
      loadRepositories(page)
    }
  }, [page, sortType])

  return {
    repositories,
    loading,
    error,

    // 정렬
    sortType,
    setSortType,

    // 페이징
    page,
    setPage,
    totalPages,

    // 검색
    searchKeyword,
    setSearchKeyword,
    searchType,
    setSearchType,
    fetchSearchResults,

    performanceStartRef,
  }
}
