'use client'

import { useEffect, useState } from 'react'
import { useCommunity } from '@/hooks/community/useCommunity'
import RepositoryCard from './RepoCard'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/input'
import { Loader2 } from 'lucide-react'
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select"
import { AnimatePresence, motion } from "framer-motion"

export default function RepositoryList() {
  const {
    repositories,
    loading,
    error,
    sortType,
    setSortType,
    page,
    setPage,
    totalPages,
    searchKeyword,
    setSearchKeyword,
    searchType,
    setSearchType,
    fetchSearchResults,
  } = useCommunity()

  // 🔥 첫 렌더 여부 판단
  const [firstRender, setFirstRender] = useState(true)
  useEffect(() => {
    setFirstRender(false)
  }, [])

  // 🎨 애니메이션 프로필 1 — 첫 렌더용(Fade + Scale)
  const initialAnim = { opacity: 0, scale: 0.95 }
  const enterAnim = { opacity: 1, scale: 1 }
  const exitAnim = { opacity: 0, scale: 0.95 }

  // 🎨 애니메이션 프로필 2 — 정렬/검색/페이지 변화용(Fade + Slide)
  const sortInitialAnim = { opacity: 0, y: -6 }
  const sortEnterAnim = { opacity: 1, y: 0 }
  const sortExitAnim = { opacity: 0, y: -6 }

  // 에러 처리
  if (error)
    return <p className="text-red-500 text-center py-8">에러 발생: {error}</p>

  return (
    <section className="flex flex-col gap-6 mt-6">

      {/* 헤더 */}
      <div>
        <h1 className="text-3xl font-bold">커뮤니티</h1>
        <p className="text-muted-foreground text-sm">
          다른 사용자의 분석 결과를 둘러보세요.
        </p>
      </div>

      {/* 검색 영역 */}
      <div className="flex gap-2 items-center">
        <Select
          value={searchType}
          onValueChange={(value) => setSearchType(value as "repoName" | "user")}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="검색 기준 선택" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="repoName">레포지토리 이름</SelectItem>
            <SelectItem value="user">작성자 이름</SelectItem>
          </SelectContent>
        </Select>

        <Input
          placeholder="레포지토리 이름 또는 작성자 이름을 검색하세요"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          className="flex-1"
        />

        <Button
          variant="default"
          onClick={() => {
            setPage(0)
            fetchSearchResults(0)
          }}
        >
          검색
        </Button>
      </div>

      {/* 정렬 버튼 */}
      <div className="flex justify-end mt-3 gap-2">
        <Button
          variant={sortType === "latest" ? "default" : "outline"}
          onClick={() => setSortType("latest")}
        >
          최신순
        </Button>
        <Button
          variant={sortType === "score" ? "default" : "outline"}
          onClick={() => setSortType("score")}
        >
          점수순
        </Button>
      </div>

      {/* ✨ 리스트 애니메이션 */}
      <AnimatePresence mode="wait">
        <motion.div
          key={`${sortType}-${page}-${searchKeyword}-${loading}`}
          initial={firstRender ? initialAnim : sortInitialAnim}
          animate={firstRender ? enterAnim : sortEnterAnim}
          exit={firstRender ? exitAnim : sortExitAnim}
          transition={{ duration: 0.15 }}
        >
          {/* 로딩 상태 */}
          {loading ? (
            <div className="flex justify-center items-center py-20 text-muted-foreground">
              <Loader2 className="w-5 h-5 animate-spin mr-2" />
              데이터를 불러오는 중입니다...
            </div>
          ) : repositories.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">
              아직 공개된 분석이 없습니다.
            </p>
          ) : (
            <div className="flex flex-col gap-6">
              {repositories.map((item) => (
                <RepositoryCard key={item.repositoryId ?? item.id} item={item} />
              ))}
            </div>
          )}
        </motion.div>
      </AnimatePresence>

      {/* 페이징 */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-4 mt-8">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            이전
          </Button>

          <span className="text-sm text-muted-foreground">
            {page + 1} / {totalPages}
          </span>

          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage(page + 1)}
          >
            다음
          </Button>
        </div>
      )}

    </section>
  )
}
