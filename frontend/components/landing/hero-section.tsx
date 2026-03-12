"use client"

import Link from "next/link"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
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

      // Headline animation with split text effect - word by word
      if (headlineRef.current) {
        const text = headlineRef.current.textContent || ""
        const words = text.split(" ")
        headlineRef.current.innerHTML = words
          .map(word => `<span class="inline-block overflow-hidden mr-[0.25em]"><span class="inline-block translate-y-full">${word}</span></span>`)
          .join("")

        const innerSpans = headlineRef.current.querySelectorAll("span > span")
        gsap.to(innerSpans, {
          y: 0,
          duration: 1.2,
          stagger: 0.1,
          ease: "power4.out",
          delay: 0.5,
        })
      }

      // Subheadline fade in with blur effect
      gsap.fromTo(
        subheadlineRef.current,
        { opacity: 0, y: 40, filter: "blur(10px)" },
        { opacity: 1, y: 0, filter: "blur(0px)", duration: 1.2, delay: 1.5, ease: "power3.out" }
      )

      // CTA buttons animation with stagger
      gsap.fromTo(
        ctaRef.current?.children || [],
        { opacity: 0, y: 30, scale: 0.95 },
        { opacity: 1, y: 0, scale: 1, duration: 0.8, stagger: 0.2, delay: 2, ease: "back.out(1.4)" }
      )
    }, sectionRef)

    return () => ctx.revert()
  }, [particles])

  return (
    <section ref={sectionRef} className="relative min-h-screen flex items-center justify-center overflow-hidden">
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

      <div className="mx-auto max-w-7xl px-6 py-32 lg:py-40">
        <div className="mx-auto max-w-5xl text-center">
          {/* Logo + Brand */}
          <div ref={logoRef} className="flex items-center justify-center gap-5 mb-14">
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

          {/* Headline - Large, bold, centered */}
          <h1 
            ref={headlineRef}
            className="text-5xl sm:text-6xl md:text-7xl lg:text-8xl font-semibold tracking-tight text-foreground leading-[1.05] text-balance"
          >
            Your Personal Agent That Coordinates Your Time
          </h1>

          {/* Subheadline */}
          <p 
            ref={subheadlineRef}
            className="mt-12 text-xl lg:text-2xl text-muted-foreground leading-relaxed max-w-3xl mx-auto text-pretty"
          >
            A coordination platform where personal agents represent users and collaborate to manage commitments, schedules, and shared time.
          </p>

          {/* CTAs */}
          <div ref={ctaRef} className="mt-14 flex flex-col sm:flex-row items-center justify-center gap-5">
            <Button 
              size="lg" 
              className="h-14 px-10 text-lg font-medium rounded-full bg-foreground text-background hover:bg-foreground/90 shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105" 
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
              className="h-14 px-10 text-lg font-medium rounded-full border-2 border-foreground/20 hover:border-foreground/40 hover:bg-muted/50 transition-all duration-300 hover:scale-105" 
              asChild
            >
              <Link href="#use-cases">
                Explore Use Cases
              </Link>
            </Button>
          </div>
        </div>
      </div>

      {/* Scroll indicator */}
      <div className="absolute bottom-10 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 animate-bounce">
        <span className="text-xs text-muted-foreground uppercase tracking-widest">Scroll</span>
        <div className="w-px h-8 bg-gradient-to-b from-foreground/40 to-transparent" />
      </div>
    </section>
  )
}
