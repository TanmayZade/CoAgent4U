import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"

export function HeroSection() {
  return (
    <section className="relative pt-32 pb-20 lg:pt-40 lg:pb-32">
      {/* Background gradient */}
      <div className="absolute inset-0 -z-10 overflow-hidden">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[600px] bg-primary/5 rounded-full blur-3xl" />
      </div>

      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mx-auto max-w-3xl text-center">
          {/* Eyebrow */}
          <div className="mb-8 inline-flex items-center gap-2 rounded-full border border-border bg-card px-4 py-1.5">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
            </span>
            <span className="text-sm text-muted-foreground">
              Now in Public Beta
            </span>
          </div>

          {/* Headline */}
          <h1 className="text-4xl font-display font-bold tracking-tight text-foreground sm:text-5xl lg:text-6xl text-balance">
            The complete platform for{" "}
            <span className="text-primary">agent coordination</span>
          </h1>

          {/* Subheadline */}
          <p className="mt-6 text-lg leading-relaxed text-muted-foreground max-w-2xl mx-auto text-pretty">
            Two agents. One goal. Zero back-and-forth. CoAgent4U orchestrates deterministic 
            coordination between personal AI agents so meetings happen without the noise.
          </p>

          {/* CTAs */}
          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button size="lg" asChild>
              <Link href="/signin">
                Get Started
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            <Button variant="outline" size="lg" asChild>
              <Link href="#how-it-works">
                See How It Works
              </Link>
            </Button>
          </div>
        </div>

        {/* Hero Visual */}
        <div className="mt-16 lg:mt-24">
          <div className="relative mx-auto max-w-5xl">
            {/* Browser mockup */}
            <div className="rounded-xl border border-border bg-card overflow-hidden shadow-2xl">
              {/* Browser bar */}
              <div className="flex items-center gap-2 px-4 py-3 bg-muted border-b border-border">
                <div className="flex gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-destructive/60" />
                  <div className="w-3 h-3 rounded-full bg-yellow-500/60" />
                  <div className="w-3 h-3 rounded-full bg-primary/60" />
                </div>
                <div className="flex-1 mx-4">
                  <div className="bg-background rounded-md px-3 py-1.5 text-xs text-muted-foreground font-mono">
                    app.coagent4u.com/dashboard
                  </div>
                </div>
              </div>
              
              {/* Dashboard preview */}
              <div className="p-6 bg-background">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {/* Stats cards */}
                  <div className="rounded-lg border border-border bg-card p-4">
                    <p className="text-sm text-muted-foreground">Active Coordinations</p>
                    <p className="text-2xl font-bold text-foreground mt-1">12</p>
                    <div className="mt-2 text-xs text-primary">+3 this week</div>
                  </div>
                  <div className="rounded-lg border border-border bg-card p-4">
                    <p className="text-sm text-muted-foreground">Meetings Scheduled</p>
                    <p className="text-2xl font-bold text-foreground mt-1">47</p>
                    <div className="mt-2 text-xs text-muted-foreground">This month</div>
                  </div>
                  <div className="rounded-lg border border-border bg-card p-4">
                    <p className="text-sm text-muted-foreground">Time Saved</p>
                    <p className="text-2xl font-bold text-foreground mt-1">6.2h</p>
                    <div className="mt-2 text-xs text-primary">Per week avg</div>
                  </div>
                </div>
                
                {/* Activity preview */}
                <div className="mt-4 rounded-lg border border-border bg-card p-4">
                  <div className="flex items-center justify-between mb-4">
                    <p className="text-sm font-medium text-foreground">Recent Activity</p>
                    <span className="text-xs text-muted-foreground">Last 24 hours</span>
                  </div>
                  <div className="space-y-3">
                    {[
                      { action: "Meeting scheduled", detail: "Team sync with Alice", time: "2m ago" },
                      { action: "Coordination completed", detail: "Project review", time: "1h ago" },
                      { action: "Approval received", detail: "Budget meeting", time: "3h ago" },
                    ].map((item, i) => (
                      <div key={i} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                        <div>
                          <p className="text-sm text-foreground">{item.action}</p>
                          <p className="text-xs text-muted-foreground">{item.detail}</p>
                        </div>
                        <span className="text-xs text-muted-foreground">{item.time}</span>
                      </div>
                    ))}
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
