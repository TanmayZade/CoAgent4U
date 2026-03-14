'use client'

import { ReactNode } from 'react'
import { useSession } from '@/hooks/use-session'

interface ProtectedRouteProps {
  children: ReactNode
  requireOnboarded?: boolean
}

export function ProtectedRoute({ 
  children, 
  requireOnboarded = true 
}: ProtectedRouteProps) {
  const { session, loading } = useSession()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin mx-auto mb-4" />
          <p className="text-foreground/60">Loading...</p>
        </div>
      </div>
    )
  }

  // If requiring onboarded user and user is still in pending registration, loading state handles redirect
  if (requireOnboarded && session?.pendingRegistration) {
    return null // useSession hook will redirect
  }

  return <>{children}</>
}
