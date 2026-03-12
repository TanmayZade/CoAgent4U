"use client"

import { Sidebar } from "@/components/layout/sidebar"
import { Bell } from "lucide-react"
import { Button } from "@/components/ui/button"

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const currentHour = new Date().getHours()
  const greeting =
    currentHour < 12
      ? "Good morning"
      : currentHour < 18
      ? "Good afternoon"
      : "Good evening"

  return (
    <div className="min-h-screen bg-charcoal">
        <Sidebar />
        
        {/* Main content */}
        <div className="ml-60">
          {/* Top bar */}
          <header className="h-16 border-b border-border bg-charcoal/80 backdrop-blur-xl sticky top-0 z-40">
            <div className="h-full px-6 flex items-center justify-between">
              <div>
                <p className="text-cream">
                  <span className="text-foreground-secondary">{greeting}, </span>
                  <span className="font-medium">Alex</span>
                  <span className="text-foreground-secondary">
                    . Your agent handled{" "}
                  </span>
                  <span className="text-accent font-medium">3 requests</span>
                  <span className="text-foreground-secondary"> today.</span>
                </p>
              </div>
              <div className="flex items-center gap-4">
                {/* Agent Status */}
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-accent/10 border border-accent/30">
                  <span className="relative flex h-2 w-2">
                    <span className="absolute inline-flex h-full w-full rounded-full bg-accent opacity-75 status-pulse" />
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-accent" />
                  </span>
                  <span className="text-xs font-medium text-accent">ACTIVE</span>
                </div>
                
                {/* Notifications */}
                <Button
                  variant="ghost"
                  size="icon"
                  className="relative text-foreground-secondary hover:text-cream"
                >
                  <Bell className="w-5 h-5" />
                  <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-accent" />
                </Button>
              </div>
            </div>
          </header>

          {/* Page content */}
          <main className="p-6">{children}</main>
        </div>
      </div>
  )
}
