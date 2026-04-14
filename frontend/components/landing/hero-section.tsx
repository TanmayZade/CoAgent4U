"use client"

import { GridScan } from "@/components/ui/grid-scan"
import gsap from "gsap"
import { ArrowRight, Lock } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { useEffect, useRef, useState } from "react"
import { toast } from "sonner"

function HeroDisabledCTA({ refProp, style }: { refProp: React.Ref<HTMLDivElement>, style?: React.CSSProperties }) {
  const [show, setShow] = useState(false)
  return (
    <div ref={refProp} style={style} className="relative inline-flex" onMouseEnter={() => setShow(true)} onMouseLeave={() => setShow(false)}>
      <button
        disabled
        className="inline-flex items-center justify-center h-13 px-8 text-base font-medium rounded-full bg-foreground/50 text-background/70 shadow-lg cursor-not-allowed select-none gap-2"
      >
        <Lock className="h-4 w-4" />
        Get Started
        <ArrowRight className="ml-1 h-5 w-5" />
      </button>
      {show && (
        <div className="absolute left-1/2 -translate-x-1/2 top-full mt-2 z-50 w-72 rounded-xl border border-border/60 bg-background/95 backdrop-blur-md p-3 shadow-xl text-sm">
          <div className="flex items-start gap-2">
            <Lock className="h-4 w-4 text-amber-500 mt-0.5 shrink-0" />
            <span className="text-muted-foreground leading-snug">
              Available to <span className="font-semibold text-foreground">test users only</span>. Interested? Contact{" "}
              <a href="mailto:easychat148@gmail.com" className="text-primary underline underline-offset-2 hover:text-primary/80 break-all">easychat148@gmail.com</a>
            </span>
          </div>
        </div>
      )}
    </div>
  )
}

