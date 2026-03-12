"use client"

import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import Link from "next/link"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

export function HeroSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headlineRef = useRef<HTMLHeadingElement>(null)
  const sublineRef = useRef<HTMLParagraphElement>(null)
  const ctaRef = useRef<HTMLDivElement>(null)
  const badgesRef = useRef<HTMLDivElement>(null)
  const scrollIndicatorRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current) return

    const ctx = gsap.context(() => {
      const tl = gsap.timeline({ defaults: { ease: "power3.out" } })

      // Headline animation - words stagger
      if (headlineRef.current) {
        const words = headlineRef.current.querySelectorAll(".word")
        tl.fromTo(
          words,
          { y: 100, opacity: 0, rotateX: -90 },
          { 
            y: 0, 
            opacity: 1, 
            rotateX: 0,
            duration: 1,
            stagger: 0.08,
          },
          0.2
        )
      }

      // Subline fade in
      if (sublineRef.current) {
        tl.fromTo(
          sublineRef.current,
          { y: 30, opacity: 0 },
          { y: 0, opacity: 1, duration: 0.8 },
          0.8
        )
      }

      // CTA buttons
      if (ctaRef.current) {
        tl.fromTo(
          ctaRef.current.children,
          { y: 20, opacity: 0 },
          { y: 0, opacity: 1, duration: 0.6, stagger: 0.1 },
          1
        )
      }

      // Badges
      if (badgesRef.current) {
        tl.fromTo(
          badgesRef.current.children,
          { y: 15, opacity: 0 },
          { y: 0, opacity: 1, duration: 0.5, stagger: 0.05 },
          1.2
        )
      }

      // Scroll indicator
      if (scrollIndicatorRef.current) {
        tl.fromTo(
          scrollIndicatorRef.current,
          { opacity: 0 },
          { opacity: 1, duration: 0.5 },
          1.5
        )

        // Continuous bounce animation
        gsap.to(scrollIndicatorRef.current.querySelector(".scroll-line"), {
          y: 10,
          duration: 1.2,
          repeat: -1,
          yoyo: true,
          ease: "power1.inOut",
        })
      }

      // Parallax effect on scroll
      gsap.to(headlineRef.current, {
        y: -50,
        opacity: 0.3,
        ease: "none",
        scrollTrigger: {
          trigger: sectionRef.current,
          start: "top top",
          end: "bottom top",
          scrub: 1,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  const words = ["Your", "Agent", "Handles", "the", "Noise."]
  const words2 = ["You", "Stay", "in", "Control."]

  return (
    <section
      ref={sectionRef}
      className="relative min-h-screen flex flex-col items-center justify-center overflow-hidden noise-overlay"
    >
      {/* Subtle gradient background */}
      <div className="absolute inset-0 bg-charcoal">
        <div className="absolute inset-0 bg-gradient-to-b from-charcoal via-charcoal to-[#0f0f0f]" />
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-accent/5 rounded-full blur-3xl" />
        <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-cream/3 rounded-full blur-3xl" />
      </div>

      {/* Content */}
      <div className="relative z-10 max-w-5xl mx-auto px-6 sm:px-8 lg:px-12 text-center">
        {/* Status indicator */}
        <div className="inline-flex items-center gap-3 mb-12">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full rounded-full bg-accent opacity-75 status-pulse" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-accent" />
          </span>
          <span className="text-sm font-mono text-foreground-secondary tracking-wider uppercase">
            Agents Online
          </span>
        </div>

        {/* Main headline */}
        <h1
          ref={headlineRef}
          className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight mb-4 font-[family-name:var(--font-display)] leading-[1.1]"
          style={{ perspective: "1000px" }}
        >
          <span className="block overflow-hidden">
            {words.map((word, i) => (
              <span
                key={i}
                className="word inline-block mr-[0.25em] text-cream"
                style={{ transformStyle: "preserve-3d" }}
              >
                {word}
              </span>
            ))}
          </span>
          <span className="block overflow-hidden mt-2">
            {words2.map((word, i) => (
              <span
                key={i}
                className={`word inline-block mr-[0.25em] ${
                  word === "Control." ? "text-accent" : "text-cream"
                }`}
                style={{ transformStyle: "preserve-3d" }}
              >
                {word}
              </span>
            ))}
          </span>
        </h1>

        {/* Subheadline */}
        <p
          ref={sublineRef}
          className="max-w-xl mx-auto text-lg sm:text-xl text-foreground-secondary mb-12 leading-relaxed"
        >
          A deterministic coordination platform for personal agents.
          <br className="hidden sm:block" />
          <span className="text-cream">Two agents. One goal. Zero back-and-forth.</span>
        </p>

        {/* CTA buttons */}
        <div ref={ctaRef} className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16">
          <Button
            size="lg"
            asChild
            className="magnetic-btn bg-accent hover:bg-accent-dark text-cream font-medium px-8 py-6 text-base rounded-full group transition-all duration-300"
          >
            <Link href="/signin">
              Get Started
              <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
            </Link>
          </Button>
          <Button
            variant="outline"
            size="lg"
            asChild
            className="magnetic-btn border-border-light hover:border-cream/30 hover:bg-cream/5 text-cream px-8 py-6 text-base rounded-full transition-all duration-300"
          >
            <Link href="#how-it-works">
              See How It Works
            </Link>
          </Button>
        </div>

        {/* Trust badges */}
        <div
          ref={badgesRef}
          className="flex flex-wrap items-center justify-center gap-x-8 gap-y-3 text-sm text-foreground-muted"
        >
          {["Deterministic", "Human-in-the-Loop", "Auditable", "GDPR Ready"].map((badge) => (
            <div key={badge} className="flex items-center gap-2">
              <span className="w-1 h-1 rounded-full bg-accent" />
              <span>{badge}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Scroll indicator */}
      <div
        ref={scrollIndicatorRef}
        className="absolute bottom-12 left-1/2 -translate-x-1/2 flex flex-col items-center gap-3 text-foreground-muted"
      >
        <span className="text-xs font-mono tracking-widest uppercase">Scroll</span>
        <div className="scroll-line w-px h-8 bg-gradient-to-b from-accent to-transparent" />
      </div>
    </section>
  )
}
