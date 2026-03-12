"use client"

import { cn } from "@/lib/utils"
import { ReactNode } from "react"

interface GlowCardProps {
  children: ReactNode
  className?: string
  glowColor?: "accent" | "cyan" | "violet" | "emerald"
}

export function GlowCard({ children, className, glowColor = "accent" }: GlowCardProps) {
  return (
    <div
      className={cn(
        "relative rounded-lg border border-charcoal-light bg-charcoal p-6 transition-all duration-300",
        "hover:border-accent/30",
        className
      )}
    >
      {children}
    </div>
  )
}
