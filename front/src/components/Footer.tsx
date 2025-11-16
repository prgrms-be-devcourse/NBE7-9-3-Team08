import Link from "next/link"
import { Sparkles, Github, Mail } from "lucide-react"

export default function Footer() {
  return (
    <footer className="border-t border-gray-200 bg-background">
      <div className="w-full max-w-none px-4 sm:px-8 py-8">
        {/* 상위 섹션: 브랜드/바로가기/문의 */}
        <div className="flex flex-col gap-8 md:flex-row md:items-start md:justify-between">
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Sparkles className="h-5 w-5" />
            </div>
            <span className="text-lg font-semibold">PortfolioIQ</span>
          </div>
          <p className="text-xs text-muted-foreground">
            © 2025 PortfolioIQ. All rights reserved.
          </p>
        </div>

          <div className="space-y-2">
            <p className="text-sm font-semibold text-foreground">문의</p>
            <div className="flex items-center gap-3 text-sm text-muted-foreground">
              <Github className="h-4 w-4" />
              <a href="https://github.com/prgrms-be-devcourse/NBE7-9-3-Team08" target="_blank" rel="noreferrer" className="hover:text-foreground">
                GitHub
              </a>
            </div>
            <div className="flex items-center gap-3 text-sm text-muted-foreground">
              <Mail className="h-4 w-4" />
              <a href="mailto:portfolioiq.official@gmail.com" className="hover:text-foreground">
                portfolioiq.official@gmail.com
              </a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  )
}