export function HeroSection() {
  const containerRef = useRef<HTMLDivElement>(null)
  const logoRef = useRef<HTMLDivElement>(null)
  const logoImageRef = useRef<HTMLDivElement>(null)
  const type1Ref = useRef<HTMLSpanElement>(null)
  const type2Ref = useRef<HTMLSpanElement>(null)
  const cur1Ref = useRef<HTMLSpanElement>(null)
  const cur2Ref = useRef<HTMLSpanElement>(null)
  const subheadlineRef = useRef<HTMLParagraphElement>(null)
  const techBadgesRef = useRef<HTMLDivElement>(null)
  const cta1Ref = useRef<HTMLDivElement>(null)
  const cta2Ref = useRef<HTMLAnchorElement>(null)

  useEffect(() => {
    // Dynamically import TextPlugin to avoid SSR issues
    const run = async () => {
      const { TextPlugin } = await import("gsap/TextPlugin")
      gsap.registerPlugin(TextPlugin)

      // Always play the animation — remove any stale flag
      localStorage.removeItem("introSeen")

      const type1 = type1Ref.current
      const type2 = type2Ref.current
      const cur1 = cur1Ref.current
      const cur2 = cur2Ref.current
      if (!type1 || !type2 || !cur1 || !cur2) return

      gsap.set([type1, type2], { opacity: 1 })
      gsap.set(cur1, { opacity: 1 })
      gsap.set(cur2, { opacity: 0 })

      const HEADLINE_1 = "Your Personal AI Agent"
      const HEADLINE_2 = "Powered by A2A & MCP"
      const CHAR_SPEED = 0.098 // slowed down by another 10%
      const DUR_1 = HEADLINE_1.length * CHAR_SPEED
      const DUR_2 = HEADLINE_2.length * CHAR_SPEED

      const tl = gsap.timeline()

      // Blink cursor 1
      const blink1 = gsap.fromTo(
        cur1,
        { opacity: 1 },
        { opacity: 0, duration: 0.4, repeat: -1, yoyo: true, ease: "none" }
      )

      // Type Line 1
      tl.to(type1, {
        duration: DUR_1,
        text: { value: HEADLINE_1, delimiter: "" },
        ease: "none",
      }, 0)

      // Swap cursors and type Line 2
      tl.call(() => {
        blink1.kill()
        gsap.set(cur1, { display: "none" }) // Hide top cursor instantly
        
        const blink2 = gsap.fromTo(
          cur2,
          { opacity: 1 },
          { opacity: 0, duration: 0.4, repeat: -1, yoyo: true, ease: "none" }
        )
        // Store blink2 safely to kill later
        tl.data = { blink2 }
      }, undefined, DUR_1 + 0.1)

      tl.to(type2, {
        duration: DUR_2,
        text: { value: HEADLINE_2, delimiter: "" },
        ease: "none",
      }, DUR_1 + 0.3)

      const TOTAL_DUR = DUR_1 + 0.3 + DUR_2

      // Stop ending cursor
      tl.add(() => {
        if (tl.data?.blink2) tl.data.blink2.kill()
        gsap.to(cur2, { opacity: 0, duration: 0.3 })
      }, TOTAL_DUR + 0.1)

      // Logo pop-in container
      tl.fromTo(
        logoRef.current,
        { opacity: 0, scale: 0.85, y: 10 },
        { opacity: 1, scale: 1, y: 0, duration: 0.5, ease: "back.out(1.7)" },
        TOTAL_DUR + 0.3
      )

      // Spin the logo asset specifically 360 degrees clockwise once
      if (logoImageRef.current) {
        tl.fromTo(
          logoImageRef.current,
          { rotate: 0 },
          { rotate: 360, duration: 1.2, ease: "power2.inOut" },
          TOTAL_DUR + 0.3
        )
      }

      // Subheadline slide-up
      tl.fromTo(
        subheadlineRef.current,
        { opacity: 0, y: 18 },
        { opacity: 1, y: 0, duration: 0.55, ease: "power2.out" },
        TOTAL_DUR + 0.55
      )

      // Tech badges slide-up
      tl.fromTo(
        techBadgesRef.current,
        { opacity: 0, y: 14 },
        { opacity: 1, y: 0, duration: 0.5, ease: "power2.out" },
        TOTAL_DUR + 0.7
      )

      // CTA buttons with stagger
      tl.fromTo(
        [cta1Ref.current, cta2Ref.current].filter(Boolean),
        { opacity: 0, scale: 0.9, y: 8 },
        { opacity: 1, scale: 1, y: 0, duration: 0.55, ease: "back.out(1.7)", stagger: 0.12 },
        TOTAL_DUR + 0.85
      )

      return () => {
        tl.kill()
        if (blink1) blink1.kill()
      }
    }

    const cleanup = run()
    return () => {
      cleanup.then((fn) => fn?.())
    }
  }, [])

  return (
    <section data-section="hero" className="relative min-h-screen flex flex-col justify-center overflow-hidden pb-16 pt-20">
      {/* GridScan background */}
      <GridScan
        lineThickness={1}
        linesColor="#392e4e"
        scanColor="#ffffff"
        scanOpacity={0.4}
        gridScale={0.1}
        lineStyle="solid"
        scanDirection="pingpong"
        scanGlow={0.5}
        scanSoftness={2}
        scanDuration={2}
        scanDelay={2}
        scanOnClick={false}
        className="absolute inset-0 w-full h-full -z-20"
      />
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_center,_transparent_0%,_var(--background)_70%)]" />

      <div ref={containerRef} className="mx-auto max-w-7xl px-6 w-full">
        <div className="mx-auto max-w-[85rem] text-center">

          {/* Logo + Brand — hidden until animation reveals */}
          <div
            ref={logoRef}
            className="flex items-center justify-center gap-5 mb-12"
            style={{ opacity: 0 }}
          >
            <div ref={logoImageRef} className="flex shrink-0">
              <Image
                src="/images/logo-light.png"
                alt="CoAgent4U Logo"
                width={72}
                height={72}
                className="drop-shadow-md"
                style={{ width: "72px", height: "72px" }}
              />
            </div>
            <span className="text-3xl font-serif font-medium text-foreground tracking-tight italic">
              CoAgent4U
            </span>
          </div>

          {/* Headline — Both lines dynamic and structurally independent */}
          <h1
            className="flex flex-col items-center justify-center text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-semibold tracking-tight text-foreground leading-[1.15] max-w-[85rem] mx-auto"
          >
            <span className="block min-h-[1.2em]">
              <span ref={type1Ref}></span>
              <span ref={cur1Ref} aria-hidden="true" className="inline-block w-[3px] h-[0.85em] bg-foreground align-middle ml-[1px] translate-y-[-0.05em]" />
            </span>
            <span className="block mt-2 lg:mt-4 min-h-[1.2em]">
              <span ref={type2Ref}></span>
              <span ref={cur2Ref} aria-hidden="true" className="inline-block w-[3px] h-[0.85em] bg-foreground align-middle ml-[2px] translate-y-[-0.05em] opacity-0" />
            </span>
          </h1>

          {/* Subheadline */}
          <p
            ref={subheadlineRef}
            className="mt-8 text-lg lg:text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto"
            style={{ opacity: 0 }}
          >
            A privacy-centric personal agent that manages your calendar, tasks, and productivity through MCP tools — and coordinates with other agents via A2A protocol.
          </p>

          {/* Tech Badges */}
          <div
            ref={techBadgesRef}
            className="mt-6 flex flex-wrap items-center justify-center gap-3"
            style={{ opacity: 0 }}
          >
            <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full bg-foreground/5 border border-foreground/10 text-sm font-medium text-foreground/80">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              MCP Protocol
            </span>
            <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full bg-foreground/5 border border-foreground/10 text-sm font-medium text-foreground/80">
              <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
              A2A Protocol
            </span>
            <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full bg-foreground/5 border border-foreground/10 text-sm font-medium text-foreground/80">
              <span className="w-2 h-2 rounded-full bg-purple-500 animate-pulse" />
              LLM-Driven Planning
            </span>
            <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full bg-foreground/5 border border-foreground/10 text-sm font-medium text-foreground/80">
              <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
              32 MCP Tools
            </span>
          </div>

          {/* CTAs */}
          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <HeroDisabledCTA
              refProp={cta1Ref}
              style={{ opacity: 0 }}
            />
            <Link
              ref={cta2Ref}
              href="#architecture"
              className="inline-flex items-center justify-center h-13 px-8 text-base font-medium rounded-full border-2 border-foreground/20 hover:border-foreground/40 hover:bg-muted/50 transition-all duration-300 hover:scale-105"
              style={{ opacity: 0 }}
            >
              View Architecture
            </Link>
          </div>

        </div>
      </div>
    </section>
  )
}
