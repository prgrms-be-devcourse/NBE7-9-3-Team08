"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Sparkles, Menu, X } from "lucide-react"
import { Button } from "@/components/ui/Button"
import { useAuth } from "@/hooks/auth/useAuth"
import { useToast } from "@/components/ui/Toast"

const navItems = [
  { label: "커뮤니티", href: "/community", requireAuth: false },
  { label: "분석 이력", href: "/history", requireAuth: true },
  { label: "분석하기", href: "/analysis", requireAuth: true },
  { label: "마이페이지", href: "/profile", requireAuth: true },
]

export default function Header() {
  const router = useRouter()
  const { isAuthed, logout, user, isLoadingUser } = useAuth()
  const { push } = useToast()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  
  // user가 있으면 로그인된 것으로 간주
  const isLoggedIn = isAuthed || !!user
  
  const guardNav = (path: string, featureName: string) => () => {
    if (!isLoggedIn) {
      push(`${featureName}은 로그인이 필요합니다!`)
      return
    }
    router.push(path)
    setIsMenuOpen(false) 
  }

  const handleCommunityClick = () => {
    router.push("/community")
    setIsMenuOpen(false)
  }

  const mobileMenu = (
    <div className="flex flex-col gap-3 pt-4 md:hidden">
      <button onClick={handleCommunityClick} className="text-base text-muted-foreground hover:text-foreground transition-colors">
        커뮤니티
      </button>
      {["/history", "/analysis", "/profile"].map((path, idx) => (
        <button
          key={path}
          onClick={guardNav(path, ["히스토리", "분석하기", "마이페이지"][idx])}
          className={`text-base ${
            !isLoggedIn && path !== "/community" ? "text-muted-foreground/70 cursor-not-allowed" : "text-muted-foreground hover:text-foreground"
          } transition-colors`}
          disabled={!isLoggedIn && path !== "/community"}
        >
          {["분석내역", "분석하기", "마이페이지"][idx]}
        </button>
      ))}
      {isLoggedIn ? (
        <>
          <Button variant="ghost" size="sm" onClick={logout}>
            로그아웃
          </Button>
        </>
      ) : (
        <div className="flex flex-col gap-2">
          <Button variant="ghost" size="sm" onClick={() => router.push("/login")}>
            로그인
          </Button>
          <Button size="sm" className="bg-primary text-primary-foreground hover:bg-primary/90" onClick={() => router.push("/signup")}>
            회원가입
          </Button>
        </div>
      )}
    </div>
  )

  return (
    <nav className="border-b border-gray-200 bg-background/80 backdrop-blur-xl">
      <div className="w-full max-w-none px-4 sm:px-8">
        <div className="flex h-16 items-center justify-between">
          <Link href="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <Sparkles className="h-5 w-5 text-primary-foreground" />
            </div>
            <span className="text-xl font-bold">PortfolioIQ</span>
          </Link>

          <div className="hidden items-center gap-6 md:flex">
            {isLoggedIn ? (
              <>
                <button onClick={handleCommunityClick} className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                  커뮤니티
                </button>
                <button onClick={guardNav("/history", "히스토리")} className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                  분석내역
                </button>
                <button onClick={() => router.push("/analysis")} className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                  분석하기
                </button>
                <div className="w-px h-5 bg-border mx-2" />
                <button onClick={guardNav("/profile", "마이페이지")} className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                  {isLoadingUser ? "로딩중..." : user?.name ? `${user.name}(마이페이지)` : "마이페이지"}
                </button>
                <Button variant="ghost" size="sm" onClick={logout} className="text-muted-foreground hover:text-foreground">
                  로그아웃
                </Button>
              </>
            ) : (
              <>
                <button onClick={handleCommunityClick} className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                  커뮤니티
                </button>
                <div className="flex items-center gap-3">
                  <Button variant="ghost" size="sm" onClick={() => router.push("/login")}>
                    로그인
                  </Button>
                  <Button size="sm" className="bg-primary text-primary-foreground hover:bg-primary/90" onClick={() => router.push("/signup")}>
                    회원가입
                  </Button>
                </div>
              </>
            )}
          </div>

          {/* [ADD] 모바일 햄버거 버튼 */}
          <button
            className="inline-flex items-center justify-center rounded-md p-2 text-foreground hover:bg-muted md:hidden"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            aria-label="메뉴 열기"
          >
            {isMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
        {isMenuOpen && mobileMenu} {/* [ADD] 모바일 메뉴 표시 */}
      </div>
    </nav>
  )
}