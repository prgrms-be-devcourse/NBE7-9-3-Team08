"use client"

import { useState } from "react"
import { Button } from "@/components/ui/Button"
import { Textarea } from "@/components/ui/textarea"
import { postComment } from "@/lib/api/community"
import { useAuth } from "@/hooks/auth/useAuth"

interface CommentFormProps {
  analysisResultId: number
  onCommentAdded?: () => void
}

export default function CommentForm({ analysisResultId, onCommentAdded }: CommentFormProps) {
  const { user } = useAuth()
  const [content, setContent] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!user) {
      setError("로그인이 필요합니다.")
      return
    }

    if (!content.trim()) return

    try {
      setLoading(true)
      setError(null)

      await postComment(analysisResultId, user.id, content)

      setContent("")
      onCommentAdded?.()
    } catch (err) {
      console.error("❌ 댓글 작성 중 오류:", err)
      setError("댓글 작성 중 오류가 발생했습니다.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 mt-6">
      <Textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="댓글을 입력하세요..."
        className="min-h-[100px]"
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            // 🔥 Enter 입력 시 submit 방지 → 줄바꿈 정상 작동
            e.stopPropagation()
          }
        }}
      />

      {error && <p className="text-sm text-red-500">{error}</p>}

      <div className="flex justify-end">
        <Button type="submit" disabled={loading || !content.trim()}>
          {loading ? "작성 중..." : "댓글 작성"}
        </Button>
      </div>
    </form>
  )
}
