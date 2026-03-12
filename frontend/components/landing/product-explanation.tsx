import { Bot, Users, CheckCircle } from "lucide-react"

export function ProductExplanation() {
  return (
    <section className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-6">
            A new way to coordinate
          </h2>
          <p className="text-lg lg:text-xl text-muted-foreground leading-relaxed">
            CoAgent4U assigns every user a personal agent that manages their schedule and coordinates with other agents. Instead of humans negotiating meeting times manually, agents coordinate deterministically.
          </p>
        </div>

        {/* Three pillars */}
        <div className="mt-16 grid sm:grid-cols-3 gap-8 lg:gap-12">
          <div className="text-center">
            <div className="w-14 h-14 rounded-2xl bg-primary/5 border border-primary/10 flex items-center justify-center mx-auto mb-5">
              <Bot className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              Deterministic Coordination
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              No AI guesswork. Your agent follows explicit rules to find the best meeting times with predictable outcomes.
            </p>
          </div>

          <div className="text-center">
            <div className="w-14 h-14 rounded-2xl bg-primary/5 border border-primary/10 flex items-center justify-center mx-auto mb-5">
              <CheckCircle className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              Human-in-the-Loop
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              Nothing happens without your approval. You maintain full control over every action your agent takes.
            </p>
          </div>

          <div className="text-center">
            <div className="w-14 h-14 rounded-2xl bg-primary/5 border border-primary/10 flex items-center justify-center mx-auto mb-5">
              <Users className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              Schedule Intelligence
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              Your agent understands your calendar, detects conflicts, and proposes optimal times automatically.
            </p>
          </div>
        </div>
      </div>
    </section>
  )
}
