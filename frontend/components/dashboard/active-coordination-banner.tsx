"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { RefreshCw, ChevronRight, Clock } from "lucide-react"

interface ActiveCoordinationBannerProps {
  coordinationId?: string
  participant?: string
  state?: string
  startedAgo?: string
}

export function ActiveCoordinationBanner({
  coordinationId = "coord-002",
  participant = "@mike-wilson",
  state = "AWAITING_APPROVAL_B",
  startedAgo = "4 minutes ago",
}: ActiveCoordinationBannerProps) {
  return (
    <div className="relative overflow-hidden rounded-xl border border-accent/30 bg-accent/5">
      {/* Shimmer effect */}
      <div className="absolute inset-0 overflow-hidden">
        <div
          className="absolute inset-0 -translate-x-full"
          style={{
            background: "linear-gradient(90deg, transparent 0%, rgba(232,90,44,0.1) 50%, transparent 100%)",
            animation: "shimmer 2.5s infinite",
          }}
        />
      </div>

      <div className="relative p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="relative">
            <div className="w-10 h-10 rounded-full bg-accent/20 flex items-center justify-center">
              <RefreshCw className="w-5 h-5 text-accent animate-spin" style={{ animationDuration: "3s" }} />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-cream">
                Active Coordination: Meeting with{" "}
                <span className="text-accent">{participant}</span>
              </span>
            </div>
            <div className="flex items-center gap-3 text-xs text-foreground-muted">
              <span className="font-mono text-accent">{state}</span>
              <span className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                Started {startedAgo}
              </span>
            </div>
          </div>
        </div>

        {/* Progress indicator */}
        <div className="flex items-center gap-4">
          <div className="hidden md:flex items-center gap-1">
            {[1, 2, 3, 4, 5, 6, 7].map((step) => (
              <div
                key={step}
                className={`w-6 h-1.5 rounded-full transition-colors ${
                  step <= 3
                    ? "bg-accent"
                    : step === 4
                    ? "bg-accent/50"
                    : "bg-border"
                }`}
              />
            ))}
          </div>
          <Button
            asChild
            size="sm"
            className="bg-accent hover:bg-accent-dark text-cream font-medium rounded-full"
          >
            <Link href={`/dashboard/coordinations/${coordinationId}`}>
              View Details
              <ChevronRight className="w-4 h-4 ml-1" />
            </Link>
          </Button>
        </div>
      </div>

      <style jsx>{`
        @keyframes shimmer {
          0% {
            transform: translateX(-100%);
          }
          100% {
            transform: translateX(200%);
          }
        }
      `}</style>
    </div>
  )
}
