import { Zap, GitBranch, UserCheck, ShieldAlert, Users } from "lucide-react"

const benefits = [
  {
    icon: Zap,
    title: "Zero Scheduling Friction",
    description: "No more back-and-forth emails. Your agent handles all the coordination while you focus on work.",
  },
  {
    icon: GitBranch,
    title: "Deterministic Coordination",
    description: "Predictable outcomes based on explicit rules. No AI hallucinations or unexpected behaviors.",
  },
  {
    icon: UserCheck,
    title: "Human-in-the-Loop Control",
    description: "Every action requires your approval. Nothing happens on your calendar without consent.",
  },
  {
    icon: ShieldAlert,
    title: "Conflict Prevention",
    description: "Automatic detection of scheduling conflicts before they become problems.",
  },
  {
    icon: Users,
    title: "Agent Collaboration",
    description: "Agents work together seamlessly to find the perfect meeting time for everyone.",
  },
]

export function PlatformBenefits() {
  return (
    <section className="py-24 lg:py-32">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="text-sm font-medium text-primary mb-3">
            Platform Benefits
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            Built for how you actually work
          </h2>
          <p className="text-muted-foreground text-lg">
            CoAgent4U removes the friction from scheduling while keeping you in complete control.
          </p>
        </div>

        {/* Benefits grid */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {benefits.map((benefit, index) => (
            <div
              key={benefit.title}
              className={`${
                index === benefits.length - 1 && benefits.length % 3 !== 0
                  ? "sm:col-span-2 lg:col-span-1"
                  : ""
              }`}
            >
              <div className="w-12 h-12 rounded-2xl bg-muted flex items-center justify-center mb-5">
                <benefit.icon className="w-6 h-6 text-foreground" />
              </div>
              <h3 className="text-lg font-semibold text-foreground mb-2">
                {benefit.title}
              </h3>
              <p className="text-muted-foreground leading-relaxed">
                {benefit.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
