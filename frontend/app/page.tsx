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

export default function LandingPage() {
  return (
    <main className="min-h-screen bg-background">
      <ScrollProgressBar />
      <Navbar />
      <HeroSection />
      <AgentPreview />
      <ProductExplanation />
      <AgentCapabilities />
      <CoordinationSection />
      <UseCaseExamples />
      <ApprovalSystem />
      <PlatformBenefits />
      <SecurityTrust />
      <FinalCTA />
      <Footer />
    </main>
  )
}
