"use client"

import { GlowCard } from "@/components/common/glow-card"
import { cn } from "@/lib/utils"
import { ChevronDown, ChevronRight } from "lucide-react"
import { useState } from "react"

interface AuditEvent {
  timestamp: string
  description: string
  type: "info" | "success" | "warning" | "error"
  payload?: Record<string, unknown>
}

interface AuditTimelineProps {
  events: AuditEvent[]
  className?: string
}

const typeColors = {
  info: "text-accent",
  success: "text-emerald-500",
  warning: "text-amber-500",
  error: "text-red-500",
}

const typeDotColors = {
  info: "bg-accent",
  success: "bg-emerald-500",
  warning: "bg-amber-500",
  error: "bg-red-500",
}

export function AuditTimeline({ events, className }: AuditTimelineProps) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null)

  return (
    <GlowCard className={cn("p-6", className)}>
      <h3 className="text-lg font-semibold text-cream mb-4 font-[family-name:var(--font-display)]">
        Audit Timeline
      </h3>
      
      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-[5px] top-2 bottom-2 w-px bg-charcoal-light" />
        
        <div className="space-y-4">
          {events.map((event, index) => (
            <div
              key={index}
              className="relative pl-6 animate-[fadeInUp_0.4s_ease-out_forwards]"
              style={{ animationDelay: `${index * 50}ms`, opacity: 0 }}
            >
              {/* Dot */}
              <div
                className={cn(
                  "absolute left-0 top-1.5 w-[11px] h-[11px] rounded-full border-2 border-charcoal",
                  typeDotColors[event.type]
                )}
              />
              
              {/* Content */}
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs font-mono text-cream/50">
                    {event.timestamp}
                  </span>
                  <span className={cn("text-sm", typeColors[event.type])}>
                    {event.description}
                  </span>
                </div>
                
                {event.payload && (
                  <button
                    onClick={() =>
                      setExpandedIndex(expandedIndex === index ? null : index)
                    }
                    className="flex items-center gap-1 text-xs text-cream/50 hover:text-cream transition-colors"
                  >
                    {expandedIndex === index ? (
                      <ChevronDown className="w-3 h-3" />
                    ) : (
                      <ChevronRight className="w-3 h-3" />
                    )}
                    View payload
                  </button>
                )}
                
                {expandedIndex === index && event.payload && (
                  <pre className="mt-2 p-3 bg-charcoal-light rounded-lg border border-charcoal-lighter text-xs font-mono text-cream/70 overflow-x-auto">
                    {JSON.stringify(event.payload, null, 2)}
                  </pre>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </GlowCard>
  )
}
