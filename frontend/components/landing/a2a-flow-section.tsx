"use client"

import { useEffect, useRef, useState } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"
import { MessageSquare, Clock, CheckCircle, Calendar, ArrowRight } from "lucide-react"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

const steps = [
  {
    icon: MessageSquare,
    title: "Request",
    description: "User A messages their agent to schedule with User B",
    state: "PENDING",
  },
  {
    icon: Clock,
    title: "Coordinate",
    description: "Both agents check calendars and find matching slots",
    state: "COORDINATING",
  },
  {
    icon: CheckCircle,
    title: "Approve",
    description: "Both users review and approve the proposed time",
    state: "AWAITING_APPROVAL",
  },
  {
    icon: Calendar,
    title: "Scheduled",
    description: "Calendar event created for both parties",
    state: "COMPLETED",
  },
]

export function A2AFlowSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headingRef = useRef<HTMLDivElement>(null)
  const flowRef = useRef<HTMLDivElement>(null)
  const lineRef = useRef<SVGSVGElement>(null)
  const mockRef = useRef<HTMLDivElement>(null)
  const [activeStep, setActiveStep] = useState(0)

  useEffect(() => {
    if (!sectionRef.current) return

    const ctx = gsap.context(() => {
      // Heading
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

      // Flow steps
      if (flowRef.current) {
        const stepElements = flowRef.current.querySelectorAll(".flow-step")
        gsap.fromTo(
          stepElements,
          { y: 50, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.7,
            stagger: 0.15,
            ease: "power3.out",
            scrollTrigger: {
              trigger: flowRef.current,
              start: "top 80%",
            },
          }
        )
      }

      // Connecting line draw animation
      if (lineRef.current) {
        const line = lineRef.current.querySelector("line")
        if (line) {
          const length = line.getTotalLength()
          gsap.set(line, { strokeDasharray: length, strokeDashoffset: length })
          gsap.to(line, {
            strokeDashoffset: 0,
            duration: 1.5,
            ease: "power2.out",
            scrollTrigger: {
              trigger: flowRef.current,
              start: "top 75%",
            },
          })
        }
      }

      // Mock Slack message
      if (mockRef.current) {
        gsap.fromTo(
          mockRef.current,
          { y: 60, opacity: 0, scale: 0.95 },
          {
            y: 0,
            opacity: 1,
            scale: 1,
            duration: 0.8,
            ease: "power3.out",
            scrollTrigger: {
              trigger: mockRef.current,
              start: "top 85%",
            },
          }
        )
      }

      // Auto-advance steps
      ScrollTrigger.create({
        trigger: flowRef.current,
        start: "top 60%",
        onEnter: () => {
          let step = 0
          const interval = setInterval(() => {
            step++
            if (step >= steps.length) {
              step = 0
            }
            setActiveStep(step)
          }, 2000)
          return () => clearInterval(interval)
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} id="how-it-works" className="relative py-32 noise-overlay overflow-hidden">
      <div className="absolute inset-0 bg-charcoal" />
      
      {/* Subtle accent gradient */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-accent/3 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 max-w-6xl mx-auto px-6 sm:px-8 lg:px-12">
        {/* Section header */}
        <div ref={headingRef} className="text-center mb-20">
          <span className="inline-block text-xs font-mono text-accent tracking-widest uppercase mb-4">
            A2A Protocol
          </span>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-cream font-[family-name:var(--font-display)] tracking-tight mb-6">
            Agent-to-Agent Flow
          </h2>
          <p className="text-foreground-secondary max-w-lg mx-auto text-lg">
            Watch how two agents collaborate with full transparency at every step.
          </p>
        </div>

        {/* Flow steps */}
        <div ref={flowRef} className="relative mb-24">
          {/* Connecting line */}
          <svg
            ref={lineRef}
            className="absolute top-12 left-0 w-full h-1 hidden md:block"
            preserveAspectRatio="none"
          >
            <line
              x1="12.5%"
              y1="50%"
              x2="87.5%"
              y2="50%"
              stroke="currentColor"
              strokeWidth="1"
              className="text-border"
            />
          </svg>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {steps.map((step, index) => (
              <div
                key={step.title}
                className={`flow-step relative text-center transition-all duration-500 ${
                  activeStep === index ? "scale-105" : ""
                }`}
              >
                <div
                  className={`relative z-10 w-12 h-12 mx-auto mb-6 rounded-full flex items-center justify-center transition-all duration-500 ${
                    activeStep >= index
                      ? "bg-accent text-cream"
                      : "bg-charcoal-lighter text-foreground-muted border border-border"
                  }`}
                >
                  <step.icon className="w-5 h-5" />
                </div>
                <h3 className="font-semibold text-cream mb-2 font-[family-name:var(--font-display)]">
                  {step.title}
                </h3>
                <p className="text-sm text-foreground-secondary max-w-[200px] mx-auto">
                  {step.description}
                </p>
                {index < steps.length - 1 && (
                  <ArrowRight className="w-4 h-4 text-border absolute top-12 -right-4 hidden md:block" />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Mock Slack message */}
        <div ref={mockRef} className="max-w-md mx-auto">
          <div className="bg-charcoal-light rounded-xl border border-border overflow-hidden shadow-2xl">
            {/* Header */}
            <div className="px-5 py-4 border-b border-border flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-accent flex items-center justify-center">
                <span className="text-xs font-bold text-cream">CA</span>
              </div>
              <div>
                <div className="text-sm font-medium text-cream">CoAgent4U</div>
                <div className="text-xs text-foreground-muted font-mono">APP</div>
              </div>
            </div>
            
            {/* Content */}
            <div className="p-5">
              <p className="text-sm text-foreground-secondary mb-4">
                Meeting proposal from <span className="text-accent">@alex</span>:
              </p>
              <div className="bg-charcoal rounded-lg p-4 border border-border mb-4">
                <div className="flex items-center gap-2 mb-2">
                  <Calendar className="w-4 h-4 text-accent" />
                  <span className="text-sm font-medium text-cream">Friday, 6:00 PM</span>
                </div>
                <p className="text-xs text-foreground-muted">Quick sync about project updates</p>
              </div>
              <div className="flex gap-3">
                <button className="flex-1 bg-accent hover:bg-accent-dark text-cream text-sm font-medium py-2.5 px-4 rounded-lg transition-colors">
                  Approve
                </button>
                <button className="flex-1 bg-transparent hover:bg-charcoal text-foreground-secondary border border-border text-sm font-medium py-2.5 px-4 rounded-lg transition-colors">
                  Decline
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
