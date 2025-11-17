import { cn } from "@/lib/utils"

interface SkeletonProps {
  className?: string
}

/**
 * 기본 회색 애니메이션 블록을 렌더링해 로딩 상태를 표현합니다.
 */
export function Skeleton({ className }: SkeletonProps) {
  return <div className={cn("animate-pulse rounded-md bg-muted", className)} />
}
