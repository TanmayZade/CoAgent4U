import { Shield, UserCheck, Users, Lock } from "lucide-react"

const rules = [
  {
    icon: Shield,
    title: "Agents Propose, Humans Approve",
    description: "Personal agents propose actions based on coordination. No action is ever executed without explicit human approval.",
  },
  {
    icon: UserCheck,
    title: "Invitee Approves First",
    description: "The invited user always approves first. They select from proposed slot options before the requester is asked to confirm.",
  },
  {
    icon: Users,
    title: "Requester Confirms Second",
    description: "After the invitee approves a slot, the requester receives an approval request to confirm the coordination.",
  },
  {
    icon: Lock,
    title: "Full Control Always",
    description: "Users can decline, reschedule, or cancel at any point in the flow. Your calendar, your rules.",
  },
]

export function ApprovalSystem() {
  return (
    <section className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          {/* Left: Content */}
          <div>
            <p className="text-sm font-medium text-primary mb-3">
              Human Approval System
            </p>
            <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
              You're always in control
            </h2>
            <p className="text-muted-foreground text-lg mb-10">
              CoAgent4U is designed with governance at its core. Personal agents propose actions, but humans approve before any execution occurs.
            </p>

            <div className="space-y-6">
              {rules.map((rule) => (
                <div key={rule.title} className="flex gap-4">
                  <div className="w-10 h-10 rounded-xl bg-card border border-border/60 flex items-center justify-center flex-shrink-0">
                    <rule.icon className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-medium text-foreground mb-1">{rule.title}</h3>
                    <p className="text-sm text-muted-foreground">{rule.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right: Visual */}
          <div className="rounded-2xl border border-border/60 bg-card p-6 lg:p-8">
            <h3 className="text-sm font-medium text-foreground mb-6">Approval Flow</h3>
            
            <div className="space-y-4">
              {/* Step 1 */}
              <div className="flex items-start gap-4">
                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-semibold text-primary">1</span>
                </div>
                <div className="flex-1 pt-1">
                  <p className="text-sm text-foreground font-medium">Coordination Initiated</p>
                  <p className="text-xs text-muted-foreground mt-0.5">Agent A parses intent, finds User A availability</p>
                  <div className="mt-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
                    User A free: 6:00 PM - 9:00 PM
                  </div>
                </div>
              </div>

              {/* Connector */}
              <div className="ml-4 w-px h-4 bg-border"></div>

              {/* Step 2 */}
              <div className="flex items-start gap-4">
                <div className="w-8 h-8 rounded-full bg-blue-500/10 flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-semibold text-blue-600">2</span>
                </div>
                <div className="flex-1 pt-1">
                  <p className="text-sm text-foreground font-medium">Agent-to-Agent Coordination</p>
                  <p className="text-xs text-muted-foreground mt-0.5">Coordination Engine matches availability</p>
                  <div className="mt-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
                    Common slot: 6:00 PM - 7:00 PM
                  </div>
                </div>
              </div>

              {/* Connector */}
              <div className="ml-4 w-px h-4 bg-border"></div>

              {/* Step 3 */}
              <div className="flex items-start gap-4">
                <div className="w-8 h-8 rounded-full bg-yellow-500/10 flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-semibold text-yellow-600">3</span>
                </div>
                <div className="flex-1 pt-1">
                  <p className="text-sm text-foreground font-medium">Invitee Selects & Approves</p>
                  <p className="text-xs text-muted-foreground mt-0.5">User B selects preferred slot from options</p>
                  <div className="mt-2 flex gap-2">
                    <button className="px-3 py-1.5 text-xs font-medium bg-foreground text-background rounded-md">
                      Approve
                    </button>
                    <button className="px-3 py-1.5 text-xs font-medium border border-border rounded-md">
                      Decline
                    </button>
                  </div>
                </div>
              </div>

              {/* Connector */}
              <div className="ml-4 w-px h-4 bg-border"></div>

              {/* Step 4 */}
              <div className="flex items-start gap-4">
                <div className="w-8 h-8 rounded-full bg-orange-500/10 flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-semibold text-orange-600">4</span>
                </div>
                <div className="flex-1 pt-1">
                  <p className="text-sm text-foreground font-medium">Requester Confirms</p>
                  <p className="text-xs text-muted-foreground mt-0.5">User A confirms the approved time</p>
                  <div className="mt-2 flex gap-2">
                    <button className="px-3 py-1.5 text-xs font-medium bg-foreground text-background rounded-md">
                      Confirm
                    </button>
                    <button className="px-3 py-1.5 text-xs font-medium border border-border rounded-md">
                      Cancel
                    </button>
                  </div>
                </div>
              </div>

              {/* Connector */}
              <div className="ml-4 w-px h-4 bg-border"></div>

              {/* Step 5 */}
              <div className="flex items-start gap-4">
                <div className="w-8 h-8 rounded-full bg-green-500/10 flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-semibold text-green-600">5</span>
                </div>
                <div className="flex-1 pt-1">
                  <p className="text-sm text-foreground font-medium">Events Created</p>
                  <p className="text-xs text-muted-foreground mt-0.5">Both calendars updated, confirmations sent</p>
                  <div className="mt-2 rounded-lg bg-green-500/5 border border-green-500/20 p-3 text-xs text-green-700">
                    Meeting scheduled: Friday 6:00 PM
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
