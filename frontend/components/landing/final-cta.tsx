"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import { useScrollReveal, useStaggerReveal } from "@/hooks/use-gsap-animations"

export function FinalCTA() {
  const headerRef = useScrollReveal<HTMLDivElement>({ y: 50, duration: 0.9 })
  const indicatorsRef = useStaggerReveal<HTMLDivElement>({ 
    stagger: 0.1, 
    y: 20, 
    duration: 0.5,
    childSelector: "> span"
  })

  return (
    <section className="py-24 lg:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <div ref={headerRef} className="max-w-3xl mx-auto text-center">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-foreground mb-6 text-balance">
            Let Your Personal Agent Coordinate Your Commitments
          </h2>
          <p className="text-lg text-muted-foreground mb-10 max-w-xl mx-auto">
            Stop wasting time on manual coordination. Let your agent collaborate with others while you focus on what matters.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button size="lg" className="h-12 px-8 text-base magnetic-btn interactive-hover group" asChild>
              <Link href="/signin">
                Start Using CoAgent4U
                <ArrowRight className="ml-2 h-4 w-4 transition-transform duration-300 group-hover:translate-x-1" />
              </Link>
            </Button>
          </div>

          {/* Trust indicators */}
          <div ref={indicatorsRef} className="mt-12 flex flex-wrap items-center justify-center gap-x-8 gap-y-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-2 transition-colors duration-300 hover:text-foreground">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
              Free to start
            </span>
            <span className="flex items-center gap-2 transition-colors duration-300 hover:text-foreground">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
              No credit card required
            </span>
            <span className="flex items-center gap-2 transition-colors duration-300 hover:text-foreground">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
              Setup in 2 minutes
            </span>
          </div>
        </div>
      </div>
    </section>
  )
}
