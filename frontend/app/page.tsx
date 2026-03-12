import { NavHeader } from "@/components/layout/nav-header"
import { HeroSection } from "@/components/landing/hero-section"
import { FeaturesSection } from "@/components/landing/features-section"
import { A2AFlowSection } from "@/components/landing/a2a-flow-section"
import { MetricsSection } from "@/components/landing/metrics-section"
import { Footer } from "@/components/landing/footer"
import { GSAPProvider } from "@/components/providers/gsap-provider"

export default function LandingPage() {
  return (
    <GSAPProvider>
      <main className="min-h-screen bg-charcoal overflow-hidden">
        <NavHeader />
        <HeroSection />
        <FeaturesSection />
        <A2AFlowSection />
        <MetricsSection />
        <Footer />
      </main>
    </GSAPProvider>
  )
}
