import { Calendar, ListChecks, AlertCircle, CheckSquare, Users, History } from "lucide-react"

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
