"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight, Play, Calendar, Bot, CheckCircle2 } from "lucide-react"
import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

export function HeroSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headlineRef = useRef<HTMLHeadingElement>(null)
  const subheadlineRef = useRef<HTMLParagraphElement>(null)
  const ctaRef = useRef<HTMLDivElement>(null)
  const visualRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Headline animation with split text effect
      if (headlineRef.current) {
        const text = headlineRef.current.textContent || ""
        const words = text.split(" ")
        headlineRef.current.innerHTML = words
          .map(word => `<span class="inline-block overflow-hidden"><span class="inline-block translate-y-full">${word}</span></span>`)
          .join(" ")

        const innerSpans = headlineRef.current.querySelectorAll("span > span")
        gsap.to(innerSpans, {
          y: 0,
          duration: 1,
          stagger: 0.08,
          ease: "power3.out",
          delay: 0.2,
        })
      }

      // Subheadline fade in
      gsap.fromTo(
        subheadlineRef.current,
        { opacity: 0, y: 30 },
        { opacity: 1, y: 0, duration: 1, delay: 0.8, ease: "power3.out" }
      )

      // CTA buttons animation
      gsap.fromTo(
        ctaRef.current?.children || [],
        { opacity: 0, y: 20 },
        { opacity: 1, y: 0, duration: 0.8, stagger: 0.15, delay: 1.1, ease: "power3.out" }
      )

      // Visual card animation
      gsap.fromTo(
        visualRef.current,
        { opacity: 0, y: 60, scale: 0.95 },
        { opacity: 1, y: 0, scale: 1, duration: 1.2, delay: 1.3, ease: "power3.out" }
      )
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} className="relative pt-28 pb-20 lg:pt-36 lg:pb-28 overflow-hidden">
      {/* Animated background gradient */}
      <div className="absolute inset-0 -z-10">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[600px] bg-gradient-to-b from-primary/[0.03] to-transparent rounded-full blur-3xl animate-pulse-glow" />
      </div>

      <div className="mx-auto max-w-6xl px-6">
        <div className="mx-auto max-w-3xl text-center">
          {/* Headline */}
          <h1 
            ref={headlineRef}
            className="text-4xl sm:text-5xl lg:text-6xl font-semibold tracking-tight text-foreground leading-[1.1] text-balance"
          >
            Your Personal Agent That Coordinates Your Time
          </h1>

          {/* Subheadline */}
          <p 
            ref={subheadlineRef}
            className="mt-6 text-lg lg:text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto text-pretty opacity-0"
          >
            A coordination platform where personal agents represent users and collaborate to manage commitments, schedules, and shared time.
          </p>

          {/* CTAs */}
          <div ref={ctaRef} className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button size="lg" className="h-12 px-6 text-base magnetic-btn interactive-hover" asChild>
              <Link href="/signin">
                Get Started
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            <Button variant="outline" size="lg" className="h-12 px-6 text-base magnetic-btn" asChild>
              <Link href="#use-cases">
                <Play className="mr-2 h-4 w-4" />
                View Demo
              </Link>
            </Button>
          </div>
        </div>

        {/* Hero Visual - Agent Interaction Preview */}
        <div className="mt-20 lg:mt-24">
          <div ref={visualRef} className="relative mx-auto max-w-4xl opacity-0">
            {/* Main card with agent interaction */}
            <div className="rounded-2xl border border-border/60 bg-card shadow-xl shadow-black/[0.03] overflow-hidden card-hover">
              {/* Header bar */}
              <div className="flex items-center justify-between px-5 py-3 bg-muted/30 border-b border-border/40">
                <div className="flex items-center gap-3">
                  <div className="flex gap-1.5">
                    <div className="w-3 h-3 rounded-full bg-red-400/80" />
                    <div className="w-3 h-3 rounded-full bg-yellow-400/80" />
                    <div className="w-3 h-3 rounded-full bg-green-400/80" />
                  </div>
                  <span className="text-sm text-muted-foreground">CoAgent4U</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
                  Connected
                </div>
              </div>
              
              {/* Content */}
              <div className="p-6 lg:p-8">
                <div className="grid lg:grid-cols-2 gap-8">
                  {/* Left: Chat/Command */}
                  <div className="space-y-4">
                    <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                      <Bot className="w-4 h-4 text-primary" />
                      Agent Interaction
                    </div>
                    
                    {/* Command input */}
                    <div className="rounded-xl border border-border/60 bg-muted/20 p-4 interactive-hover">
                      <p className="text-sm text-muted-foreground mb-2">You said:</p>
                      <p className="text-foreground font-medium">
                        {"@CoAgent4U schedule meeting with @Sarah Friday evening"}
                      </p>
                    </div>
                    
                    {/* Agent response */}
                    <div className="rounded-xl border border-primary/20 bg-primary/[0.02] p-4 interactive-hover">
                      <p className="text-sm text-primary mb-2">Agent Response:</p>
                      <p className="text-foreground text-sm leading-relaxed">
                        {"Coordinating with Sarah's agent. Common availability found: 6:00 PM - 7:00 PM. Awaiting Sarah's approval before confirming."}
                      </p>
                      <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
                        <CheckCircle2 className="w-3.5 h-3.5 text-green-500" />
                        Agent-to-agent coordination in progress
                      </div>
                    </div>
                  </div>
                  
                  {/* Right: Schedule Preview */}
                  <div className="space-y-4">
                    <div className="flex items-center gap-2 text-sm font-medium text-foreground">
                      <Calendar className="w-4 h-4 text-primary" />
                      Friday Schedule
                    </div>
                    
                    <div className="rounded-xl border border-border/60 bg-muted/20 p-4 space-y-3">
                      {[
                        { time: "9:00 AM", event: "Team Standup", duration: "30m" },
                        { time: "11:00 AM", event: "Project Review", duration: "1h" },
                        { time: "6:00 PM", event: "Meeting with Sarah", duration: "1h", pending: true },
                      ].map((item, i) => (
                        <div 
                          key={i} 
                          className={`flex items-center justify-between py-2.5 px-3 rounded-lg transition-all duration-300 hover:scale-[1.02] ${
                            item.pending 
                              ? "bg-primary/5 border border-primary/20 animate-pulse-glow" 
                              : "bg-background/50 hover:bg-background/80"
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <span className="text-xs font-mono text-muted-foreground w-16">{item.time}</span>
                            <span className={`text-sm ${item.pending ? "text-primary font-medium" : "text-foreground"}`}>
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
      </div>
    </section>
  )
}
