"use client"

import { useEffect, useRef, useState, useMemo } from "react"
import { useRouter } from "next/navigation"
import { ERROR_CODES, ERROR_MESSAGES, ErrorCode } from "@/types/api"
import { analysisApi } from "@/lib/api/analysis"
import { useRequireAuth } from "@/hooks/auth/useRequireAuth" // [변경] 인증 훅 추가

type AnalysisErrorKind = "repo" | "auth" | "rate" | "duplicate" | "server" | "network"

const defaultAnalysisError = {
  type: "server" as AnalysisErrorKind,
  message: "분석 처리 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.",
}

const errorCodeValues = new Set<string>(Object.values(ERROR_CODES))

const resolveErrorCode = (raw?: string): ErrorCode | undefined => {
  if (!raw) return undefined

  if (errorCodeValues.has(raw)) {
    return raw as ErrorCode
  }

  const matchedEntry = Object.entries(ERROR_CODES).find(([, value]) => value === raw)
  if (matchedEntry) {
    return matchedEntry[1]
  }

  return undefined
}

const mapErrorCodeToAlert = (
  code?: string,
  fallback?: string
): { type: AnalysisErrorKind; message: string } => {
  const resolvedCode = resolveErrorCode(code)

  if (!resolvedCode) {
    return {
      type: "server",
      message: fallback || defaultAnalysisError.message,
    }
  }

  const message = ERROR_MESSAGES[resolvedCode] || fallback || defaultAnalysisError.message

  let type: AnalysisErrorKind = "server"

  const repoErrors = new Set<ErrorCode>([
    ERROR_CODES.GITHUB_REPO_NOT_FOUND,
    ERROR_CODES.GITHUB_API_FAILED,
    ERROR_CODES.GITHUB_REPO_TOO_LARGE,
  ])

  const authErrors = new Set<ErrorCode>([
    ERROR_CODES.GITHUB_INVALID_TOKEN,
    ERROR_CODES.UNAUTHORIZED,
    ERROR_CODES.FORBIDDEN,
  ])

  if (repoErrors.has(resolvedCode)) {
    type = "repo"
  } else if (authErrors.has(resolvedCode)) {
    type = "auth"
  } else if (resolvedCode === ERROR_CODES.GITHUB_RATE_LIMIT_EXCEEDED) {
    type = "rate"
  } else if (resolvedCode === ERROR_CODES.ANALYSIS_IN_PROGRESS) {
    type = "duplicate"
  } else if (resolvedCode === ERROR_CODES.NETWORK_ERROR) {
    type = "network"
  }

  return { type, message }
}

const stashAnalysisError = (payload: { type: AnalysisErrorKind; message: string }) => {
  try {
    sessionStorage.setItem("analysisError", JSON.stringify(payload))
  } catch {
    /* ignore */
  }
}

const normalize = (text: string) => text.replace(/\s+/g, "")

