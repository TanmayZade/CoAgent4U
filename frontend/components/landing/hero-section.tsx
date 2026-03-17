"use client"

import { useEffect, useRef } from "react"
import Link from "next/link"
import Image from "next/image"
import { ArrowRight } from "lucide-react"
import gsap from "gsap"
import { GridScan } from "@/components/ui/grid-scan"

const HEADLINE =
  "Your Personal Agent That Assists You and Collaborates with Other User's Agent"

export function HeroSection() {
  const containerRef = useRef<HTMLDivElement>(null)
  const logoRef = useRef<HTMLDivElement>(null)
  const cursorRef = useRef<HTMLSpanElement>(null)
  const headlineRef = useRef<HTMLHeadingElement>(null)
  const subheadlineRef = useRef<HTMLParagraphElement>(null)
  const cta1Ref = useRef<HTMLAnchorElement>(null)
  const cta2Ref = useRef<HTMLAnchorElement>(null)

  useEffect(() => {
    // Dynamically import TextPlugin to avoid SSR issues
    const run = async () => {
      const { TextPlugin } = await import("gsap/TextPlugin")
      gsap.registerPlugin(TextPlugin)

      // Always play the animation — remove any stale flag
      localStorage.removeItem("introSeen")

      const h1 = headlineRef.current
      const cursor = cursorRef.current
      if (!h1 || !cursor) return

      // Make h1 visible but empty; cursor sits inline after text
      gsap.set(h1, { opacity: 1 })
      gsap.set(cursor, { opacity: 1 })

      const CHAR_SPEED = 0.10 // seconds per character
      const DURATION = HEADLINE.length * CHAR_SPEED

      const tl = gsap.timeline()

      // Blinking cursor during typing
      const blinkTween = gsap.fromTo(
        cursor,
        { opacity: 1 },
        { opacity: 0, duration: 0.7, repeat: -1, yoyo: true, ease: "none" }
      )

      // Type text into the h1 span (cursor is a sibling <span> — it auto-follows inline flow)
      tl.to(h1, {
        duration: DURATION,
        text: { value: HEADLINE, delimiter: "" },
        ease: "none",
      }, 0)

      // After typing: stop blink, hide cursor
      tl.add(() => {
        blinkTween.kill()
        gsap.to(cursor, { opacity: 0, duration: 0.3 })
      }, DURATION + 0.1)

      // Logo pop-in
      tl.fromTo(
        logoRef.current,
        { opacity: 0, scale: 0.85, y: 10 },
        { opacity: 1, scale: 1, y: 0, duration: 0.5, ease: "back.out(1.7)" },
        DURATION + 0.3
      )

      // Subheadline slide-up
      tl.fromTo(
        subheadlineRef.current,
        { opacity: 0, y: 18 },
        { opacity: 1, y: 0, duration: 0.55, ease: "power2.out" },
        DURATION + 0.55
      )

      // CTA buttons with stagger
      tl.fromTo(
        [cta1Ref.current, cta2Ref.current].filter(Boolean),
        { opacity: 0, scale: 0.9, y: 8 },
        { opacity: 1, scale: 1, y: 0, duration: 0.55, ease: "back.out(1.7)", stagger: 0.12 },
        DURATION + 0.75
      )

      return () => {
        tl.kill()
        blinkTween.kill()
      }
    }

    const cleanup = run()
    return () => {
      cleanup.then((fn) => fn?.())
    }
  }, [])

  return (
    <section data-section="hero" className="relative min-h-screen flex flex-col justify-start overflow-hidden pt-32 pb-16">
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
        <div className="mx-auto max-w-5xl text-center">

          {/* Logo + Brand — hidden until animation reveals */}
          <div
            ref={logoRef}
            className="flex items-center justify-center gap-5 mb-12"
            style={{ opacity: 0 }}
          >
            <Image
              src="/images/logo-light.png"
              alt="CoAgent4U Logo"
              width={72}
              height={72}
              className="drop-shadow-md"
              style={{ width: "72px", height: "72px" }}
            />
            <span className="text-3xl font-serif font-medium text-foreground tracking-tight italic">
              CoAgent4U
            </span>
          </div>

          {/* Headline — inline cursor follows typed characters naturally */}
          <h1
            ref={headlineRef}
            className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-semibold tracking-tight text-foreground leading-[1.15] max-w-4xl mx-auto min-h-[4rem]"
            style={{ opacity: 0, whiteSpace: "pre-wrap", wordWrap: "break-word", wordBreak: "break-word", overflowWrap: "break-word" }}
          >
            {/* cursor is an inline sibling so it sits right after last typed char */}
            <span ref={cursorRef} aria-hidden="true" className="inline-block w-[3px] h-[0.85em] bg-foreground align-middle ml-0.5 translate-y-[-0.05em]" />
          </h1>

          {/* Subheadline */}
          <p
            ref={subheadlineRef}
            className="mt-8 text-lg lg:text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto"
            style={{ opacity: 0 }}
          >
            The Coordination Platform for Personal Agents
          </p>

          {/* CTAs */}
          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              ref={cta1Ref}
              href="/signin"
              className="inline-flex items-center justify-center h-13 px-8 text-base font-medium rounded-full bg-foreground text-background hover:bg-foreground/90 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105"
              style={{ opacity: 0 }}
            >
              Get Started
              <ArrowRight className="ml-2 h-5 w-5" />
            </Link>
            <Link
              ref={cta2Ref}
              href="#use-cases"
              className="inline-flex items-center justify-center h-13 px-8 text-base font-medium rounded-full border-2 border-foreground/20 hover:border-foreground/40 hover:bg-muted/50 transition-all duration-300 hover:scale-105"
              style={{ opacity: 0 }}
            >
              View Demo
            </Link>
          </div>

        </div>
      </div>
    </section>
  )
}
