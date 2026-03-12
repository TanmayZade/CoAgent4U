import { Calendar, Plus, AlertCircle, CheckSquare, Users, Clock } from "lucide-react"

const capabilities = [
  {
    icon: Calendar,
    title: "View My Schedule",
    description: "Access your calendar instantly. Your agent retrieves events directly from Google Calendar.",
  },
  {
    icon: Plus,
    title: "Add Events",
    description: "Create calendar events through natural commands. Your agent handles parsing and scheduling.",
  },
  {
    icon: AlertCircle,
    title: "Detect Conflicts",
    description: "Automatic conflict detection before any event is created. Never double-book again.",
  },
  {
    icon: CheckSquare,
    title: "Approve Events",
    description: "Review and approve every action. Your agent presents proposals, you make the final call.",
  },
  {
    icon: Users,
    title: "Coordinate with Others",
    description: "Your agent talks to other agents to find common availability automatically.",
  },
  {
    icon: Clock,
    title: "Maintain History",
    description: "Full coordination history and audit logs. Track every interaction and decision.",
  },
]

export function AgentCapabilities() {
  return (
    <section id="capabilities" className="py-24 lg:py-32">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div className="max-w-2xl mb-16">
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
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {capabilities.map((capability) => (
            <div
              key={capability.title}
              className="group p-6 rounded-2xl border border-border/60 bg-card hover:border-border hover:shadow-lg hover:shadow-black/[0.02] transition-all duration-300"
            >
              <div className="w-11 h-11 rounded-xl bg-muted flex items-center justify-center mb-4 group-hover:bg-primary/5 transition-colors">
                <capability.icon className="w-5 h-5 text-muted-foreground group-hover:text-primary transition-colors" />
              </div>
              <h3 className="text-base font-semibold text-foreground mb-2">
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
