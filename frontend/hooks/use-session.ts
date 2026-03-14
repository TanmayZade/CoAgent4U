'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'

export interface SessionData {
  authenticated: boolean
  username?: string
  email?: string
  pendingRegistration: boolean
  googleCalendarConnected?: boolean
}

export function useSession() {
  const [session, setSession] = useState<SessionData | null>(null)
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
    const checkSession = async () => {
      try {
        // GET /auth/session
        // Returns: { authenticated: boolean, username: string, pendingRegistration: boolean }
        
        // Mock implementation - replace with actual API call
        const response = await fetch('/api/auth/session', {
          credentials: 'include',
        }).catch(() => null)

        if (response?.ok) {
          const data = await response.json()
          setSession(data)
          
          // Route based on session state
          if (!data.authenticated) {
            router.replace('/signin')
          } else if (data.pendingRegistration) {
            router.replace('/onboarding')
          }
        } else {
          // No valid session
          setSession({
            authenticated: false,
            pendingRegistration: false,
          })
          router.replace('/signin')
        }
      } catch (error) {
        console.error('Session check failed:', error)
        setSession({
          authenticated: false,
          pendingRegistration: false,
        })
        router.replace('/signin')
      } finally {
        setLoading(false)
      }
    }

    checkSession()
  }, [router])

  return { session, loading }
}
