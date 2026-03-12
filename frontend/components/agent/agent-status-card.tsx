"use client"

import { useEffect, useRef } from "react"
import gsap from "gsap"
import { Activity, Calendar, CheckCircle, Clock } from "lucide-react"

const recentActivity = [
  { action: "Calendar synced", time: "2 min ago" },
  { action: "Meeting scheduled with @sarah", time: "15 min ago" },
  { action: "Approval received from @mike", time: "1 hour ago" },
]

export function AgentStatusCard() {
  const cardRef = useRef<HTMLDivElement>(null)
  const countersRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!cardRef.current) return

    const ctx = gsap.context(() => {
      gsap.fromTo(
        cardRef.current,
        { y: 20, opacity: 0 },
        { y: 0, opacity: 1, duration: 0.6, ease: "power3.out" }
      )

      // Animate counters
      if (countersRef.current) {
        const counters = countersRef.current.querySelectorAll(".counter-value")
        counters.forEach((counter, index) => {
          const target = parseInt(counter.getAttribute("data-value") || "0")
          const obj = { value: 0 }
          gsap.to(obj, {
            value: target,
            duration: 1.5,
            delay: index * 0.2 + 0.3,
            ease: "power2.out",
            onUpdate: () => {
              counter.textContent = Math.round(obj.value).toString()
            },
          })
        })
      }
    }, cardRef)

    return () => ctx.revert()
  }, [])

  return (
    <div
      ref={cardRef}
      className="p-6 bg-charcoal-light rounded-xl border border-border card-lift"
    >
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
        {/* Status */}
        <div className="flex items-center gap-4">
          <div className="relative">
            <div className="w-14 h-14 rounded-full bg-accent/10 flex items-center justify-center">
              <Activity className="w-7 h-7 text-accent" />
            </div>
            <div className="absolute inset-0 rounded-full border border-accent/30 status-pulse" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-cream font-[family-name:var(--font-display)]">
              Agent Online
            </h2>
            <p className="text-sm text-foreground-secondary">
              Monitoring your calendar and Slack
            </p>
          </div>
        </div>

        {/* Metrics */}
        <div ref={countersRef} className="flex items-center gap-8">
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 text-2xl font-bold text-accent">
              <Calendar className="w-5 h-5" />
              <span className="counter-value tabular-nums" data-value="3">0</span>
            </div>
            <p className="text-xs text-foreground-muted">Coordinations</p>
          </div>
          <div className="w-px h-10 bg-border" />
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 text-2xl font-bold text-cream">
              <Clock className="w-5 h-5" />
              <span className="counter-value tabular-nums" data-value="1">0</span>
            </div>
            <p className="text-xs text-foreground-muted">Pending</p>
          </div>
          <div className="w-px h-10 bg-border" />
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 text-2xl font-bold text-cream">
              <CheckCircle className="w-5 h-5" />
              <span className="counter-value tabular-nums" data-value="5">0</span>
            </div>
            <p className="text-xs text-foreground-muted">Created</p>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="lg:w-56 bg-charcoal rounded-lg p-4 border border-border">
          <h3 className="text-xs font-mono text-foreground-muted uppercase tracking-wider mb-3">
            Recent
          </h3>
          <div className="space-y-2">
            {recentActivity.map((item, i) => (
              <div
                key={i}
                className="flex items-center justify-between text-xs"
              >
                <span className="text-foreground-secondary truncate">
                  {item.action}
                </span>
                <span className="text-foreground-muted font-mono ml-2 flex-shrink-0">
                  {item.time}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
