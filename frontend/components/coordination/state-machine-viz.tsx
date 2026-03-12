"use client"

import { cn } from "@/lib/utils"

const states = [
  { id: "initiated", label: "Initiated" },
  { id: "querying_a", label: "Querying A" },
  { id: "querying_b", label: "Querying B" },
  { id: "matching", label: "Matching" },
  { id: "awaiting_b", label: "Awaiting B" },
  { id: "awaiting_a", label: "Awaiting A" },
  { id: "confirmed", label: "Confirmed" },
]

interface StateMachineVizProps {
  currentStateIndex?: number
  interactive?: boolean
}

export function StateMachineViz({ currentStateIndex = 0, interactive = true }: StateMachineVizProps) {
  return (
    <div className="relative">
      {/* Progress line */}
      <div className="absolute top-5 left-0 right-0 h-0.5 bg-charcoal-light">
        <div 
          className="h-full bg-accent transition-all duration-500"
          style={{ width: `${(currentStateIndex / (states.length - 1)) * 100}%` }}
        />
      </div>
      
      {/* State nodes */}
      <div className="relative flex justify-between">
        {states.map((state, index) => {
          const isCompleted = index < currentStateIndex
          const isCurrent = index === currentStateIndex
          const isPending = index > currentStateIndex
          
          return (
            <div key={state.id} className="flex flex-col items-center">
              {/* Node */}
              <div
                className={cn(
                  "w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all duration-300",
                  isCompleted && "bg-accent border-accent",
                  isCurrent && "bg-charcoal border-accent shadow-[0_0_20px_rgba(19,136,8,0.3)]",
                  isPending && "bg-charcoal border-charcoal-light"
                )}
              >
                {isCompleted ? (
                  <svg className="w-5 h-5 text-charcoal" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                ) : (
                  <span className={cn(
                    "text-xs font-mono",
                    isCurrent ? "text-accent" : "text-cream/40"
                  )}>
                    {index + 1}
                  </span>
                )}
              </div>
              
              {/* Label */}
              <span className={cn(
                "mt-3 text-xs font-medium text-center max-w-[80px]",
                isCompleted && "text-cream",
                isCurrent && "text-accent",
                isPending && "text-cream/40"
              )}>
                {state.label}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
