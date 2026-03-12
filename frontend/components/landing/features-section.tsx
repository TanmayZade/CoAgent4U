"use client"

import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"
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

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

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
  const sectionRef = useRef<HTMLElement>(null)
  const headingRef = useRef<HTMLDivElement>(null)
  const cardsRef = useRef<HTMLDivElement>(null)
  const gridRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current) return

    const ctx = gsap.context(() => {
      // Heading animation
      if (headingRef.current) {
        gsap.fromTo(
          headingRef.current.children,
          { y: 40, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.8,
            stagger: 0.15,
            ease: "power3.out",
            scrollTrigger: {
              trigger: headingRef.current,
              start: "top 85%",
            },
          }
        )
      }

      // Differentiator cards
      if (cardsRef.current) {
        const cards = cardsRef.current.children
        gsap.fromTo(
          cards,
          { y: 60, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.8,
            stagger: 0.12,
            ease: "power3.out",
            scrollTrigger: {
              trigger: cardsRef.current,
              start: "top 80%",
            },
          }
        )
      }

      // Feature grid
      if (gridRef.current) {
        const items = gridRef.current.children
        gsap.fromTo(
          items,
          { y: 40, opacity: 0, scale: 0.95 },
          {
            y: 0,
            opacity: 1,
            scale: 1,
            duration: 0.6,
            stagger: 0.08,
            ease: "power3.out",
            scrollTrigger: {
              trigger: gridRef.current,
              start: "top 85%",
            },
          }
        )
      }
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} id="features" className="relative py-32 noise-overlay">
      <div className="absolute inset-0 bg-charcoal-light" />
      
      <div className="relative z-10 max-w-6xl mx-auto px-6 sm:px-8 lg:px-12">
        {/* Section header */}
        <div ref={headingRef} className="text-center mb-20">
          <span className="inline-block text-xs font-mono text-accent tracking-widest uppercase mb-4">
            What Makes It Different
          </span>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-cream font-[family-name:var(--font-display)] tracking-tight mb-6">
            Not another chatbot
          </h2>
          <p className="text-foreground-secondary max-w-lg mx-auto text-lg">
            A deterministic coordination engine built for trust and transparency.
          </p>
        </div>

        {/* Differentiator cards */}
        <div ref={cardsRef} className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-32">
          {differentiators.map((item) => (
            <div
              key={item.title}
              className="group p-8 bg-charcoal rounded-2xl border border-border hover:border-accent/30 transition-all duration-500 card-lift"
            >
              <div className="w-12 h-12 rounded-xl bg-accent/10 flex items-center justify-center mb-6 group-hover:bg-accent/20 transition-colors duration-300">
                <item.icon className="w-6 h-6 text-accent" />
              </div>
              <h3 className="text-xl font-semibold text-cream mb-3 font-[family-name:var(--font-display)]">
                {item.title}
              </h3>
              <p className="text-foreground-secondary leading-relaxed">
                {item.description}
              </p>
            </div>
          ))}
        </div>

        {/* Feature grid section */}
        <div className="text-center mb-16">
          <span className="inline-block text-xs font-mono text-accent tracking-widest uppercase mb-4">
            Capabilities
          </span>
          <h2 className="text-3xl sm:text-4xl font-bold text-cream font-[family-name:var(--font-display)] tracking-tight">
            Built for modern workflows
          </h2>
        </div>

        <div ref={gridRef} className="grid grid-cols-2 md:grid-cols-3 gap-4">
          {features.map((feature) => (
            <div
              key={feature.title}
              className="group p-6 bg-charcoal/50 rounded-xl border border-border/50 hover:border-border-light hover:bg-charcoal transition-all duration-300"
            >
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-lg bg-charcoal-lighter flex items-center justify-center flex-shrink-0 group-hover:bg-accent/10 transition-colors duration-300">
                  <feature.icon className="w-5 h-5 text-foreground-secondary group-hover:text-accent transition-colors duration-300" />
                </div>
                <div>
                  <h3 className="font-medium text-cream mb-1">{feature.title}</h3>
                  <p className="text-sm text-foreground-muted">{feature.description}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
