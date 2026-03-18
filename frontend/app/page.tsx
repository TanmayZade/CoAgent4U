"use client"

import { useEffect } from "react"
import { Navbar } from "@/components/landing/navbar"
import { HeroSection } from "@/components/landing/hero-section"
import { AgentPreview } from "@/components/landing/agent-preview"
import { ProductExplanation } from "@/components/landing/product-explanation"
import { AgentCapabilities } from "@/components/landing/agent-capabilities"
import { CoordinationSection } from "@/components/landing/coordination-section"
import { UseCaseExamples } from "@/components/landing/use-case-examples"
import { ApprovalSystem } from "@/components/landing/approval-system"
import { PlatformBenefits } from "@/components/landing/platform-benefits"
import { SecurityTrust } from "@/components/landing/security-trust"
import { FinalCTA } from "@/components/landing/final-cta"
import { Footer } from "@/components/landing/footer"
import { ScrollProgressBar } from "@/components/ui/parallax-wrapper"

import { ScaleInScroll } from "@/components/landing/scale-in-scroll"
import { SmoothScroll } from "@/components/landing/smooth-scroll"

export default function LandingPage() {
  
  useEffect(() => {
    // Aggressively force light theme strictly on the landing page
    const htmlEl = document.documentElement
    if (htmlEl.classList.contains("dark")) {
      htmlEl.classList.remove("dark")
      htmlEl.classList.add("light")
      htmlEl.style.colorScheme = "light"
    }

    // Observers to block `next-themes` from re-adding the dark class while on the landing page
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === "class" && htmlEl.classList.contains("dark")) {
          htmlEl.classList.remove("dark")
          htmlEl.classList.add("light")
        }
      })
    })

    observer.observe(htmlEl, { attributes: true })

    return () => observer.disconnect()
  }, [])

  return (
    <SmoothScroll>
      <main className="min-h-screen bg-background text-foreground light overflow-hidden">
        <ScrollProgressBar />
        <Navbar />
        <HeroSection />
        <AgentPreview />
        
        <div className="flex flex-col gap-24 lg:gap-32 mt-24 pb-24">
          <ScaleInScroll><ProductExplanation /></ScaleInScroll>
          <ScaleInScroll><AgentCapabilities /></ScaleInScroll>
          <ScaleInScroll><CoordinationSection /></ScaleInScroll>
          <ScaleInScroll><UseCaseExamples /></ScaleInScroll>
          {/* <ScaleInScroll><ApprovalSystem /></ScaleInScroll> */}
          <ScaleInScroll><PlatformBenefits /></ScaleInScroll>
          <ScaleInScroll><SecurityTrust /></ScaleInScroll>
          <ScaleInScroll><FinalCTA /></ScaleInScroll>
        </div>

        <Footer />
      </main>
    </SmoothScroll>
  )
}
