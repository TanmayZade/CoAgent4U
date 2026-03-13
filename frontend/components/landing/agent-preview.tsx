"use client"

import { Bot, Calendar, CheckCircle2 } from "lucide-react"
import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

export function AgentPreview() {
  const sectionRef = useRef<HTMLElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Card scroll animation
      gsap.fromTo(
        cardRef.current,
        { opacity: 0, y: 80, scale: 0.95 },
        { 
          opacity: 1, 
          y: 0, 
          scale: 1, 
          duration: 1.2, 
          ease: "power3.out",
          scrollTrigger: {
            trigger: cardRef.current,
            start: "top 85%",
            toggleActions: "play none none reverse",
          }
        }
      )
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} className="py-16 lg:py-24">
      <div className="mx-auto max-w-7xl px-6">
        {/* Agent Preview Card */}
        <div 
          ref={cardRef}
          className="max-w-4xl mx-auto"
        >
          <div className="rounded-2xl border border-border/60 bg-card shadow-2xl shadow-black/[0.08] overflow-hidden">
            {/* Window Header */}
            <div className="flex items-center justify-between px-5 py-3 border-b border-border/60 bg-muted/30">
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-red-400" />
                  <div className="w-3 h-3 rounded-full bg-yellow-400" />
                  <div className="w-3 h-3 rounded-full bg-green-400" />
                </div>
                <span className="text-sm font-medium text-foreground ml-2">CoAgent4U</span>
              </div>
              <div className="flex items-center gap-2 text-xs text-green-600">
                <div className="w-2 h-2 rounded-full bg-green-500" />
                Connected
              </div>
            </div>

            {/* Content */}
            <div className="p-6 lg:p-8">
              <div className="grid lg:grid-cols-2 gap-8">
                {/* Left: Chat/Command */}
                <div className="space-y-5">
                  <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <Bot className="w-4 h-4 text-foreground" />
                    Agent Interaction
                  </div>
                  
                  {/* Command input */}
                  <div className="rounded-xl border border-border/60 bg-muted/30 p-4">
                    <p className="text-sm text-muted-foreground mb-2">You said:</p>
                    <p className="text-foreground font-medium">
                      @CoAgent4U schedule meeting with @Sarah Friday evening
                    </p>
                  </div>
                  
                  {/* Agent response */}
                  <div className="rounded-xl border border-foreground/20 bg-foreground/[0.02] p-4">
                    <p className="text-sm text-foreground font-medium mb-2">Agent Response:</p>
                    <p className="text-foreground/80 text-sm leading-relaxed">
                      Coordinating with Sarah&apos;s agent. Common availability found: 6:00 PM - 7:00 PM. Awaiting Sarah&apos;s approval before confirming.
                    </p>
                    <div className="mt-3 flex items-center gap-2 text-xs text-green-600">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      Agent-to-agent coordination in progress
                    </div>
                  </div>
                </div>

                {/* Right: Schedule Preview */}
                <div className="space-y-5">
                  <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <Calendar className="w-4 h-4 text-foreground" />
                    Friday Schedule
                  </div>
                  
                  <div className="rounded-xl border border-border/60 bg-muted/30 p-4 space-y-3">
                    {[
                      { time: "9:00 AM", event: "Team Standup", duration: "30m" },
                      { time: "11:00 AM", event: "Project Review", duration: "1h" },
                      { time: "6:00 PM", event: "Meeting with Sarah", duration: "1h", pending: true },
                    ].map((item, i) => (
                      <div 
                        key={i} 
                        className={`flex items-center justify-between py-2.5 px-3 rounded-lg transition-all duration-300 ${
                          item.pending 
                            ? "bg-foreground/5 border border-foreground/20" 
                            : "bg-background/50"
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <span className="text-xs font-mono text-muted-foreground w-16">{item.time}</span>
                          <span className={`text-sm ${item.pending ? "text-foreground font-medium" : "text-foreground"}`}>
                            {item.event}
                          </span>
                        </div>
                        <span className="text-xs text-muted-foreground">{item.duration}</span>
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
