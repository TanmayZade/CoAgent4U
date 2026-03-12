"use client"

import { useState, useEffect, useRef } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Shield, Slack } from "lucide-react"
import { useRouter } from "next/navigation"
import gsap from "gsap"

export default function SignInPage() {
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()
  const containerRef = useRef<HTMLDivElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)

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
    await new Promise((resolve) => setTimeout(resolve, 1500))
    router.push("/onboarding")
  }

  return (
    <main ref={containerRef} className="min-h-screen flex items-center justify-center relative overflow-hidden noise-overlay">
      {/* Background */}
      <div className="absolute inset-0 bg-charcoal" />
      
      {/* Subtle accent glow */}
      <div className="absolute inset-0 overflow-hidden">
        <div
          className="absolute w-[500px] h-[500px] top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full opacity-10"
          style={{
            background: "radial-gradient(circle, rgba(232,90,44,0.4) 0%, transparent 70%)",
          }}
        />
      </div>

      {/* Content */}
      <div ref={cardRef} className="relative z-10 w-full max-w-sm mx-4">
        {/* Logo */}
        <div className="text-center mb-10">
          <Link href="/" className="inline-flex items-center gap-3 mb-4">
            <div className="w-12 h-12 rounded-xl bg-accent flex items-center justify-center">
              <span className="text-xl font-bold text-cream">CA</span>
            </div>
          </Link>
          <h1 className="text-xl font-semibold text-cream font-[family-name:var(--font-display)]">
            CoAgent4U
          </h1>
        </div>

        {/* Sign In Card */}
        <div className="p-8 bg-charcoal-light rounded-xl border border-border">
          <div className="text-center mb-8">
            <h2 className="text-lg font-semibold text-cream mb-2 font-[family-name:var(--font-display)]">
              Connect Your Agent
            </h2>
            <p className="text-sm text-foreground-secondary">
              Sign in with your Slack workspace to provision your personal agent
            </p>
          </div>

          <Button
            onClick={handleSlackSignIn}
            disabled={isLoading}
            className="w-full bg-[#4A154B] hover:bg-[#611f64] text-white py-6 text-base font-medium transition-all rounded-lg"
          >
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Connecting...</span>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Slack className="w-5 h-5" />
                <span>Sign in with Slack</span>
              </div>
            )}
          </Button>

          {/* Privacy note */}
          <div className="flex items-center justify-center gap-2 mt-6 text-xs text-foreground-muted">
            <Shield className="w-4 h-4 text-accent" />
            <span>Your data stays yours.</span>
          </div>
        </div>

        {/* Terms */}
        <p className="text-center text-xs text-foreground-muted mt-6">
          By signing in, you agree to our{" "}
          <Link href="/terms" className="text-accent hover:underline">
            Terms of Service
          </Link>{" "}
          and{" "}
          <Link href="/privacy" className="text-accent hover:underline">
            Privacy Policy
          </Link>
        </p>
      </div>
    </main>
  )
}
