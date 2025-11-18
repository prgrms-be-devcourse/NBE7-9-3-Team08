"use client"

import { useState, useCallback, useMemo } from "react"
import { useHistory } from "@/hooks/history/useHistory"
import { HistoryStats } from "@/components/history/HistoryStatsProps"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/Button"
import { Skeleton } from "@/components/ui/skeleton"
import { ScoreBadge } from "@/components/history/ScoreBadge"
import { Input } from "@/components/ui/input"
import { formatRelativeTimeKST } from "@/lib/utils/formatDate"
import { Github, ExternalLink, Trash2, Calendar, GitCompare, X } from "lucide-react"
import { RepositoryComparisonResponse } from "@/types/analysis"
import { useRouter } from "next/navigation"
import { HistoryCompare } from "@/components/history/HistoryCompare"
import { analysisApi } from "@/lib/api/analysis"
import { AnimatePresence, motion } from "framer-motion"

interface HistoryContentProps {
  memberId: number
  name: string
}

export default function HistoryContent({ memberId, name }: HistoryContentProps) {
  const {
    repositories,
    loading,
    error,
    handleDelete,
    sortType,
    setSortType,
    keyword,
    setKeyword,
    searchQuery,
    applySearch,
  } = useHistory(memberId)

  const router = useRouter()

  const [compareMode, setCompareMode] = useState(false)
  const [selectedRepoIds, setSelectedRepoIds] = useState<number[]>([])
  const [comparisonRepos, setComparisonRepos] = useState<RepositoryComparisonResponse[]>([])
  const [comparisonLoading, setComparisonLoading] = useState(false)
  const [comparisonError, setComparisonError] = useState<string | null>(null)

  const fetchComparisonRepos = useCallback(async () => {
    setComparisonLoading(true)
    setComparisonError(null)

    try {
      const data = await analysisApi.getRepositoriesForComparison()
      setComparisonRepos(data)
    } catch (err) {
      setComparisonError(err instanceof Error ? err.message : "비교 데이터를 불러오지 못했습니다.")
    } finally {
      setComparisonLoading(false)
    }
  }, [])

  const handleToggleCompareMode = () => {
    const next = !compareMode
    setCompareMode(next)
    setSelectedRepoIds([])

    if (next && comparisonRepos.length === 0) {
      fetchComparisonRepos()
    }
  }

  const handleSelectRepo = (repoId: number) => {
    setSelectedRepoIds((prev) => {
      if (prev.includes(repoId)) return prev.filter((id) => id !== repoId)
      if (prev.length >= 5) return prev
      return [...prev, repoId]
    })
  }

  const isEmpty = useMemo(() => repositories.length === 0, [repositories])

  /* --------------------------------------------------- */
  /* 로딩 UI                                             */
  /* --------------------------------------------------- */
  if (loading) {
    return (
      <div className="max-w-3xl mx-auto p-6 space-y-4">
        {Array.from({ length: 3 }).map((_, idx) => (
          <Card key={idx} className="p-5 space-y-3">
            <Skeleton className="h-5 w-60" />
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-5 w-48" />
          </Card>
        ))}
      </div>
    )
  }

  /* --------------------------------------------------- */
  /* 에러 UI                                             */
  /* --------------------------------------------------- */
  if (error) {
    return (
      <div className="max-w-3xl mx-auto p-6">
        <Card className="p-8 text-center space-y-4">
          <p className="text-lg font-semibold">히스토리를 불러오는 중 문제가 발생했습니다.</p>
          <p className="text-sm text-muted-foreground">{error}</p>
          <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
            <Button onClick={() => window.location.reload()} className="flex-1 sm:flex-none">
              다시 시도
            </Button>
            <Button
              variant="outline"
              onClick={() => router.push("/analysis")}
              className="flex-1 sm:flex-none"
            >
              첫 분석 시작하기
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  /* --------------------------------------------------- */
  /* 실제 화면                                           */
  /* --------------------------------------------------- */
  return (
    <div className="max-w-3xl mx-auto p-6 space-y-6">
      <header className="flex flex-col gap-3">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold">분석 히스토리</h1>
            <p className="text-sm text-muted-foreground">
              {compareMode
                ? "비교할 리포지토리를 선택해 주세요 (최대 5개)"
                : `${name}님의 최근 분석 기록을 검색하거나 정렬할 수 있습니다.`}
            </p>
          </div>

          <Button
            variant={compareMode ? "default" : "outline"}
            className="gap-2"
            onClick={handleToggleCompareMode}
            disabled={isEmpty && !compareMode}
          >
            {compareMode ? (
              <>
                <X className="h-4 w-4" />
                비교 취소
              </>
            ) : (
              <>
                <GitCompare className="h-4 w-4" />
                리포지토리 비교
              </>
            )}
          </Button>
        </div>

        {/* 🔍 검색 입력 + 검색 버튼 */}
        {!compareMode && (
          <div className="flex items-center gap-2 mt-2">
            <Input
              placeholder="리포지토리 검색..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") applySearch()
              }}
              className="max-w-xs"
            />

            <Button onClick={applySearch} variant="default">
              검색
            </Button>
          </div>
        )}

        {/* 검색 결과 안내 문구 */}
        {!compareMode && searchQuery && (
          <span className="text-xs text-muted-foreground mt-1">
            '{searchQuery}' 검색 결과
          </span>
        )}

        {/* 정렬 UI */}
        {!compareMode && (
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mt-2">
            <span className="text-sm text-muted-foreground">
              {isEmpty ? "분석 기록이 없습니다." : "정렬 기준을 선택하세요."}
            </span>

            <div className="flex gap-2">
              <Button
                variant={sortType === "latest" ? "default" : "outline"}
                size="sm"
                disabled={isEmpty}
                onClick={() => setSortType("latest")}
              >
                최신순
              </Button>
              <Button
                variant={sortType === "score" ? "default" : "outline"}
                size="sm"
                disabled={isEmpty}
                onClick={() => setSortType("score")}
              >
                점수순
              </Button>
            </div>
          </div>
        )}
      </header>

      {/* --------------------------------------------------- */}
      {/* 리스트 or 비교 모드                                 */}
      {/* --------------------------------------------------- */}
      <AnimatePresence mode="wait">
        {compareMode ? (
          <motion.div
            key="compare"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.2 }}
          >
            <HistoryCompare
              repositories={comparisonRepos}
              selectedRepoIds={selectedRepoIds}
              onSelectRepo={handleSelectRepo}
              loading={comparisonLoading}
              error={comparisonError}
              onExit={handleToggleCompareMode}
            />
          </motion.div>
        ) : (
          <motion.div
            key="list"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.2 }}
          >
            {!isEmpty && <HistoryStats repositories={repositories} />}

            {isEmpty ? (
              <Card className="p-10 text-center space-y-4">
                <h2 className="text-2xl font-semibold">아직 저장된 분석이 없습니다</h2>
                <p className="text-muted-foreground">
                  첫 분석을 실행하면 개선 사항을 비교하고 추적할 수 있어요.
                </p>
                <Button className="w-full sm:w-auto" onClick={() => router.push("/analysis")}>
                  첫 분석 시작하기
                </Button>
              </Card>
            ) : (
              <div className="space-y-4 mt-6">
                {repositories.map((repo) => (
                  <Card
                    key={repo.id}
                    className="p-6 transition-all hover:border-primary/50 cursor-pointer"
                    onClick={() => router.push(`/analysis/${repo.id}`)}
                  >
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                      <div className="flex-1">
                        <div className="mb-2 flex items-center gap-2">
                          <Github className="h-4 w-4 text-muted-foreground" />
                          <a
                            href={repo.htmlUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-semibold text-primary hover:underline flex items-center gap-1"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {repo.name}
                            <ExternalLink className="h-3 w-3" />
                          </a>

                          {repo.publicRepository ? (
                            <Badge variant="default" className="gap-1 bg-green-600 text-white">
                              <span className="h-2 w-2 rounded-full bg-white" />
                              Public
                            </Badge>
                          ) : (
                            <Badge variant="secondary" className="gap-1">
                              <span className="h-2 w-2 rounded-full bg-muted-foreground" />
                              Private
                            </Badge>
                          )}
                        </div>

                        <p className="text-sm text-muted-foreground mb-3 line-clamp-2">
                          {repo.description || "설명이 없습니다."}
                        </p>

                        <div className="mb-3 flex flex-wrap gap-2">
                          {repo.languages.map((lang) => (
                            <Badge key={lang} variant="secondary" className="text-xs">
                              {lang}
                            </Badge>
                          ))}
                        </div>

                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                          <Calendar className="h-4 w-4" />
                          <span>
                            {formatRelativeTimeKST(repo.latestAnalysisDate ?? repo.createDate)}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-6">
                        {repo.latestScore != null ? (
                          <div className="text-center">
                            <div className="mb-1 text-sm text-muted-foreground">점수</div>
                            <ScoreBadge score={repo.latestScore} size="sm" />
                          </div>
                        ) : (
                          <div className="text-sm text-muted-foreground">점수 없음</div>
                        )}

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation()
                            if (confirm("정말 이 리포지토리를 삭제하시겠습니까?")) {
                              handleDelete(repo.id)
                            }
                          }}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
