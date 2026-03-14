"use client"

import { useState, useEffect, createContext, useContext } from "react"
import { Sidebar } from "@/components/layout/sidebar"
import { ThemeToggle } from "@/components/theme-toggle"
import { Bell, LogOut } from "lucide-react"
import { Button } from "@/components/ui/button"
import { authAPI, APIError } from "@/lib/api"

// User data context
interface UserData {
  username: string
  slack_name: string
  slack_workspace: string
  slack_workspace_domain: string
  slack_avatar_url: string
  isSlackAppInstalled: boolean
}

interface UserContextType {
  user: UserData | null
  isLoading: boolean
  error: string | null
}

const UserContext = createContext<UserContextType | undefined>(undefined)

export function useUser() {
  const context = useContext(UserContext)
  if (!context) {
    throw new Error('useUser must be used within UserProvider')
  }
  return context
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [user, setUser] = useState<UserData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const currentHour = new Date().getHours()
  const greeting =
    currentHour < 12
      ? "Good morning"
      : currentHour < 18
      ? "Good afternoon"
      : "Good evening"

  // Fetch user data on mount
  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const data = await authAPI.getMe()
        
        // Check authentication status
        if (!data) {
          window.location.replace("/signin")
          return
        }

        // Redirect to onboarding if still pending registration
        if (data.pendingRegistration === true) {
          window.location.replace("/onboarding")
          return
        }

        // Store user data
        setUser(data)
      } catch (err) {
        console.error("[v0] Failed to fetch user data:", err)
        if (err instanceof APIError && err.status === 401) {
          // Unauthorized - redirect to signin
          window.location.replace("/signin")
          return
        }
        setError("Failed to load user data")
      } finally {
        setIsLoading(false)
      }
    }

    fetchUserData()
  }, [])

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      // Call the backend logout endpoint
      await authAPI.logout()
    } catch (error) {
      console.error("Logout failed:", error)
    } finally {
      // Redirect back to sign-in page regardless of API response
      window.location.href = "/signin"
    }
  }

  // Get greeting name - first word of slack_name or fallback to username
  const greetingName = user?.slack_name 
    ? user.slack_name.split(' ')[0]
    : user?.username || "there"

  // Get user initials for avatar
  const initials = user?.slack_name
    ? user.slack_name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : "?"

  return (
    <UserContext.Provider value={{ user, isLoading, error }}>
      <div className="min-h-screen bg-background">
        <Sidebar user={user} isLoading={isLoading} />
        
        {/* Main content */}
        <div className="ml-60">
          {/* Top bar */}
          <header className="h-16 border-b border-border/50 bg-background/80 backdrop-blur-sm sticky top-0 z-40">
            <div className="h-full px-6 flex items-center justify-between">
              <div>
                <p className="text-foreground text-sm">
                  <span className="text-foreground/60">{greeting}, </span>
                  <span className="font-semibold">{greetingName}</span>
                  <span className="text-foreground/60">. Your agent handled </span>
                  <span className="text-primary font-semibold">3 requests</span>
                  <span className="text-foreground/60"> today.</span>
                </p>
              </div>
              <div className="flex items-center gap-4">
                {/* Agent Status */}
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-primary/10 border border-primary/20">
                  <span className="relative flex h-2 w-2">
                    <span className="absolute inline-flex h-full w-full rounded-full bg-primary opacity-75 animate-pulse" />
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-primary" />
                  </span>
                  <span className="text-xs font-semibold text-primary">ACTIVE</span>
                </div>
                
                {/* Notifications */}
                <Button
                  variant="ghost"
                  size="icon"
                  className="relative text-foreground/60 hover:text-foreground h-10 w-10"
                >
                  <Bell className="w-5 h-5" />
                  <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-destructive" />
                </Button>

                {/* Theme Toggle */}
                <ThemeToggle />

                {/* Logout */}
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleLogout}
                  disabled={isLoggingOut}
                  className="text-foreground/60 hover:text-destructive hover:bg-destructive/10 h-10 w-10 disabled:opacity-50"
                  title="Sign out"
                >
                  {isLoggingOut ? (
                    <div className="w-5 h-5 border-2 border-destructive/30 border-t-destructive rounded-full animate-spin" />
                  ) : (
                    <LogOut className="w-5 h-5" />
                  )}
                </Button>
              </div>
            </div>
          </header>

          {/* Page content */}
          <main className="p-6">{children}</main>
        </div>
      </div>
    </UserContext.Provider>
  )
}
