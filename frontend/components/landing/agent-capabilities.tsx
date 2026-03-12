"use client"

import { Calendar, ListChecks, AlertCircle, CheckSquare, Users, History } from "lucide-react"
import { useScrollReveal, useStaggerReveal } from "@/hooks/use-gsap-animations"

const capabilities = [
  {
    icon: Calendar,
    title: "View My Schedule",
    description: "Access your calendar instantly. Your agent retrieves commitments directly from Google Calendar.",
  },
  {
    icon: ListChecks,
    title: "Manage Commitments",
    description: "Create and organize events, meetings, and time blocks through natural language commands.",
  },
  {
    icon: AlertCircle,
    title: "Detect Conflicts",
    description: "Automatic conflict detection across all your commitments. Never double-book or miss overlaps.",
  },
  {
    icon: Users,
    title: "Coordinate With Other Agents",
    description: "Your agent communicates with other users' agents to find common availability automatically.",
  },
  {
    icon: CheckSquare,
    title: "Request Human Approval",
    description: "Every coordination proposal requires your explicit approval before any action is taken.",
  },
  {
    icon: History,
    title: "Maintain History",
    description: "Full coordination history and audit logs. Track every agent interaction and decision.",
  },
]

export function AgentCapabilities() {
  const headerRef = useScrollReveal<HTMLDivElement>({ y: 60, duration: 1 })
  const cardsRef = useStaggerReveal<HTMLDivElement>({ 
    stagger: 0.12, 
    y: 50, 
    duration: 0.9,
    childSelector: "> div"
  })

  return (
    <section id="capabilities" className="py-32 lg:py-40">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div ref={headerRef} className="max-w-3xl mb-20">
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-6">
            Capabilities
          </p>
          <h2 className="text-4xl sm:text-5xl lg:text-6xl font-semibold tracking-tight text-foreground mb-8 leading-[1.1]">
            Your agent, your rules
          </h2>
          <p className="text-xl lg:text-2xl text-muted-foreground leading-relaxed">
            Each user receives a personal agent that can perform these core actions on your behalf.
          </p>
        </div>

        {/* Capability cards */}
        <div ref={cardsRef} className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {capabilities.map((capability, index) => (
            <div
              key={capability.title}
              className="group p-8 rounded-3xl border border-border/40 bg-card hover:border-foreground/20 card-hover"
            >
              <div className="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-6 group-hover:bg-foreground/10 group-hover:scale-110 transition-all duration-500">
                <capability.icon className="w-7 h-7 text-foreground/70 group-hover:text-foreground transition-colors duration-300" />
              </div>
              <h3 className="text-xl font-semibold text-foreground mb-4">
                {capability.title}
              </h3>
              <p className="text-base text-muted-foreground leading-relaxed">
                {capability.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
