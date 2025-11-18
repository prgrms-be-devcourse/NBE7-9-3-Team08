"use client"

import { useSearchParams } from "next/navigation"
import { Progress } from "@/components/ui/progress"
import { AlertCircle, HelpCircle } from "lucide-react"
import LoadingHeader from "@/components/analysis/LoadingHeader"
import LoadingStepList from "@/components/analysis/LoadingStepList"
import LoadingInfoBox from "@/components/analysis/LoadingInfoBox"
import { useAnalysisProgress } from "@/hooks/analysis/useLoadingProgress"

export default function LoadingContent() {
  const searchParams = useSearchParams()
  const repoUrl = searchParams.get("repoUrl")

  const { progress, currentStep, steps, statusMessage, error } = useAnalysisProgress(repoUrl)

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="mx-auto max-w-2xl">
          <LoadingHeader repoUrl={repoUrl} />
          {error && (
            <div className="mb-6 rounded-lg border border-destructive/50 bg-destructive/10 p-4">
              <div className="flex items-start gap-3">
                <AlertCircle className="h-5 w-5 text-destructive flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <h4 className="font-semibold text-destructive mb-1">
                    분석 요청 실패
                  </h4>
                  <p className="text-sm text-destructive/90">
                    {error}
                  </p>
                  <p className="text-xs text-muted-foreground mt-2">
                    잠시 후 자동으로 분석 페이지로 돌아갑니다...
                  </p>
                </div>
              </div>
            </div>
          )}

          {!error && (
          <> 
            <div className="mb-12 space-y-3">
              <Progress value={progress} className="h-2" />
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">
                  {Math.round(progress)}% 완료
                </span>
                <span className="text-muted-foreground">
                  {currentStep + 1} / {steps.length} 단계
                </span>
              </div>
              <div className="flex items-center gap-2 rounded-lg bg-muted/50 px-3 py-2 text-xs sm:text-sm">
                  <HelpCircle className="h-4 w-4 text-primary" />
                  <div className="flex-1 text-muted-foreground">
                    {steps.length - (currentStep + 1)} 단계 남았습니다. 평균 약 1~2분 소요됩니다.
                  </div>
                </div>
            </div>

            {/* 단계 리스트 및 추가 정보 */}
            <LoadingStepList steps={steps} currentStep={currentStep} />
            <LoadingInfoBox />
          </>
          
          )}
        </div>
      </div>
    </div>
  )
}
