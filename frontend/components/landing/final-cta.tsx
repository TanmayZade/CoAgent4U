"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import { useScrollReveal, useStaggerReveal } from "@/hooks/use-gsap-animations"

export function FinalCTA() {
  const headerRef = useScrollReveal<HTMLDivElement>({ y: 60, duration: 1 })
  const indicatorsRef = useStaggerReveal<HTMLDivElement>({ 
    stagger: 0.15, 
    y: 30, 
    duration: 0.6,
    childSelector: "> span"
  })

  return (
    <section className="py-32 lg:py-44">
      <div className="mx-auto max-w-6xl px-6">
        <div ref={headerRef} className="max-w-4xl mx-auto text-center">
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-8">
            Get Started Today
          </p>
          <h2 className="text-4xl sm:text-5xl lg:text-6xl xl:text-7xl font-semibold tracking-tight text-foreground mb-10 leading-[1.1] text-balance">
            Let Your Personal Agent Coordinate Your Commitments
          </h2>
          <p className="text-xl lg:text-2xl text-muted-foreground mb-14 max-w-2xl mx-auto leading-relaxed">
            Stop wasting time on manual coordination. Let your agent collaborate with others while you focus on what matters.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-5">
            <Button 
              size="lg" 
              className="h-14 px-10 text-lg font-medium rounded-full bg-foreground text-background hover:bg-foreground/90 shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105 group" 
              asChild
            >
              <Link href="/signin">
                Start Using CoAgent4U
                <ArrowRight className="ml-2 h-5 w-5 transition-transform duration-300 group-hover:translate-x-1" />
              </Link>
            </Button>
          </div>

          {/* Trust indicators */}
          <div ref={indicatorsRef} className="mt-16 flex flex-wrap items-center justify-center gap-x-10 gap-y-5 text-base text-muted-foreground">
            <span className="flex items-center gap-3 transition-colors duration-300 hover:text-foreground">
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              Free to start
            </span>
            <span className="flex items-center gap-3 transition-colors duration-300 hover:text-foreground">
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              No credit card required
            </span>
            <span className="flex items-center gap-3 transition-colors duration-300 hover:text-foreground">
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              Setup in 2 minutes
            </span>
          </div>
        </div>
      </div>
    </section>
  )
}
