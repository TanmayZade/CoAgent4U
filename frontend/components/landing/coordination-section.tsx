import { ArrowRight, Bot, CheckCircle2, User } from "lucide-react"

export function CoordinationSection() {
  return (
    <section id="how-it-works" className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="text-sm font-medium text-primary mb-3">
            Agent Coordination Visualization
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            Agents coordinate so you don't have to
          </h2>
          <p className="text-muted-foreground text-lg">
            Personal agents collaborate through a coordination network, eliminating manual back-and-forth communication.
          </p>
        </div>

        {/* Coordination flow visual */}
        <div className="max-w-4xl mx-auto">
          <div className="rounded-2xl border border-border/60 bg-card p-6 lg:p-10">
            {/* Mental model text */}
            <p className="text-center text-sm text-muted-foreground mb-8">
              Human → Personal Agent → Agent Coordination Network → Coordinated Outcome
            </p>

            {/* Flow diagram */}
            <div className="flex flex-col lg:flex-row items-center justify-between gap-6 lg:gap-4">
              {/* User A */}
              <div className="flex flex-col items-center text-center">
                <div className="w-14 h-14 rounded-full bg-primary/10 flex items-center justify-center mb-3">
                  <User className="w-7 h-7 text-primary" />
                </div>
                <span className="text-sm font-medium text-foreground">User A</span>
                <span className="text-xs text-muted-foreground">Invokes agent</span>
              </div>

              <ArrowRight className="w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0" />

              {/* Agent A */}
              <div className="flex flex-col items-center text-center">
                <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mb-3 border border-border">
                  <Bot className="w-7 h-7 text-foreground" />
                </div>
                <span className="text-sm font-medium text-foreground">Agent A</span>
                <span className="text-xs text-muted-foreground">Parses intent</span>
              </div>

              <ArrowRight className="w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0" />

              {/* Coordination Engine */}
              <div className="flex flex-col items-center text-center">
                <div className="w-16 h-16 rounded-2xl bg-foreground flex items-center justify-center mb-3">
                  <span className="text-background font-bold text-lg">CE</span>
                </div>
                <span className="text-sm font-medium text-foreground">Coordination</span>
                <span className="text-xs text-muted-foreground">Engine</span>
              </div>

              <ArrowRight className="w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0" />

              {/* Agent B */}
              <div className="flex flex-col items-center text-center">
                <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mb-3 border border-border">
                  <Bot className="w-7 h-7 text-foreground" />
                </div>
                <span className="text-sm font-medium text-foreground">Agent B</span>
                <span className="text-xs text-muted-foreground">Checks availability</span>
              </div>

              <ArrowRight className="w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0" />

              {/* User B */}
              <div className="flex flex-col items-center text-center">
                <div className="w-14 h-14 rounded-full bg-green-500/10 flex items-center justify-center mb-3">
                  <CheckCircle2 className="w-7 h-7 text-green-600" />
                </div>
                <span className="text-sm font-medium text-foreground">User B</span>
                <span className="text-xs text-muted-foreground">Approves first</span>
              </div>
            </div>

            {/* Description */}
            <div className="mt-10 pt-8 border-t border-border/60">
              <div className="grid sm:grid-cols-3 gap-6">
                <div>
                  <p className="text-sm font-medium text-foreground mb-1">Step 1: Intent & Availability</p>
                  <p className="text-sm text-muted-foreground">
                    Agent A parses the request and checks User A's calendar for available time slots.
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground mb-1">Step 2: Deterministic Matching</p>
                  <p className="text-sm text-muted-foreground">
                    Coordination Engine queries Agent B and performs deterministic matching to find common availability.
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground mb-1">Step 3: Human Approval</p>
                  <p className="text-sm text-muted-foreground">
                    User B approves slot proposals first, then User A confirms. Events created in both calendars.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