export function useAnalysisProgress(repoUrl?: string | null) {
  const router = useRouter()
  const { user, isAuthed, isInitializing } = useRequireAuth() // [변경] 인증 상태 참조
  const [progress, setProgress] = useState(0)
  const [currentStep, setCurrentStep] = useState(0)
  const [statusMessage, setStatusMessage] = useState("분석 준비 중...")
  const [isCompleted, setIsCompleted] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const repositoryIdRef = useRef<number | null>(null)

  const hasRequestedAnalysis = useRef(false)

  const steps = useMemo(
    () => [
      { label: "분석 시작", description: "요청을 수락했어요." },
      { label: "GitHub 연결 중", description: "리포지토리 데이터를 불러와요..." },
      { label: "커밋 히스토리 분석", description: "커밋 활동과 패턴을 확인해요..." },
      { label: "문서화 품질 분석", description: "README 등 문서를 검사해요..." },
      { label: "보안 구성 분석", description: "민감 정보, 빌드 파일을 점검해요..." },
      { label: "테스트 구성 분석", description: "테스트 커버리지와 폴더를 확인해요..." },
      { label: "CI/CD 설정 분석", description: "자동화된 빌드/배포를 점검해요..." },
      { label: "커뮤니티 활동 분석", description: "이슈/PR 협업 지표를 분석해요..." },
      { label: "AI 평가", description: "수집된 정보를 종합해 평가해요..." },
      { label: "최종 리포트 작성", description: "결과를 정리하고 있어요." },
    ],
    []
  )

  const aliasToLabel: Record<string, string> = useMemo(
    () => ({
      GitHub데이터수집완료: "AI 평가",
      AI평가완료: "AI 평가",
      최종리포트생성: "최종 리포트 작성",
      최종리포트작성: "최종 리포트 작성",
    }),
    []
  )

  useEffect(() => {
    if (!repoUrl || hasRequestedAnalysis.current) return
    if (isInitializing) return
    if (!isAuthed || !user?.id) {
      router.push("/login")
      return
    }

    hasRequestedAnalysis.current = true

    const requestAnalysis = async () => {
      try {
        const data = await analysisApi.requestAnalysis({ githubUrl: repoUrl })
        repositoryIdRef.current = data.repositoryId
      } catch (err: any) {
        if (err.code === "ECONNRESET" || err.message?.includes("socket hang up")) {
          const duplicatePayload = {
            type: "duplicate" as AnalysisErrorKind,
            message: "이미 분석이 진행 중이에요. 잠시 후 다시 확인해 주세요.",
          }
          setError(duplicatePayload.message)
          setStatusMessage("중복 요청을 감지했어요.")
          stashAnalysisError(duplicatePayload)
          setTimeout(() => router.push("/analysis"), 3000)
          return
        }

        const alertPayload = mapErrorCodeToAlert(err.code, err.message)
        setError(alertPayload.message)
        setStatusMessage("요청 처리에 문제가 발생했어요.")
        stashAnalysisError(alertPayload)
        setTimeout(() => router.push("/analysis"), 3000)
      }
    }

    requestAnalysis()
  }, [isAuthed, isInitializing, repoUrl, router, user?.id])

  useEffect(() => {
    if (isInitializing) return
    if (!isAuthed || !user?.id) return

    const handleStatus = (e: CustomEvent<string>) => {
      const message = e.detail
      setStatusMessage(message)

      const normalizedMessage = normalize(message)
      let stepIndex = steps.findIndex((s) => normalizedMessage.includes(normalize(s.label)))

      if (stepIndex === -1) {
        const aliasKey = Object.keys(aliasToLabel).find((key) =>
          normalizedMessage.includes(key)
        )
        if (aliasKey) {
          const targetLabel = aliasToLabel[aliasKey]
          stepIndex = steps.findIndex((s) => normalize(s.label) === normalize(targetLabel))
        }
      }

      if (stepIndex !== -1) {
        setCurrentStep((prev) => {
          const nextStep = Math.max(prev, stepIndex)
          setProgress(Math.min(((nextStep + 1) / steps.length) * 100, 99))
          return nextStep
        })
      }
    }

    const handleComplete = (e: CustomEvent<string>) => {
      setStatusMessage("최종 리포트 작성")
      setCurrentStep(steps.length - 1)
      setProgress(100)
      setIsCompleted(true)

      setTimeout(() => {
        const repoId = repositoryIdRef.current
        if (repoId) router.push(`/analysis/${repoId}`)
      }, 1500)
    }

    const handleError = () => {
      setError("서버에 문제가 발생했어요.")
      setTimeout(() => router.push("/analysis"), 3000)
    }

    window.addEventListener("SSE_STATUS", handleStatus as EventListener)
    window.addEventListener("SSE_COMPLETE", handleComplete as EventListener)
    window.addEventListener("SSE_ERROR", handleError as EventListener)

    const disconnect = analysisApi.connectStream(user.id)

    return () => {
      disconnect()
      window.removeEventListener("SSE_STATUS", handleStatus as EventListener)
      window.removeEventListener("SSE_COMPLETE", handleComplete as EventListener)
      window.removeEventListener("SSE_ERROR", handleError as EventListener)
    }
  }, [aliasToLabel, isAuthed, isInitializing, router, steps, user?.id])

  return { progress, currentStep, steps, statusMessage, isCompleted, error }
}
