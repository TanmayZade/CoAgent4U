import { 
  Bot, 
  Eye, 
  Shield,
  Calendar,
  MessageSquare,
  GitBranch,
  CheckSquare,
  FileText,
  KeyRound,
} from "lucide-react"

const differentiators = [
  {
    icon: Bot,
    title: "Deterministic, Not AI",
    description: "Rule-based orchestration means predictable outcomes. Every decision follows explicit logic.",
  },
  {
    icon: Eye,
    title: "Human-in-the-Loop",
    description: "Nothing happens without your approval. Both parties must explicitly accept before any action.",
  },
  {
    icon: Shield,
    title: "GDPR by Design",
    description: "Your data stays yours. Minimal retention, full audit trails, complete data sovereignty.",
  },
]

const features = [
  {
    icon: Calendar,
    title: "Google Calendar",
    description: "Two-way sync for real-time availability.",
  },
  {
    icon: MessageSquare,
    title: "Slack Native",
    description: "Schedule without switching contexts.",
  },
  {
    icon: GitBranch,
    title: "Deterministic Flow",
    description: "Predictable state machine orchestration.",
  },
  {
    icon: CheckSquare,
    title: "Dual Approval",
    description: "Both parties approve before action.",
  },
  {
    icon: FileText,
    title: "Audit Trail",
    description: "Every action timestamped and logged.",
  },
  {
    icon: KeyRound,
    title: "OAuth2 Security",
    description: "Enterprise-grade authentication.",
  },
]

export function FeaturesSection() {
  return (
    <section id="features" className="relative py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center mb-16">
          <p className="text-sm font-medium text-primary mb-3">
            What Makes It Different
          </p>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-display font-bold text-foreground tracking-tight mb-4 text-balance">
            Not another chatbot
          </h2>
          <p className="text-muted-foreground max-w-lg mx-auto text-lg">
            A deterministic coordination engine built for trust and transparency.
          </p>
        </div>

        {/* Differentiator cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-24">
          {differentiators.map((item) => (
            <div
              key={item.title}
              className="group p-8 bg-card rounded-xl border border-border hover:border-primary/30 transition-all duration-300"
            >
              <div className="w-12 h-12 rounded-lg bg-primary/10 flex items-center justify-center mb-6 group-hover:bg-primary/20 transition-colors duration-300">
                <item.icon className="w-6 h-6 text-primary" />
              </div>
              <h3 className="text-xl font-semibold text-foreground mb-3 font-display">
                {item.title}
              </h3>
              <p className="text-muted-foreground leading-relaxed">
                {item.description}
              </p>
            </div>
          ))}
        </div>

        {/* Feature grid section */}
        <div className="text-center mb-12">
          <p className="text-sm font-medium text-primary mb-3">
            Capabilities
          </p>
          <h2 className="text-3xl sm:text-4xl font-display font-bold text-foreground tracking-tight">
            Built for modern workflows
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {features.map((feature) => (
            <div
              key={feature.title}
              className="group p-6 bg-card/50 rounded-lg border border-border/50 hover:border-border hover:bg-card transition-all duration-300"
            >
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-lg bg-muted flex items-center justify-center flex-shrink-0 group-hover:bg-primary/10 transition-colors duration-300">
                  <feature.icon className="w-5 h-5 text-muted-foreground group-hover:text-primary transition-colors duration-300" />
                </div>
                <div>
                  <h3 className="font-medium text-foreground mb-1">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
