"use client"

import { useState, useEffect, useRef } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Shield, Slack, AlertCircle } from "lucide-react"
import gsap from "gsap"
import { authAPI, integrationAPI } from "@/lib/api"

export default function SignInPage() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)

  // On mount: check session and redirect if already authenticated
  useEffect(() => {
    const checkSession = async () => {
      // Check URL error params first
      const params = new URLSearchParams(window.location.search)
      const errorParam = params.get("error")
      if (errorParam === "access_denied") {
        setError("Sign in was cancelled")
      } else if (errorParam) {
        setError("Sign in failed")
      }

      try {
        const data = await authAPI.checkSession()
        if (data.authenticated === true) {
          if (data.pendingRegistration === true) {
            window.location.replace("/onboarding")
          } else {
            window.location.replace("/dashboard")
          }
        }
      } catch {
        // Not authenticated or network error — stay on signin
      }
    }

    checkSession()
  }, [])

  useEffect(() => {
    if (!containerRef.current) return

    const ctx = gsap.context(() => {
      gsap.fromTo(
        cardRef.current,
        { y: 40, opacity: 0, scale: 0.95 },
        { y: 0, opacity: 1, scale: 1, duration: 0.8, ease: "power3.out", delay: 0.2 }
      )
    }, containerRef)

    return () => ctx.revert()
  }, [])

  const handleSlackSignIn = async () => {
    setIsLoading(true)
    setError(null)
    try {
      // Redirect to Slack OAuth endpoint
      window.location.href = authAPI.slackStart()
    } catch (err) {
      setError("Failed to connect with Slack. Please try again.")
      setIsLoading(false)
    }
  }

  return (
    <main ref={containerRef} className="min-h-screen flex items-center justify-center relative overflow-hidden">
      {/* Deep charcoal background */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950" />
      
      {/* Glassmorphic glow effect */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {/* Electric blue accent glow - top left */}
        <div className="absolute -top-40 -left-40 w-80 h-80 rounded-full opacity-20 blur-3xl"
          style={{
            background: "radial-gradient(circle, #0EA5E9 0%, transparent 70%)",
          }}
        />
        {/* Navy accent - bottom right */}
        <div className="absolute -bottom-40 -right-40 w-96 h-96 rounded-full opacity-15 blur-3xl"
          style={{
            background: "radial-gradient(circle, #1E40AF 0%, transparent 70%)",
          }}
        />
      </div>

      {/* Content */}
      <div ref={cardRef} className="relative z-10 w-full max-w-md mx-4">
        {/* Logo Section */}
        <div className="text-center mb-12">
          <Link href="/" className="inline-flex items-center gap-2.5 mb-6 group">
            <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-400/20 to-blue-500/20 border border-cyan-400/40 flex items-center justify-center group-hover:border-cyan-400/60 transition-colors backdrop-blur-sm">
              <span className="text-lg font-bold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">⚡</span>
            </div>
            <span className="text-xl font-semibold text-slate-100 hidden sm:inline">CoAgent4U</span>
          </Link>
          <h1 className="text-3xl font-bold text-slate-50 mb-2">Sign In</h1>
          <p className="text-slate-400">Connect with Slack to access your scheduling agent</p>
        </div>

        {/* Glassmorphic Card */}
        <div className="p-8 rounded-2xl border border-white/10 backdrop-blur-xl bg-white/5 shadow-2xl hover:shadow-3xl transition-shadow hover:border-white/20">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-50 mb-1">Welcome Back</h2>
            <p className="text-sm text-slate-400">
              Sign in with your Slack workspace to access CoAgent4U
            </p>
          </div>

          {/* Error message - Red alert */}
          {error && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/50 rounded-lg flex items-start gap-3 backdrop-blur-sm">
              <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-red-300 font-medium">{error}</p>
            </div>
          )}

          {/* Slack Sign In Button */}
          <Button
            onClick={handleSlackSignIn}
            disabled={isLoading}
            size="lg"
            className="w-full font-semibold py-6 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 transition-all duration-200 shadow-lg hover:shadow-cyan-500/25 disabled:opacity-50"
          >
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin" />
                <span>Connecting...</span>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Slack className="w-5 h-5" />
                <span>Continue with Slack</span>
              </div>
            )}
          </Button>

          {/* Security note */}
          <div className="flex items-center justify-center gap-2 mt-6 text-xs text-slate-500">
            <Shield className="w-4 h-4" />
            <span>Secure OAuth 2.0 login</span>
          </div>
        </div>

        {/* Terms */}
        <p className="text-center text-xs text-slate-500 mt-8">
          By signing in, you agree to our{" "}
          <Link href="/terms" className="text-cyan-400 hover:text-cyan-300 transition-colors font-medium">
            Terms of Service
          </Link>{" "}
          and{" "}
          <Link href="/privacy" className="text-cyan-400 hover:text-cyan-300 transition-colors font-medium">
            Privacy Policy
          </Link>
        </p>
      </div>
    </main>
  )
}
