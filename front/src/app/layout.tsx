import type React from "react"
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "@/styles/globals.css"
import { Suspense } from "react"
import Header from "@/components/Header"
import Footer from "@/components/Footer"
import { ToastProvider } from "@/components/ui/Toast"
import Link from "next/link"
import Script from "next/script"   // ⬅⬅ 추가

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
})

export const metadata: Metadata = {
  title: "PortfolioIQ - AI 기반 GitHub 저장소 분석",
  description:
    "AI로 GitHub 저장소를 분석하고 코드 품질, 문서화, 프로젝트 구조를 개선할 수 있는 실행 가능한 피드백을 받으세요.",
}

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <head>
        {/* ✅ 애드센스 소유권 확인용 스크립트 */}
        <Script
          id="adsense-init"
          async
          src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3350957700000483"
          crossOrigin="anonymous"
          strategy="afterInteractive"
        />
      </head>

      {/* ⭐ Footer 고정을 위한 핵심 부분 추가됨 ⭐ */}
      <body className={`font-sans ${inter.variable} antialiased flex flex-col min-h-screen`}>
        <ToastProvider>
          {/* 상단 Header */}
          <Header />

          {/* ⭐ 남은 공간을 모두 채우게 만들어 footer를 아래로 밀어냄 */}
          <main className="flex-1">
            <Suspense fallback={<div>로딩 중...</div>}>
              {children}
            </Suspense>
          </main>

          {/* 하단 Footer - 항상 아래 고정 */}
          <Footer />
        </ToastProvider>
      </body>
    </html>
  )
}
