import { Bot, Users, CheckCircle } from "lucide-react"

export function ProductExplanation() {
  return (
    <section className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-6">
            Personal Agent Coordination Infrastructure
          </h2>
          <p className="text-lg lg:text-xl text-muted-foreground leading-relaxed">
            CoAgent4U is a coordination layer where personal agents represent users and collaborate to manage commitments across people and tools. Instead of humans coordinating manually, agents coordinate on behalf of users.
          </p>
        </div>

        {/* Three pillars */}
        <div className="mt-16 grid sm:grid-cols-3 gap-8 lg:gap-12">
          <div className="text-center">
            <div className="w-14 h-14 rounded-2xl bg-primary/5 border border-primary/10 flex items-center justify-center mx-auto mb-5">
              <Bot className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              Agent Coordination
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              Personal agents understand your commitments, analyze availability, and coordinate with other agents to manage your time.
            </p>
          </div>

          <div className="text-center">
            <div className="w-14 h-14 rounded-2xl bg-primary/5 border border-primary/10 flex items-center justify-center mx-auto mb-5">
              <Users className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              Commitment Management
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              Manage scheduling, events, time conflicts, work sessions, team availability, and time windows through your personal agent.
            </p>
          </div>

          <div className="text-center">
            <div className="w-14 h-14 rounded-2xl bg-primary/5 border border-primary/10 flex items-center justify-center mx-auto mb-5">
              <CheckCircle className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">
              Human-in-the-Loop Approvals
            </h3>
            <p className="text-muted-foreground text-sm leading-relaxed">
              Users remain in control through approvals. Personal agents propose actions, humans approve before execution.
            </p>
          </div>
        </div>
      </div>
    </section>
  )
}
