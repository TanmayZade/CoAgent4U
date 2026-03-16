"use client"

import { GlowCard } from "@/components/common/glow-card"
import { cn } from "@/lib/utils"
import { ChevronDown, ChevronRight } from "lucide-react"
import { useState } from "react"

interface AgentActivityEvent {
  timestamp: string
  description: string
  type: "INFO" | "SUCCESS" | "WARNING" | "ERROR"
}

interface AgentActivityTimelineProps {
  events: AgentActivityEvent[]
  className?: string
}

const typeColors = {
  INFO: "text-accent",
  SUCCESS: "text-emerald-500",
  WARNING: "text-amber-500",
  ERROR: "text-red-500",
}

const typeDotColors = {
  INFO: "bg-accent",
  SUCCESS: "bg-emerald-500",
  WARNING: "bg-amber-500",
  ERROR: "bg-red-500",
}

export function AgentActivityTimeline({ events, className }: AgentActivityTimelineProps) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null)

  return (
    <GlowCard className={cn("p-6", className)}>
      <h3 className="text-lg font-semibold text-cream mb-4 font-[family-name:var(--font-display)]">
        AgentActivity Timeline
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
              </div>
            </div>
          ))}
        </div>
      </div>
    </GlowCard>
  )
}
