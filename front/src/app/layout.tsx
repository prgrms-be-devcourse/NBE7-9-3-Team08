import type React from "react"
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "@/styles/globals.css"
import { Suspense } from "react"
import Header from "@/components/Header"
import Footer from "@/components/Footer"
import { ToastProvider } from "@/components/ui/Toast"
import Link from "next/link"


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
        {/* ✅ 구글이 준 코드 그대로 */}
        <script
          async
          src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3350957700000483"
          crossOrigin="anonymous"
        ></script>
      </head>

      <body className={`font-sans ${inter.variable} antialiased flex flex-col min-h-screen`}>
        <ToastProvider>
          <Header />

          <main className="flex-1">
            <Suspense fallback={<div>로딩 중...</div>}>
              {children}
            </Suspense>
          </main>

          <Footer />
        </ToastProvider>
      </body>
    </html>
  )
}
