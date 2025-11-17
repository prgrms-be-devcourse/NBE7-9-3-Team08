"use client"

import { useEffect, useState } from "react"
import HistoryContent from "@/components/history/HistoryContent"
import type { StoredUser } from "@/types/user"
import { Button } from "@/components/ui/Button"
import { Card } from "@/components/ui/card"
import { useRouter } from "next/navigation"

export default function HistoryPage() {
  const router = useRouter()
  const [user, setUser] = useState<StoredUser | null>(null)

  useEffect(() => {
    const stored = localStorage.getItem("user")
    if (stored) {
      try {
        setUser(JSON.parse(stored))
      } catch (err) {
        console.error("유저 정보 파싱 실패:", err)
      }
    }
  }, [])

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="max-w-md w-full p-8 text-center space-y-4">
          <h1 className="text-2xl font-bold">분석 히스토리를 불러올 수 없어요</h1>
          <p className="text-muted-foreground">
            먼저 로그인 후 분석을 진행하면 기록을 쌓을 수 있습니다.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button className="flex-1" onClick={() => router.push("/login")}>
              로그인하기
            </Button>
            <Button variant="outline" className="flex-1" onClick={() => router.push("/analysis")}>
              첫 분석 시작하기
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  return <HistoryContent memberId={user.id} name={user.name} />
}