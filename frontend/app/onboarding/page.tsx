"use client"

import { useState, useEffect, useRef } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { 
  Check, 
  ChevronRight, 
  Calendar, 
  Lock,
  Sparkles,
  AlertCircle,
  Slack,
  User
} from "lucide-react"
import { cn } from "@/lib/utils"
import gsap from "gsap"
import { authAPI, integrationAPI, APIError } from "@/lib/api"

const steps = [
  { id: 1, title: "Choose Username", description: "Pick your unique handle" },
  { id: 2, title: "Slack Verified", description: "Your Slack identity verified" },
  { id: 3, title: "Connect Calendar", description: "Link your Google Calendar" },
  { id: 4, title: "Agent Ready", description: "Your agent is live" },
]

interface SessionData {
  userId?: string
  username?: string
  pendingRegistration: boolean
  slack_name?: string
  slack_workspace?: string
  slack_workspace_domain?: string
  slack_email?: string
  slack_avatar_url?: string
  error?: string
}

export default function OnboardingPage() {
  const [currentStep, setCurrentStep] = useState(1)
  const [sessionData, setSessionData] = useState<SessionData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isConnecting, setIsConnecting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [username, setUsername] = useState("")
  const [usernameError, setUsernameError] = useState<string | null>(null)
  const [isSubmittingUsername, setIsSubmittingUsername] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const contentRef = useRef<HTMLDivElement>(null)

  // Username validation regex
  const usernameRegex = /^[a-zA-Z0-9_-]{3,32}$/

  // Fetch session on mount and check for google=success param
  useEffect(() => {
    const fetchSession = async () => {
      try {
        const data = await authAPI.getMe()
        setSessionData(data)

        // Check URL for google=success parameter - skip to step 4
        const params = new URLSearchParams(window.location.search)
        if (params.get("google") === "success") {
          setCurrentStep(4)
          return
        }

        // pendingRegistration: false means username already chosen → skip to step 3
        if (data.pendingRegistration === false) {
          setCurrentStep(3)
        }
      } catch (err) {
        if (err instanceof APIError && err.status === 401) {
          // 401 means not authenticated → back to sign in
          window.location.replace("/signin")
          return
        }
        console.error("Failed to fetch session:", err)
        setError("Failed to load your profile. Please try again.")
      } finally {
        setIsLoading(false)
      }
    }

    fetchSession()
  }, [])

  useEffect(() => {
    if (!containerRef.current) return

    const ctx = gsap.context(() => {
      gsap.fromTo(
        contentRef.current,
        { y: 40, opacity: 0, scale: 0.95 },
        { y: 0, opacity: 1, scale: 1, duration: 0.8, ease: "power3.out", delay: 0.3 }
      )
    }, containerRef)

    return () => ctx.revert()
  }, [])

  // Validate username on change
  const handleUsernameChange = (value: string) => {
    setUsername(value)
    if (value && !usernameRegex.test(value)) {
      if (value.length < 3) {
        setUsernameError("Username must be at least 3 characters")
      } else if (value.length > 32) {
        setUsernameError("Username must be no more than 32 characters")
      } else {
        setUsernameError("Only letters, numbers, underscores, and hyphens allowed")
      }
    } else {
      setUsernameError(null)
    }
  }

  // Submit username to API
  const handleUsernameSubmit = async () => {
    if (!usernameRegex.test(username)) {
      setUsernameError("Please enter a valid username")
      return
    }

    setIsSubmittingUsername(true)
    setUsernameError(null)

    try {
      await authAPI.setUsername(username)
      // Success - move to step 2 (Slack Verified)
      setCurrentStep(2)
    } catch (err) {
      setUsernameError(err instanceof Error ? err.message : "Failed to set username")
    } finally {
      setIsSubmittingUsername(false)
    }
  }

  const handleConnectCalendar = () => {
    setIsConnecting(true)
    // Perform full browser redirect to Google Calendar OAuth
    window.location.href = integrationAPI.googleAuthorize()
  }

  const handleComplete = () => {
    // Redirect to dashboard
    window.location.href = "/dashboard"
  }

  // Extract initials from slack name for avatar
  const getInitials = (name?: string) => {
    if (!name) return "?"
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2)
  }

  if (isLoading) {
    return (
      <main className="min-h-screen flex items-center justify-center relative overflow-hidden py-12">
        <div className="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950" />
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute -top-40 -right-40 w-96 h-96 rounded-full opacity-20 blur-3xl" style={{ background: "radial-gradient(circle, #0EA5E9 0%, transparent 70%)" }} />
          <div className="absolute -bottom-40 -left-40 w-80 h-80 rounded-full opacity-15 blur-3xl" style={{ background: "radial-gradient(circle, #1E40AF 0%, transparent 70%)" }} />
        </div>
        <div className="relative z-10 flex items-center gap-3">
          <div className="w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-300 text-sm font-medium">Loading your profile...</p>
        </div>
      </main>
    )
  }

  return (
    <main ref={containerRef} className="min-h-screen flex items-center justify-center relative overflow-hidden py-12">
      {/* Deep charcoal background */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950" />
      
      {/* Glassmorphic glow effects */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {/* Electric blue - top right */}
        <div className="absolute -top-40 -right-40 w-96 h-96 rounded-full opacity-20 blur-3xl"
          style={{
            background: "radial-gradient(circle, #0EA5E9 0%, transparent 70%)",
          }}
        />
        {/* Navy - bottom left */}
        <div className="absolute -bottom-40 -left-40 w-80 h-80 rounded-full opacity-15 blur-3xl"
          style={{
            background: "radial-gradient(circle, #1E40AF 0%, transparent 70%)",
          }}
        />
      </div>

      <div ref={contentRef} className="relative z-10 w-full max-w-2xl mx-4">
        {/* Logo */}
        <div className="text-center mb-12">
          <Link href="/" className="inline-flex items-center gap-2.5 mb-6 group justify-center">
            <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-400/20 to-blue-500/20 border border-cyan-400/40 flex items-center justify-center group-hover:border-cyan-400/60 transition-colors backdrop-blur-sm">
              <span className="text-lg font-bold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">⚡</span>
            </div>
            <span className="text-xl font-semibold text-slate-100">CoAgent4U</span>
          </Link>
          <h1 className="text-3xl font-bold text-slate-50 mb-2">Complete Your Setup</h1>
          <p className="text-slate-400">Get your scheduling agent live in 4 quick steps</p>
        </div>

        {/* Step Indicator */}
        <div className="flex justify-between mb-10 px-2">
          {steps.map((step, index) => (
            <div key={step.id} className="flex-1 flex flex-col items-center">
              <div className="flex items-center w-full">
                <div
                  className={cn(
                    "w-12 h-12 rounded-full flex items-center justify-center font-semibold text-lg border-2 transition-all backdrop-blur-sm",
                    step.id < currentStep
                      ? "bg-cyan-500/20 border-cyan-400 text-cyan-300"
                      : step.id === currentStep
                      ? "bg-gradient-to-r from-cyan-500 to-blue-600 border-cyan-400 text-slate-950 shadow-lg shadow-cyan-500/50"
                      : "bg-white/5 border-white/10 text-slate-500"
                  )}
                >
                  {step.id < currentStep ? <Check className="w-6 h-6" /> : step.id}
                </div>
                {index < steps.length - 1 && (
                  <div
                    className={cn(
                      "flex-1 h-1 mx-2 rounded-full transition-all backdrop-blur-sm",
                      step.id < currentStep ? "bg-gradient-to-r from-cyan-500 to-blue-600" : "bg-white/10"
                    )}
                  />
                )}
              </div>
              <p className="text-sm font-medium text-slate-300 mt-2">{step.title}</p>
              <p className="text-xs text-slate-500">{step.description}</p>
            </div>
          ))}
        </div>

        {/* Step Content - Glassmorphic Card */}
        <div className="p-8 rounded-2xl border border-white/10 backdrop-blur-xl bg-white/5 shadow-2xl">
          {/* Error state */}
          {error && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/50 rounded-lg flex items-start gap-3 backdrop-blur-sm">
              <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-red-300 font-medium">{error}</p>
            </div>
          )}

          {/* Step 1: Choose Username */}
          {currentStep === 1 && (
            <div>
              <div className="text-center mb-8">
                <div className="w-16 h-16 mx-auto mb-4 rounded-xl bg-gradient-to-br from-cyan-400/20 to-blue-500/20 border border-cyan-400/40 flex items-center justify-center backdrop-blur-sm">
                  <User className="w-8 h-8 text-cyan-300" />
                </div>
                <h2 className="text-2xl font-bold text-slate-50 mb-2">Choose Your Username</h2>
                <p className="text-slate-400">This is how other users will find your agent</p>
              </div>

              {/* Username input */}
              <div className="space-y-4 mb-6">
                <div>
                  <label htmlFor="username" className="block text-sm font-medium text-slate-300 mb-2">
                    Your CoAgent4U Username
                  </label>
                  <Input
                    id="username"
                    type="text"
                    placeholder="e.g. john-agent"
                    value={username}
                    onChange={(e) => handleUsernameChange(e.target.value)}
                    disabled={isSubmittingUsername}
                    className={cn(
                      "h-12 bg-white/5 border-white/10 text-slate-100 placeholder:text-slate-500 focus:border-cyan-400/50 focus:ring-cyan-400/20",
                      usernameError && "border-red-500/50 focus:border-red-500/50"
                    )}
                  />
                  {usernameError && (
                    <p className="mt-2 text-sm text-red-400 flex items-center gap-1.5">
                      <AlertCircle className="w-4 h-4" />
                      {usernameError}
                    </p>
                  )}
                  <p className="mt-2 text-xs text-slate-500">
                    3-32 characters. Letters, numbers, underscores, and hyphens only.
                  </p>
                </div>
              </div>

              <Button
                onClick={handleUsernameSubmit}
                disabled={!username || !!usernameError || isSubmittingUsername}
                className="w-full font-semibold py-6 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 transition-all duration-200 shadow-lg hover:shadow-cyan-500/25 disabled:opacity-50"
              >
                {isSubmittingUsername ? (
                  <div className="flex items-center gap-2">
                    <div className="w-5 h-5 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin" />
                    <span>Setting username...</span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <span>Select Username</span>
                    <ChevronRight className="w-5 h-5" />
                  </div>
                )}
              </Button>
            </div>
          )}

          {/* Step 2: Slack Verified */}
          {currentStep === 2 && (
            <div className="text-center">
              {/* Verified checkmark animation */}
              <div className="relative w-20 h-20 mx-auto mb-6">
                <div className="absolute inset-0 rounded-full bg-gradient-to-r from-cyan-500/20 to-blue-600/20 backdrop-blur-sm border border-cyan-400/40 animate-pulse" />
                <div className="absolute inset-4 rounded-full bg-gradient-to-r from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/50">
                  <Check className="w-10 h-10 text-slate-950" />
                </div>
              </div>

              <h2 className="text-2xl font-bold text-slate-50 mb-2">Slack Connected</h2>
              <p className="text-slate-400 mb-6">Your Slack workspace identity has been verified</p>

              {/* User info card */}
              <div className="bg-white/5 backdrop-blur-sm rounded-lg p-6 border border-white/10 mb-6">
                <div className="flex items-center gap-4">
                  {sessionData?.slack_avatar_url ? (
                    <img 
                      src={sessionData.slack_avatar_url} 
                      alt={sessionData?.slack_name || "User avatar"}
                      className="w-12 h-12 rounded-lg object-cover border border-cyan-400/40"
                    />
                  ) : (
                    <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-cyan-400/20 to-blue-500/20 border border-cyan-400/40 flex items-center justify-center">
                      <span className="text-sm font-bold text-slate-200">{getInitials(sessionData?.slack_name)}</span>
                    </div>
                  )}
                  <div className="text-left">
                    <p className="font-medium text-slate-50">{sessionData?.slack_name || "User"}</p>
                    <div className="flex items-center gap-1 mt-1">
                      <Slack className="w-3 h-3 text-slate-400" />
                      <p className="text-sm text-slate-400">
                        {sessionData?.slack_workspace || "Slack Workspace"}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <Button
                onClick={() => setCurrentStep(3)}
                className="w-full font-semibold py-6 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 transition-all duration-200 shadow-lg hover:shadow-cyan-500/25"
              >
                Continue Setup
                <ChevronRight className="w-5 h-5 ml-2" />
              </Button>
            </div>
          )}

          {/* Step 3: Connect Google Calendar */}
          {currentStep === 3 && (
            <div>
              <div className="text-center mb-8">
                <div className="w-16 h-16 mx-auto mb-4 rounded-xl bg-gradient-to-br from-cyan-400/20 to-blue-500/20 border border-cyan-400/40 flex items-center justify-center backdrop-blur-sm">
                  <Calendar className="w-8 h-8 text-cyan-300" />
                </div>
                <h2 className="text-2xl font-bold text-slate-50 mb-2">Connect Google Calendar</h2>
                <p className="text-slate-400">Allow your agent to manage your schedule</p>
              </div>

              {/* Permissions card */}
              <div className="bg-white/5 backdrop-blur-sm rounded-lg p-6 border border-white/10 mb-6">
                <h3 className="text-sm font-semibold text-slate-50 mb-4">Required Permissions</h3>
                <div className="space-y-3">
                  <div className="flex items-start gap-3">
                    <Lock className="w-4 h-4 text-cyan-400 mt-1 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-medium text-slate-200">Read Calendar</p>
                      <p className="text-xs text-slate-500">View your availability and events</p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Lock className="w-4 h-4 text-cyan-400 mt-1 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-medium text-slate-200">Create Events</p>
                      <p className="text-xs text-slate-500">Schedule meetings with your approval</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Security note */}
              <div className="flex items-start gap-3 mb-8 p-3 bg-cyan-500/10 backdrop-blur-sm rounded-lg border border-cyan-400/30">
                <Lock className="w-4 h-4 text-cyan-400 mt-0.5 flex-shrink-0" />
                <p className="text-xs text-cyan-300">
                  Only refresh tokens are stored. Your calendar data never touches our servers.
                </p>
              </div>

              <div className="flex gap-3">
                <Button
                  onClick={handleConnectCalendar}
                  disabled={isConnecting}
                  className="flex-1 font-semibold py-6 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 transition-all duration-200 shadow-lg hover:shadow-cyan-500/25 disabled:opacity-50"
                >
                  {isConnecting ? (
                    <div className="flex items-center gap-2">
                      <div className="w-5 h-5 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin" />
                      <span>Redirecting...</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-2">
                      <Calendar className="w-5 h-5" />
                      <span>Connect Google Calendar</span>
                    </div>
                  )}
                </Button>
                <Button
                  onClick={() => setCurrentStep(4)}
                  variant="outline"
                  className="flex-1 font-semibold py-6 rounded-lg border border-white/10 bg-white/5 hover:bg-white/10 text-slate-300 transition-all duration-200 backdrop-blur-sm"
                >
                  Skip for Now
                </Button>
              </div>
            </div>
          )}

          {/* Step 4: Agent Ready */}
          {currentStep === 4 && (
            <div className="text-center">
              {/* Celebration animation */}
              <div className="relative w-24 h-24 mx-auto mb-6">
                <div className="absolute inset-0 rounded-full bg-gradient-to-r from-cyan-500/20 to-blue-600/20 backdrop-blur-sm border border-cyan-400/40 animate-pulse" />
                <div className="absolute inset-2 rounded-full bg-gradient-to-r from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/50">
                  <Sparkles className="w-12 h-12 text-slate-950" />
                </div>
                <div className="absolute -inset-2 rounded-full border border-cyan-400/30 animate-spin" style={{ animationDuration: '3s' }} />
              </div>

              <h2 className="text-2xl font-bold text-slate-50 mb-2">Your Agent is Ready</h2>
              <p className="text-slate-400 mb-8">Start scheduling through Slack right away</p>

              {/* Agent status visualization */}
              <div className="bg-white/5 backdrop-blur-sm rounded-lg p-6 border border-white/10 mb-8">
                <div className="flex items-center justify-center gap-8">
                  {/* Slack */}
                  <div className="flex flex-col items-center">
                    <div className="w-12 h-12 rounded-full bg-white/10 border border-white/20 flex items-center justify-center mb-2 backdrop-blur-sm">
                      <Slack className="w-5 h-5 text-slate-300" />
                    </div>
                    <span className="text-xs text-slate-400">Slack</span>
                  </div>

                  {/* Connection line */}
                  <div className="w-8 h-0.5 bg-gradient-to-r from-cyan-500 to-blue-600" />

                  {/* Agent (center, larger) */}
                  <div className="flex flex-col items-center">
                    <div className="relative">
                      <div className="w-16 h-16 rounded-full bg-gradient-to-r from-cyan-500/20 to-blue-600/20 border-2 border-cyan-400 flex items-center justify-center mb-2 backdrop-blur-sm shadow-lg shadow-cyan-500/25">
                        <span className="text-xs font-bold text-cyan-300">Agent</span>
                      </div>
                      <div className="absolute -inset-1 rounded-full border border-cyan-400/50 animate-spin" style={{ animationDuration: '4s' }} />
                    </div>
                    <span className="text-xs font-semibold text-cyan-400">ACTIVE</span>
                  </div>

                  {/* Connection line */}
                  <div className="w-8 h-0.5 bg-gradient-to-r from-blue-600 to-cyan-500" />

                  {/* Calendar */}
                  <div className="flex flex-col items-center">
                    <div className="w-12 h-12 rounded-full bg-white/10 border border-white/20 flex items-center justify-center mb-2 backdrop-blur-sm">
                      <Calendar className="w-5 h-5 text-slate-300" />
                    </div>
                    <span className="text-xs text-slate-400">Calendar</span>
                  </div>
                </div>
              </div>

              <Button
                onClick={handleComplete}
                className="w-full font-semibold py-6 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 transition-all duration-200 shadow-lg hover:shadow-cyan-500/25"
              >
                Go to Dashboard
                <ChevronRight className="w-5 h-5 ml-2" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </main>
  )
}
