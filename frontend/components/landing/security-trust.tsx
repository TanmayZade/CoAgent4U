"use client"

import { KeyRound, Calendar, Shield, FileText } from "lucide-react"
import { useScrollReveal, useStaggerReveal } from "@/hooks/use-gsap-animations"
import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

const securityFeatures = [
  {
    icon: KeyRound,
    title: "OAuth2 Authentication",
    description: "Enterprise-grade security with industry-standard OAuth2 protocol. Your credentials are never stored.",
  },
  {
    icon: Calendar,
    title: "Google Calendar Integration",
    description: "Secure two-way sync with Google Calendar using official APIs. Read and write with proper scopes.",
  },
  {
    icon: Shield,
    title: "Secure by Design",
    description: "Built with security-first architecture. All data encrypted in transit and at rest.",
  },
  {
    icon: FileText,
    title: "Complete Agent Activity Logs",
    description: "Every action is timestamped and logged. Full transparency into what your agent does and when.",
  },
]

export function SecurityTrust() {
  const contentRef = useScrollReveal<HTMLDivElement>({ y: 50, duration: 0.8 })
  const featuresRef = useStaggerReveal<HTMLDivElement>({ 
    stagger: 0.1, 
    y: 30, 
    duration: 0.6,
    childSelector: ".security-feature"
  })
  const visualRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap.fromTo(
        visualRef.current,
        { opacity: 0, scale: 0.95, x: -30 },
        { 
          opacity: 1, 
          scale: 1,
          x: 0,
          duration: 0.8, 
          ease: "power3.out",
          scrollTrigger: {
            trigger: visualRef.current,
            start: "top 80%",
            toggleActions: "play none none reverse",
          }
        }
      )

      const statusItems = visualRef.current?.querySelectorAll(".status-item")
      if (statusItems) {
        gsap.fromTo(
          statusItems,
          { opacity: 0, x: -20 },
          { 
            opacity: 1, 
            x: 0,
            duration: 0.4, 
            stagger: 0.08,
            delay: 0.3,
            ease: "power2.out",
            scrollTrigger: {
              trigger: visualRef.current,
              start: "top 80%",
              toggleActions: "play none none reverse",
            }
          }
        )
      }
    })

    return () => ctx.revert()
  }, [])

  return (
    <section id="security" className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          {/* Left: Visual */}
          <div className="order-2 lg:order-1">
            <div ref={visualRef} className="rounded-2xl border border-border/60 bg-card p-6 lg:p-8 card-hover">
              <div className="flex items-center gap-3 mb-6 group cursor-pointer">
                <div className="w-10 h-10 rounded-xl bg-green-500/10 flex items-center justify-center transition-all duration-300 group-hover:scale-110">
                  <Shield className="w-5 h-5 text-green-600" />
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">Security Status</p>
                  <p className="text-xs text-green-600">All systems secure</p>
                </div>
              </div>

              <div className="space-y-4">
                <div className="status-item flex items-center justify-between py-3 border-b border-border/40 transition-all duration-300 hover:bg-muted/20 hover:px-2 rounded">
                  <span className="text-sm text-muted-foreground">OAuth Integration</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600 transition-transform duration-200 hover:scale-105">Connected</span>
                </div>
                <div className="status-item flex items-center justify-between py-3 border-b border-border/40 transition-all duration-300 hover:bg-muted/20 hover:px-2 rounded">
                  <span className="text-sm text-muted-foreground">Google Calendar</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600 transition-transform duration-200 hover:scale-105">Synced</span>
                </div>
                <div className="status-item flex items-center justify-between py-3 border-b border-border/40 transition-all duration-300 hover:bg-muted/20 hover:px-2 rounded">
                  <span className="text-sm text-muted-foreground">Data Encryption</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600 transition-transform duration-200 hover:scale-105">AES-256</span>
                </div>
                <div className="status-item flex items-center justify-between py-3 transition-all duration-300 hover:bg-muted/20 hover:px-2 rounded">
                  <span className="text-sm text-muted-foreground">Agent Activity Logging</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600 transition-transform duration-200 hover:scale-105">Enabled</span>
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-border/40">
                <p className="text-xs text-muted-foreground">
                  Last security audit: March 10, 2026
                </p>
              </div>
            </div>
          </div>

          {/* Right: Content */}
          <div ref={contentRef} className="order-1 lg:order-2">
            <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">
              Security & Trust
            </p>
            <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
              Enterprise-grade security
            </h2>
            <p className="text-muted-foreground text-lg mb-10">
              Your data security is our priority. CoAgent4U is built with the highest security standards.
            </p>

            <div ref={featuresRef} className="grid sm:grid-cols-2 gap-6">
              {securityFeatures.map((feature, index) => (
                <div 
                  key={feature.title} 
                  className="security-feature p-4 rounded-xl transition-all duration-300 hover:bg-card hover:shadow-md cursor-pointer"
                  style={{ transitionDelay: `${index * 50}ms` }}
                >
                  <div className="w-10 h-10 rounded-xl bg-muted flex items-center justify-center mb-3 transition-all duration-300 hover:scale-110 hover:bg-foreground/10">
                    <feature.icon className="w-5 h-5 text-foreground transition-colors duration-300" />
                  </div>
                  <h3 className="font-medium text-foreground mb-1">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
