'use client'

import { useEffect, useState, useRef, useMemo } from 'react'
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
    const p = pageNum ?? 0

    // 검색어 없으면 전체 조회
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
        sort: sortType,
        page: p,
        size: 5
      })

      setRepositories(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setPage(p)
    } catch (err: any) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  /** 🔥 프론트에서 description 기반 2차 필터링 */
  const filteredRepositories = useMemo(() => {
    if (!isSearching) return repositories // 검색 모드 아닐 땐 그대로 반환

    if (!searchKeyword.trim()) return repositories

    const lower = searchKeyword.toLowerCase()

    return repositories.filter(repo =>
      repo.repositoryName?.toLowerCase().includes(lower) ||
      repo.userName?.toLowerCase().includes(lower) ||
      repo.description?.toLowerCase().includes(lower)   // 🔥 핵심
    )
  }, [repositories, searchKeyword, isSearching])

  /** 🔄 페이지 or 정렬 변경 시 재조회 */
  useEffect(() => {
    if (isSearching) {
      fetchSearchResults(page)
    } else {
      loadRepositories(page)
    }
  }, [page, sortType])

  return {
    repositories: filteredRepositories, // 🔥 필터링된 결과 반환
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
