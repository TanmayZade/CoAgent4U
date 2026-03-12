import { Users, Bot, Calendar, CheckCircle } from "lucide-react"

const steps = [
  {
    number: "01",
    icon: Users,
    title: "Connect Your Calendars",
    description: "Link your Google Calendar and Slack. Your agent learns your availability preferences automatically.",
  },
  {
    number: "02",
    icon: Bot,
    title: "Agents Coordinate",
    description: "When scheduling is needed, your agent talks to the other party's agent. They find overlapping availability slots.",
  },
  {
    number: "03",
    icon: CheckCircle,
    title: "You Approve",
    description: "Both parties receive a proposal and must explicitly approve. Nothing happens without human consent.",
  },
  {
    number: "04",
    icon: Calendar,
    title: "Event Created",
    description: "Once approved, the event is created on both calendars. Full audit trail is maintained.",
  },
]

export function HowItWorksSection() {
  return (
    <section id="how-it-works" className="py-24 lg:py-32">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center mb-16">
          <p className="text-sm font-medium text-primary mb-3">
            How It Works
          </p>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-display font-bold text-foreground tracking-tight mb-4 text-balance">
            Make teamwork seamless
          </h2>
          <p className="text-muted-foreground max-w-lg mx-auto text-lg">
            Tools for your team to coordinate and iterate faster without the back-and-forth.
          </p>
        </div>

        {/* Steps */}
        <div className="relative">
          {/* Connection line */}
          <div className="hidden lg:block absolute top-24 left-0 right-0 h-px bg-border" />
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-6">
            {steps.map((step, index) => (
              <div key={step.number} className="relative">
                {/* Step card */}
                <div className="relative bg-card rounded-xl border border-border p-6 h-full">
                  {/* Number badge */}
                  <div className="absolute -top-4 left-6 bg-background px-3 py-1 rounded-full border border-border">
                    <span className="text-sm font-mono text-primary font-medium">{step.number}</span>
                  </div>
                  
                  {/* Icon */}
                  <div className="w-12 h-12 rounded-lg bg-primary/10 flex items-center justify-center mt-4 mb-4">
                    <step.icon className="w-6 h-6 text-primary" />
                  </div>
                  
                  {/* Content */}
                  <h3 className="text-lg font-semibold text-foreground mb-2 font-display">
                    {step.title}
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    {step.description}
                  </p>
                </div>

                {/* Arrow connector for mobile */}
                {index < steps.length - 1 && (
                  <div className="lg:hidden flex justify-center py-4">
                    <div className="w-px h-8 bg-border" />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Code/Demo Preview */}
        <div className="mt-20">
          <div className="rounded-xl border border-border bg-card overflow-hidden">
            <div className="flex items-center gap-2 px-4 py-3 bg-muted border-b border-border">
              <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-destructive/60" />
                <div className="w-3 h-3 rounded-full bg-yellow-500/60" />
                <div className="w-3 h-3 rounded-full bg-primary/60" />
              </div>
              <span className="text-xs text-muted-foreground font-mono ml-2">coordination-flow.tsx</span>
            </div>
            <div className="p-6 bg-background font-mono text-sm overflow-x-auto">
              <pre className="text-muted-foreground">
                <code>{`// Agent Coordination Flow
const coordination = await startCoordination({
  initiator: "alice@company.com",
  participant: "bob@partner.com",
  duration: 60, // minutes
  window: { days: 7 }
});

// Agents find mutual availability
const slots = await coordination.findSlots();
// → [{ start: "2025-03-15T10:00", end: "2025-03-15T11:00" }, ...]

// Both parties approve
await coordination.requestApproval(slots[0]);
// → { alice: "approved", bob: "approved" }

// Event created on both calendars
const event = await coordination.createEvent();
// → { id: "evt_abc123", status: "confirmed" }`}</code>
              </pre>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
