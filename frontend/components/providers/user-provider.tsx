"use client"

import React, { createContext, useContext, useEffect, useState } from "react"
import { useRouter } from "next/navigation"

interface User {
  userId: string
  username: string
  pendingRegistration: boolean
  slack_name: string
  slack_workspace: string
  slack_workspace_domain: string
  slack_email: string
  slack_avatar_url: string
  isSlackAppInstalled: boolean
}

interface UserContextType {
  user: User | null
  loading: boolean
  refresh: () => Promise<void>
}

const UserContext = createContext<UserContextType>({
  user: null,
  loading: true,
  refresh: async () => {},
})

export const useUser = () => useContext(UserContext)

export function UserProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  const fetchUser = async () => {
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "https://api.coagent4u.com"
      const res = await fetch(`${apiUrl}/auth/me`, {
        credentials: "include", // Required for cross-origin cookies
      })
      
      if (res.ok) {
        const data = await res.json()
        setUser(data)
        
        // If registration is pending and we aren't already on onboarding, redirect
        if (data.pendingRegistration && !window.location.pathname.startsWith("/onboarding")) {
          router.push("/onboarding")
        }
      } else {
        setUser(null)
        // If unauthorized on a dashboard page, redirect to signin
        if (window.location.pathname.startsWith("/dashboard")) {
          router.push("/signin")
        }
      }
    } catch (err) {
      console.error("Failed to fetch user:", err)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUser()
  }, [])

  return (
    <UserContext.Provider value={{ user, loading, refresh: fetchUser }}>
      {children}
    </UserContext.Provider>
  )
}
