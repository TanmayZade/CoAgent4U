"use client"

import { useEffect, useRef } from "react"
import gsap from "gsap"
import { CalendarPlus, Users, Clock, List } from "lucide-react"

const actions = [
  {
    icon: CalendarPlus,
    label: "Add Calendar Event",
    description: "Create a new event",
  },
  {
    icon: Users,
    label: "Schedule Meeting",
    description: "Coordinate with others",
  },
  {
    icon: Clock,
    label: "Check Availability",
    description: "View free slots",
  },
  {
    icon: List,
    label: "View Schedule",
    description: "See your calendar",
  },
]

export function QuickActions() {
  const gridRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!gridRef.current) return

    const ctx = gsap.context(() => {
      gsap.fromTo(
        gridRef.current?.children || [],
        { y: 20, opacity: 0 },
        {
          y: 0,
          opacity: 1,
          duration: 0.5,
          stagger: 0.08,
          ease: "power3.out",
          delay: 0.1,
        }
      )
    }, gridRef)

    return () => ctx.revert()
  }, [])

  return (
    <div ref={gridRef} className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {actions.map((action) => (
        <div
          key={action.label}
          className="p-4 bg-charcoal-light border border-border rounded-xl cursor-pointer group hover:border-accent/30 transition-all duration-300 card-lift"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-charcoal flex items-center justify-center group-hover:bg-accent/10 transition-colors duration-300">
              <action.icon className="w-5 h-5 text-foreground-secondary group-hover:text-accent transition-colors duration-300" />
            </div>
            <div>
              <p className="text-sm font-medium text-cream">{action.label}</p>
              <p className="text-xs text-foreground-muted">{action.description}</p>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
