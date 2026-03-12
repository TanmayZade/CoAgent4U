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
  const headerRef = useScrollReveal<HTMLDivElement>({ y: 40, duration: 0.8 })
  const cardsRef = useStaggerReveal<HTMLDivElement>({ 
    stagger: 0.1, 
    y: 40, 
    duration: 0.7,
    childSelector: "> div"
  })

  return (
    <section id="capabilities" className="py-24 lg:py-32">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div ref={headerRef} className="max-w-2xl mb-16">
          <p className="text-sm font-medium text-primary mb-3">
            Personal Agent Capabilities
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            Your agent, your rules
          </h2>
          <p className="text-muted-foreground text-lg">
            Each user receives a personal agent that can perform these core actions on your behalf.
          </p>
        </div>

        {/* Capability cards */}
        <div ref={cardsRef} className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {capabilities.map((capability, index) => (
            <div
              key={capability.title}
              className="group p-6 rounded-2xl border border-border/60 bg-card hover:border-border transition-all duration-500 card-hover"
              style={{ transitionDelay: `${index * 50}ms` }}
            >
              <div className="w-11 h-11 rounded-xl bg-muted flex items-center justify-center mb-4 group-hover:bg-primary/10 group-hover:scale-110 transition-all duration-300">
                <capability.icon className="w-5 h-5 text-muted-foreground group-hover:text-primary transition-colors duration-300" />
              </div>
              <h3 className="text-base font-semibold text-foreground mb-2 group-hover:text-primary transition-colors duration-300">
                {capability.title}
              </h3>
              <p className="text-sm text-muted-foreground leading-relaxed">
                {capability.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
