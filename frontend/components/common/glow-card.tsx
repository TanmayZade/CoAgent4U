"use client"

import { cn } from "@/lib/utils"
import { HTMLAttributes, ReactNode } from "react"

interface GlowCardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  className?: string
  glowColor?: "accent" | "cyan" | "violet" | "emerald"
}

export function GlowCard({ children, className, glowColor = "accent", ...rest }: GlowCardProps) {
  return (
    <div
      className={cn(
        "relative rounded-lg border border-charcoal-light bg-charcoal p-6 transition-all duration-300",
        "hover:border-accent/30",
        className
      )}
      {...rest}
    >
      {children}
    </div>
  )
}
