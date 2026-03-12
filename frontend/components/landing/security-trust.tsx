import { KeyRound, Calendar, Shield, FileText } from "lucide-react"

const securityFeatures = [
  {
    icon: KeyRound,
    title: "OAuth2 Authentication",
    description: "Enterprise-grade security with industry-standard OAuth2 protocol. Your credentials are never stored.",
  },
  {
    icon: Calendar,
    title: "Google Calendar Integration",
    description: "Secure two-way sync with Google Calendar using official APIs. Read and write with proper scopes.",
  },
  {
    icon: Shield,
    title: "Secure by Design",
    description: "Built with security-first architecture. All data encrypted in transit and at rest.",
  },
  {
    icon: FileText,
    title: "Complete Audit Logs",
    description: "Every action is timestamped and logged. Full transparency into what your agent does and when.",
  },
]

export function SecurityTrust() {
  return (
    <section id="security" className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          {/* Left: Visual */}
          <div className="order-2 lg:order-1">
            <div className="rounded-2xl border border-border/60 bg-card p-6 lg:p-8">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-green-500/10 flex items-center justify-center">
                  <Shield className="w-5 h-5 text-green-600" />
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">Security Status</p>
                  <p className="text-xs text-green-600">All systems secure</p>
                </div>
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between py-3 border-b border-border/40">
                  <span className="text-sm text-muted-foreground">OAuth Integration</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600">Connected</span>
                </div>
                <div className="flex items-center justify-between py-3 border-b border-border/40">
                  <span className="text-sm text-muted-foreground">Google Calendar</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600">Synced</span>
                </div>
                <div className="flex items-center justify-between py-3 border-b border-border/40">
                  <span className="text-sm text-muted-foreground">Data Encryption</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600">AES-256</span>
                </div>
                <div className="flex items-center justify-between py-3">
                  <span className="text-sm text-muted-foreground">Audit Logging</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600">Enabled</span>
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-border/40">
                <p className="text-xs text-muted-foreground">
                  Last security audit: March 10, 2026
                </p>
              </div>
            </div>
          </div>

          {/* Right: Content */}
          <div className="order-1 lg:order-2">
            <p className="text-sm font-medium text-primary mb-3">
              Security & Trust
            </p>
            <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
              Enterprise-grade security
            </h2>
            <p className="text-muted-foreground text-lg mb-10">
              Your data security is our priority. CoAgent4U is built with the highest security standards.
            </p>

            <div className="grid sm:grid-cols-2 gap-6">
              {securityFeatures.map((feature) => (
                <div key={feature.title}>
                  <div className="w-10 h-10 rounded-xl bg-muted flex items-center justify-center mb-3">
                    <feature.icon className="w-5 h-5 text-foreground" />
                  </div>
                  <h3 className="font-medium text-foreground mb-1">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
