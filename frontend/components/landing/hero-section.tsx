"use client"

import Link from "next/link"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { ArrowRight, Bot, Calendar, CheckCircle2 } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

// Particle type
interface Particle {
  id: number
  x: number
  y: number
  size: number
  delay: number
  duration: number
}

// Generate random particles for the scattered effect
function generateParticles(count: number): Particle[] {
  const particles: Particle[] = []
  for (let i = 0; i < count; i++) {
    particles.push({
      id: i,
      x: Math.random() * 35,
      y: Math.random() * 100,
      size: Math.random() * 3 + 1,
      delay: Math.random() * 2,
      duration: Math.random() * 3 + 2,
    })
  }
  // Add particles on the right side too
  for (let i = count; i < count * 2; i++) {
    particles.push({
      id: i,
      x: 65 + Math.random() * 35,
      y: Math.random() * 100,
      size: Math.random() * 3 + 1,
      delay: Math.random() * 2,
      duration: Math.random() * 3 + 2,
    })
  }
  return particles
}

export function HeroSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headlineRef = useRef<HTMLHeadingElement>(null)
  const subheadlineRef = useRef<HTMLParagraphElement>(null)
  const ctaRef = useRef<HTMLDivElement>(null)
  const logoRef = useRef<HTMLDivElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)
  const particlesRef = useRef<HTMLDivElement>(null)
  
  // Generate particles only on client side to avoid hydration mismatch
  const [particles, setParticles] = useState<Particle[]>([])
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setParticles(generateParticles(30))
    setMounted(true)
  }, [])

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Particles floating animation
      const particleElements = particlesRef.current?.querySelectorAll(".particle")
      if (particleElements) {
        particleElements.forEach((particle, i) => {
          gsap.to(particle, {
            y: "random(-30, 30)",
            x: "random(-15, 15)",
            duration: particles[i % particles.length]?.duration || 3,
            delay: particles[i % particles.length]?.delay || 0,
            repeat: -1,
            yoyo: true,
            ease: "sine.inOut",
          })
        })
      }

      // Logo animation - scale and fade in elegantly
      gsap.fromTo(
        logoRef.current,
        { opacity: 0, scale: 0.8, y: 20 },
        { opacity: 1, scale: 1, y: 0, duration: 1, ease: "power3.out", delay: 0.2 }
      )

      // Headline animation - fade in with stagger per word
      if (headlineRef.current) {
        gsap.fromTo(
          headlineRef.current,
          { opacity: 0, y: 60 },
          { opacity: 1, y: 0, duration: 1.2, ease: "power4.out", delay: 0.5 }
        )
      }

      // Subheadline fade in with blur effect
      gsap.fromTo(
        subheadlineRef.current,
        { opacity: 0, y: 40, filter: "blur(10px)" },
        { opacity: 1, y: 0, filter: "blur(0px)", duration: 1.2, delay: 1.2, ease: "power3.out" }
      )

      // CTA buttons animation with stagger
      gsap.fromTo(
        ctaRef.current?.children || [],
        { opacity: 0, y: 30, scale: 0.95 },
        { opacity: 1, y: 0, scale: 1, duration: 0.8, stagger: 0.2, delay: 1.6, ease: "back.out(1.4)" }
      )

      // Card animation
      gsap.fromTo(
        cardRef.current,
        { opacity: 0, y: 60, scale: 0.95 },
        { opacity: 1, y: 0, scale: 1, duration: 1, delay: 2, ease: "power3.out" }
      )
    }, sectionRef)

    return () => ctx.revert()
  }, [particles])

  return (
    <section ref={sectionRef} className="relative min-h-screen flex flex-col justify-center overflow-hidden pt-24 pb-16">
      {/* Scattered particles - both sides (only render after mount to avoid hydration mismatch) */}
      <div ref={particlesRef} className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        {mounted && particles.map((particle) => (
          <div
            key={particle.id}
            className="particle absolute rounded-full bg-foreground/15"
            style={{
              left: `${particle.x}%`,
              top: `${particle.y}%`,
              width: `${particle.size}px`,
              height: `${particle.size}px`,
            }}
          />
        ))}
      </div>

      {/* Subtle gradient background */}
      <div className="absolute inset-0 -z-20">
        <div className="absolute top-1/4 left-1/4 w-[800px] h-[800px] bg-gradient-to-br from-muted/40 to-transparent rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-[600px] h-[600px] bg-gradient-to-tl from-muted/30 to-transparent rounded-full blur-3xl" />
      </div>

      <div className="mx-auto max-w-7xl px-6 w-full">
        <div className="mx-auto max-w-5xl text-center">
          {/* Logo + Brand */}
          <div ref={logoRef} className="flex items-center justify-center gap-5 mb-12">
            <Image 
              src="/images/logo.png" 
              alt="CoAgent4U Logo" 
              width={72} 
              height={72}
              className="drop-shadow-md"
            />
            <span className="text-3xl font-serif font-medium text-foreground tracking-tight italic">
              CoAgent4U
            </span>
          </div>

          {/* Headline - Large, bold, centered with proper word wrapping */}
          <h1 
            ref={headlineRef}
            className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-semibold tracking-tight text-foreground leading-[1.1] max-w-4xl mx-auto"
          >
            Your Personal Agent That Coordinates Your Time
          </h1>

          {/* Subheadline */}
          <p 
            ref={subheadlineRef}
            className="mt-8 text-lg lg:text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto"
          >
            A coordination platform where personal agents represent users and collaborate to manage commitments, schedules, and shared time.
          </p>

          {/* CTAs */}
          <div ref={ctaRef} className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button 
              size="lg" 
              className="h-13 px-8 text-base font-medium rounded-full bg-foreground text-background hover:bg-foreground/90 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105" 
              asChild
            >
              <Link href="/signin">
                Get Started
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>
            <Button 
              variant="outline" 
              size="lg" 
              className="h-13 px-8 text-base font-medium rounded-full border-2 border-foreground/20 hover:border-foreground/40 hover:bg-muted/50 transition-all duration-300 hover:scale-105" 
              asChild
            >
              <Link href="#use-cases">
                Explore Use Cases
              </Link>
            </Button>
          </div>
        </div>

        {/* Agent Preview Card */}
        <div 
          ref={cardRef}
          className="mt-16 max-w-4xl mx-auto"
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
