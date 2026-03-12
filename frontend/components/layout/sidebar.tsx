"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { 
  Home, 
  Calendar, 
  Users, 
  FileText, 
  Shield, 
  Settings,
  LogOut,
  Slack
} from "lucide-react"
import { Button } from "@/components/ui/button"

const navItems = [
  { href: "/dashboard", icon: Home, label: "Dashboard" },
  { href: "/dashboard/calendar", icon: Calendar, label: "Calendar View" },
  { href: "/dashboard/coordinations", icon: Users, label: "Coordinations" },
  { href: "/dashboard/audit", icon: FileText, label: "Audit Log" },
  { href: "/dashboard/data", icon: Shield, label: "Data & Permissions" },
  { href: "/dashboard/settings", icon: Settings, label: "Settings" },
]

export function Sidebar() {
  const pathname = usePathname()

  return (
    <aside className="w-60 h-screen bg-charcoal border-r border-border flex flex-col fixed left-0 top-0">
      {/* Logo */}
      <div className="p-4 border-b border-border">
        <Link href="/" className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-accent flex items-center justify-center">
            <span className="text-sm font-bold text-cream">CA</span>
          </div>
          <span className="text-lg font-semibold text-cream font-[family-name:var(--font-display)]">
            CoAgent4U
          </span>
        </Link>
      </div>

      {/* User info */}
      <div className="p-4 border-b border-border">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-charcoal-lighter flex items-center justify-center border border-border">
            <span className="text-sm font-medium text-cream">AJ</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-cream truncate">Alex Johnson</p>
            <div className="flex items-center gap-1.5">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full rounded-full bg-accent opacity-75 status-pulse" />
                <span className="relative inline-flex rounded-full h-2 w-2 bg-accent" />
              </span>
              <span className="text-xs text-accent">Agent Active</span>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const isActive = pathname === item.href
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-200",
                isActive
                  ? "bg-accent/10 text-accent"
                  : "text-foreground-secondary hover:text-cream hover:bg-charcoal-light"
              )}
            >
              <item.icon className="w-5 h-5" />
              {item.label}
            </Link>
          )
        })}
      </nav>

      {/* Bottom section */}
      <div className="p-3 border-t border-border space-y-2">
        <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-charcoal-light">
          <Slack className="w-4 h-4 text-foreground-muted" />
          <span className="text-xs text-foreground-muted">Acme Corp</span>
        </div>
        <Button
          variant="ghost"
          className="w-full justify-start text-foreground-secondary hover:text-destructive hover:bg-destructive/10"
        >
          <LogOut className="w-4 h-4 mr-2" />
          Sign Out
        </Button>
      </div>
    </aside>
  )
}
