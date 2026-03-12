"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { GlowCard } from "@/components/common/glow-card"
import { 
  Check, 
  ChevronRight, 
  Calendar, 
  Lock, 
  Slack,
  Sparkles,
  ExternalLink
} from "lucide-react"
import { cn } from "@/lib/utils"

const steps = [
  { id: 1, title: "Slack Connected" },
  { id: 2, title: "Connect Calendar" },
  { id: 3, title: "Agent Ready" },
]

export default function OnboardingPage() {
  const [currentStep, setCurrentStep] = useState(1)
  const [isConnecting, setIsConnecting] = useState(false)
  const [gdprConsent, setGdprConsent] = useState(false)
  const router = useRouter()

  const handleConnectCalendar = async () => {
    setIsConnecting(true)
    await new Promise((resolve) => setTimeout(resolve, 2000))
    setIsConnecting(false)
    setCurrentStep(3)
  }

  const handleComplete = () => {
    router.push("/dashboard")
  }

  return (
    <main className="min-h-screen flex items-center justify-center relative overflow-hidden bg-charcoal">
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-br from-charcoal via-charcoal-light to-charcoal" />

      {/* Celebration particles for step 3 */}
      {currentStep === 3 && (
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          {[...Array(30)].map((_, i) => (
            <div
              key={i}
              className="absolute w-2 h-2 rounded-full bg-accent"
              style={{
                left: `${Math.random() * 100}%`,
                top: `${Math.random() * 100}%`,
                animation: `float ${2 + Math.random() * 3}s ease-in-out infinite`,
                animationDelay: `${Math.random() * 2}s`,
                opacity: 0.6,
              }}
            />
          ))}
        </div>
      )}

      <div className="relative z-10 w-full max-w-lg mx-4 py-8">
        {/* Logo */}
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-accent flex items-center justify-center">
              <span className="text-lg font-bold text-charcoal">CA</span>
            </div>
            <span className="text-xl font-bold text-cream font-[family-name:var(--font-display)]">
              CoAgent4U
            </span>
          </Link>
        </div>

        {/* Stepper */}
        <div className="flex items-center justify-center gap-2 mb-8">
          {steps.map((step, index) => (
            <div key={step.id} className="flex items-center">
              <div
                className={cn(
                  "w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium transition-all duration-300",
                  step.id < currentStep
                    ? "bg-emerald-500 text-white"
                    : step.id === currentStep
                    ? "bg-accent text-charcoal"
                    : "bg-charcoal-lighter text-cream/50"
                )}
              >
                {step.id < currentStep ? (
                  <Check className="w-4 h-4" />
                ) : (
                  step.id
                )}
              </div>
              {index < steps.length - 1 && (
                <div
                  className={cn(
                    "w-12 h-0.5 mx-2 transition-colors duration-300",
                    step.id < currentStep ? "bg-emerald-500" : "bg-charcoal-lighter"
                  )}
                />
              )}
            </div>
          ))}
        </div>

        {/* Step Content */}
        <GlowCard className="p-8">
          {/* Step 1: Slack Connected */}
          {currentStep === 1 && (
            <div className="text-center">
              {/* Animated checkmark */}
              <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-emerald-500/20 flex items-center justify-center">
                <svg
                  className="w-10 h-10 text-emerald-500"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path
                    d="M5 13l4 4L19 7"
                    className="animate-[draw_0.5s_ease-out_forwards]"
                    style={{
                      strokeDasharray: 24,
                      strokeDashoffset: 24,
                      animation: "draw 0.5s ease-out 0.3s forwards",
                    }}
                  />
                </svg>
              </div>

              <h2 className="text-xl font-semibold text-cream mb-2 font-[family-name:var(--font-display)]">
                Slack Connected
              </h2>
              <p className="text-cream/70 mb-6">
                Your Slack identity has been verified
              </p>

              {/* User info */}
              <div className="bg-charcoal-light rounded-lg p-4 border border-charcoal-lighter mb-6">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-lg bg-accent/20 flex items-center justify-center">
                    <Slack className="w-6 h-6 text-accent" />
                  </div>
                  <div className="text-left">
                    <p className="font-medium text-cream">Alex Johnson</p>
                    <p className="text-sm text-cream/50 font-mono">
                      @alex.johnson
                    </p>
                  </div>
                </div>
                <div className="mt-3 pt-3 border-t border-charcoal-lighter">
                  <p className="text-xs text-cream/50 font-mono">
                    Workspace: Acme Corp
                  </p>
                </div>
              </div>

              <Button
                onClick={() => setCurrentStep(2)}
                className="w-full bg-accent hover:bg-accent-light text-charcoal font-semibold py-6"
              >
                Continue
                <ChevronRight className="w-4 h-4 ml-2" />
              </Button>
            </div>
          )}

          {/* Step 2: Connect Google Calendar */}
          {currentStep === 2 && (
            <div>
              <div className="text-center mb-6">
                <div className="w-16 h-16 mx-auto mb-4 rounded-xl bg-accent/10 flex items-center justify-center">
                  <Calendar className="w-8 h-8 text-accent" />
                </div>
                <h2 className="text-xl font-semibold text-cream mb-2 font-[family-name:var(--font-display)]">
                  Connect Google Calendar
                </h2>
                <p className="text-sm text-cream/70">
                  Allow CoAgent4U to read and create calendar events
                </p>
              </div>

              {/* Permissions breakdown */}
              <div className="bg-charcoal-light rounded-lg p-4 border border-charcoal-lighter mb-6">
                <h3 className="text-sm font-medium text-cream mb-3">
                  Requested Permissions
                </h3>
                <div className="space-y-3">
                  <div className="flex items-start gap-3">
                    <Lock className="w-4 h-4 text-accent mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-sm text-cream">calendar.readonly</p>
                      <p className="text-xs text-cream/50">
                        View your calendar availability
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Lock className="w-4 h-4 text-accent mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-sm text-cream">calendar.events</p>
                      <p className="text-xs text-cream/50">
                        Create events with your approval
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Data storage notice */}
              <div className="flex items-start gap-2 mb-6 p-3 bg-emerald-500/10 rounded-lg border border-emerald-500/20">
                <Lock className="w-4 h-4 text-emerald-500 mt-0.5 flex-shrink-0" />
                <p className="text-xs text-cream/70">
                  We store only a refresh token. No calendar data is stored on our
                  servers.
                </p>
              </div>

              {/* GDPR Consent */}
              <div className="flex items-start gap-3 mb-6">
                <Checkbox
                  id="gdpr"
                  checked={gdprConsent}
                  onCheckedChange={(checked) => setGdprConsent(checked as boolean)}
                  className="mt-0.5 border-charcoal-lighter data-[state=checked]:bg-accent data-[state=checked]:border-accent"
                />
                <label
                  htmlFor="gdpr"
                  className="text-sm text-cream/70 cursor-pointer"
                >
                  I consent to CoAgent4U processing my calendar data in accordance
                  with the{" "}
                  <Link href="/privacy" className="text-accent hover:underline">
                    Privacy Policy
                  </Link>
                </label>
              </div>

              <Button
                onClick={handleConnectCalendar}
                disabled={!gdprConsent || isConnecting}
                className="w-full bg-accent hover:bg-accent-light text-charcoal font-semibold py-6 disabled:opacity-50"
              >
                {isConnecting ? (
                  <div className="flex items-center gap-2">
                    <div className="w-5 h-5 border-2 border-charcoal/30 border-t-charcoal rounded-full animate-spin" />
                    <span>Connecting...</span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <Calendar className="w-5 h-5" />
                    <span>Connect Google Calendar</span>
                  </div>
                )}
              </Button>
            </div>
          )}

          {/* Step 3: Agent Ready */}
          {currentStep === 3 && (
            <div className="text-center">
              {/* Celebration animation */}
              <div className="relative w-24 h-24 mx-auto mb-6">
                <div className="absolute inset-0 rounded-full bg-accent animate-pulse" />
                <div className="absolute inset-2 rounded-full bg-charcoal flex items-center justify-center">
                  <Sparkles className="w-10 h-10 text-accent" />
                </div>
                <div className="absolute -inset-4 rounded-full border border-accent/30 animate-ping" />
              </div>

              <h2 className="text-2xl font-bold text-cream mb-2 font-[family-name:var(--font-display)]">
                Your Personal Agent is Live
              </h2>
              <p className="text-cream/70 mb-8">
                Start scheduling meetings through Slack
              </p>

              {/* Agent node visualization */}
              <div className="bg-charcoal-light rounded-lg p-6 border border-charcoal-lighter mb-8">
                <div className="flex items-center justify-center gap-8">
                  {/* Slack node */}
                  <div className="flex flex-col items-center">
                    <div className="w-12 h-12 rounded-full bg-charcoal-lighter border border-charcoal-light flex items-center justify-center">
                      <Slack className="w-6 h-6 text-cream" />
                    </div>
                    <span className="text-xs text-cream/50 mt-2">Slack</span>
                  </div>

                  {/* Connection line */}
                  <div className="w-8 h-0.5 bg-accent/50" />

                  {/* Agent node (center, larger) */}
                  <div className="flex flex-col items-center">
                    <div className="relative">
                      <div className="w-16 h-16 rounded-full bg-accent/20 border-2 border-accent flex items-center justify-center">
                        <span className="text-sm font-bold text-accent">You</span>
                      </div>
                      <div className="absolute -inset-1 rounded-full border border-accent/50 animate-agent-pulse" />
                    </div>
                    <span className="text-xs text-accent mt-2 font-medium">
                      Agent Active
                    </span>
                  </div>

                  {/* Connection line */}
                  <div className="w-8 h-0.5 bg-accent/50" />

                  {/* Calendar node */}
                  <div className="flex flex-col items-center">
                    <div className="w-12 h-12 rounded-full bg-charcoal-lighter border border-charcoal-light flex items-center justify-center">
                      <Calendar className="w-6 h-6 text-cream" />
                    </div>
                    <span className="text-xs text-cream/50 mt-2">Calendar</span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row gap-3">
                <Button
                  variant="outline"
                  className="flex-1 border-charcoal-lighter hover:border-accent/50 hover:bg-accent/5 py-6"
                >
                  <ExternalLink className="w-4 h-4 mr-2" />
                  Open Slack
                </Button>
                <Button
                  onClick={handleComplete}
                  className="flex-1 bg-accent hover:bg-accent-light text-charcoal font-semibold py-6"
                >
                  Go to Dashboard
                  <ChevronRight className="w-4 h-4 ml-2" />
                </Button>
              </div>
            </div>
          )}
        </GlowCard>
      </div>

      <style jsx>{`
        @keyframes draw {
          to {
            stroke-dashoffset: 0;
          }
        }
      `}</style>
    </main>
  )
}
