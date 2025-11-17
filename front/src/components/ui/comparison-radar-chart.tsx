"use client"

import {
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Legend,
  ResponsiveContainer,
} from "recharts"

interface RepoChart {
  name: string
  color: string
  data: { category: string; score: number }[]
}

interface ComparisonRadarChartProps {
  repositories: RepoChart[]
}

const MAX_SCORES = {
  README: 30,
  TEST: 30,
  COMMIT: 25,
  "CI/CD": 15,
} as const
type CategoryKey = keyof typeof MAX_SCORES


export function ComparisonRadarChart({ repositories }: ComparisonRadarChartProps) {
  if (!repositories || repositories.length === 0)
    return <p className="text-center text-sm text-muted-foreground">비교할 리포지토리를 선택해주세요.</p>

  // 데이터 통합 (Recharts는 같은 key를 가진 객체 배열이 필요함)
  const chartData = Object.keys(MAX_SCORES).map((category) => {
    const maxScore = MAX_SCORES[category as CategoryKey]
    const merged: Record<string, string | number> = { category }
  
    repositories.forEach((repo) => {
      const scoreEntry = repo.data.find((item) => item.category === category)
      const rawScore = scoreEntry?.score ?? 0
      merged[repo.name] = Math.min((rawScore / maxScore) * 100, 100)
    })
  
    return merged
  })  

  return (
    <div className="w-full h-[400px]">
      <ResponsiveContainer width="100%" height="100%">
        <RadarChart outerRadius="75%" data={chartData}>
          <PolarGrid />
          <PolarAngleAxis dataKey="category" />
          <PolarRadiusAxis domain={[0, 100]} />
          {repositories.map((repo) => (
            <Radar
              key={repo.name}
              name={repo.name}
              dataKey={repo.name}
              stroke={repo.color}
              fill={repo.color}
              fillOpacity={0.4}
            />
          ))}
          <Legend />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  )
}
