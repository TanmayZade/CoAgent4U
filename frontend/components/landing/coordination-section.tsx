"use client"

import { ArrowRight, Bot, CheckCircle2, User } from "lucide-react"
import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

export function CoordinationSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headerRef = useRef<HTMLDivElement>(null)
  const flowRef = useRef<HTMLDivElement>(null)
  const stepsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Header animation
      gsap.fromTo(
        headerRef.current,
        { opacity: 0, y: 40 },
        { 
          opacity: 1, 
          y: 0, 
          duration: 0.8, 
          ease: "power3.out",
          scrollTrigger: {
            trigger: headerRef.current,
            start: "top 85%",
            toggleActions: "play none none reverse",
          }
        }
      )

      // Flow items stagger animation
      const flowItems = flowRef.current?.querySelectorAll(".flow-item")
      if (flowItems) {
        gsap.fromTo(
          flowItems,
          { opacity: 0, y: 30, scale: 0.9 },
          { 
            opacity: 1, 
            y: 0, 
            scale: 1,
            duration: 0.6, 
            stagger: 0.1,
            ease: "power3.out",
            scrollTrigger: {
              trigger: flowRef.current,
              start: "top 80%",
              toggleActions: "play none none reverse",
            }
          }
        )
      }

      // Arrows animation
      const arrows = flowRef.current?.querySelectorAll(".flow-arrow")
      if (arrows) {
        gsap.fromTo(
          arrows,
          { opacity: 0, x: -10 },
          { 
            opacity: 1, 
            x: 0,
            duration: 0.4, 
            stagger: 0.1,
            delay: 0.5,
            ease: "power2.out",
            scrollTrigger: {
              trigger: flowRef.current,
              start: "top 80%",
              toggleActions: "play none none reverse",
            }
          }
        )
      }

      // Steps stagger animation
      const stepCards = stepsRef.current?.querySelectorAll(".step-card")
      if (stepCards) {
        gsap.fromTo(
          stepCards,
          { opacity: 0, y: 30 },
          { 
            opacity: 1, 
            y: 0,
            duration: 0.6, 
            stagger: 0.15,
            ease: "power3.out",
            scrollTrigger: {
              trigger: stepsRef.current,
              start: "top 85%",
              toggleActions: "play none none reverse",
            }
          }
        )
      }
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} id="how-it-works" className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div ref={headerRef} className="max-w-2xl mx-auto text-center mb-16">
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">
            Agent Coordination Visualization
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            Agents coordinate so you don&apos;t have to
          </h2>
          <p className="text-muted-foreground text-lg">
            Personal agents collaborate through a coordination network, eliminating manual back-and-forth communication.
          </p>
        </div>

        {/* Coordination flow visual */}
        <div className="max-w-4xl mx-auto">
          <div className="rounded-2xl border border-border/60 bg-card p-6 lg:p-10 card-hover">
            {/* Mental model text */}
            <p className="text-center text-sm text-muted-foreground mb-8">
              Human → Personal Agent → Agent Coordination Network → Coordinated Outcome
            </p>

            {/* Flow diagram */}
            <div ref={flowRef} className="flex flex-col lg:flex-row items-center justify-between gap-6 lg:gap-4">
              {/* User A */}
              <div className="flow-item flex flex-col items-center text-center group cursor-pointer">
                <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mb-3 border border-border transition-all duration-300 group-hover:scale-110 group-hover:border-foreground/50">
                  <User className="w-7 h-7 text-foreground" />
                </div>
                <span className="text-sm font-medium text-foreground">User A</span>
                <span className="text-xs text-muted-foreground">Invokes agent</span>
              </div>

              <ArrowRight className="flow-arrow w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0 transition-colors duration-300 hover:text-foreground" />

              {/* Agent A */}
              <div className="flow-item flex flex-col items-center text-center group cursor-pointer">
                <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mb-3 border border-border transition-all duration-300 group-hover:scale-110 group-hover:border-foreground/50">
                  <Bot className="w-7 h-7 text-foreground transition-colors duration-300 group-hover:text-foreground" />
                </div>
                <span className="text-sm font-medium text-foreground">Agent A</span>
                <span className="text-xs text-muted-foreground">Parses intent</span>
              </div>

              <ArrowRight className="flow-arrow w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0 transition-colors duration-300 hover:text-foreground" />

              {/* Coordination Engine */}
              <div className="flow-item flex flex-col items-center text-center group cursor-pointer">
                <div className="w-16 h-16 rounded-2xl bg-foreground flex items-center justify-center mb-3 transition-all duration-300 group-hover:scale-110 group-hover:shadow-lg group-hover:shadow-foreground/20">
                  <span className="text-background font-bold text-lg">CE</span>
                </div>
                <span className="text-sm font-medium text-foreground">Coordination</span>
                <span className="text-xs text-muted-foreground">Engine</span>
              </div>

              <ArrowRight className="flow-arrow w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0 transition-colors duration-300 hover:text-foreground" />

              {/* Agent B */}
              <div className="flow-item flex flex-col items-center text-center group cursor-pointer">
                <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mb-3 border border-border transition-all duration-300 group-hover:scale-110 group-hover:border-foreground/50">
                  <Bot className="w-7 h-7 text-foreground transition-colors duration-300 group-hover:text-foreground" />
                </div>
                <span className="text-sm font-medium text-foreground">Agent B</span>
                <span className="text-xs text-muted-foreground">Checks availability</span>
              </div>

              <ArrowRight className="flow-arrow w-5 h-5 text-muted-foreground/50 rotate-90 lg:rotate-0 transition-colors duration-300 hover:text-foreground" />

              {/* User B */}
              <div className="flow-item flex flex-col items-center text-center group cursor-pointer">
                <div className="w-14 h-14 rounded-full bg-green-500/10 flex items-center justify-center mb-3 transition-all duration-300 group-hover:scale-110 group-hover:bg-green-500/20">
                  <CheckCircle2 className="w-7 h-7 text-green-600" />
                </div>
                <span className="text-sm font-medium text-foreground">User B</span>
                <span className="text-xs text-muted-foreground">Approves first</span>
              </div>
            </div>

            {/* Description */}
            <div ref={stepsRef} className="mt-10 pt-8 border-t border-border/60">
              <div className="grid sm:grid-cols-3 gap-6">
                <div className="step-card p-4 rounded-xl transition-all duration-300 hover:bg-muted/30">
                  <p className="text-sm font-medium text-foreground mb-1">Step 1: Intent & Availability</p>
                  <p className="text-sm text-muted-foreground">
                    Agent A parses the request and checks User A&apos;s calendar for available time slots.
                  </p>
                </div>
                <div className="step-card p-4 rounded-xl transition-all duration-300 hover:bg-muted/30">
                  <p className="text-sm font-medium text-foreground mb-1">Step 2: Deterministic Matching</p>
                  <p className="text-sm text-muted-foreground">
                    Coordination Engine queries Agent B and performs deterministic matching to find common availability.
                  </p>
                </div>
                <div className="step-card p-4 rounded-xl transition-all duration-300 hover:bg-muted/30">
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
