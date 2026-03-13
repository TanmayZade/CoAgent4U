"use client"

import Link from "next/link"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import { motion } from "framer-motion"
import { useScrollAnimation } from "@/hooks/use-framer-animations"
import { FloatingIcons } from "@/components/landing/floating-icons"

export function HeroSection() {
  const { ref: sectionRef, isInView } = useScrollAnimation()

  // Hero timeline: logo → headline → subtitle → CTA buttons
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.12,
        delayChildren: 0.15,
      },
    },
  }

  const itemVariants = {
    hidden: { opacity: 0, y: 25 },
    visible: {
      opacity: 1,
      y: 0,
      transition: {
        duration: 0.7,
        ease: "easeOut",
      },
    },
  }

  return (
    <section ref={sectionRef} className="relative min-h-screen flex flex-col justify-center overflow-hidden pt-24 pb-16">
      {/* Floating Icons Background - Antigravity style */}
      <FloatingIcons className="-z-10" />

      {/* Subtle gradient background */}
      <div className="absolute inset-0 -z-20">
        <div className="absolute top-1/4 left-1/4 w-[800px] h-[800px] bg-gradient-to-br from-muted/40 to-transparent rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-[600px] h-[600px] bg-gradient-to-tl from-muted/30 to-transparent rounded-full blur-3xl" />
      </div>
      
      {/* Radial gradient overlay for depth */}
      <div className="absolute inset-0 -z-15 bg-[radial-gradient(ellipse_at_center,_transparent_0%,_var(--background)_70%)]" />

      <div className="mx-auto max-w-7xl px-6 w-full">
        <motion.div 
          className="mx-auto max-w-5xl text-center"
          variants={containerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          {/* Logo + Brand */}
          <motion.div 
            className="flex items-center justify-center gap-5 mb-12"
            variants={itemVariants}
          >
            <Image 
              src="/images/logo.png" 
              alt="CoAgent4U Logo" 
              width={72} 
              height={72}
              className="drop-shadow-md"
              style={{ width: '72px', height: '72px' }}
            />
            <span className="text-3xl font-serif font-medium text-foreground tracking-tight italic">
              CoAgent4U
            </span>
          </motion.div>

          {/* Headline - Large, bold, centered with proper word wrapping */}
          <motion.h1 
            className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-semibold tracking-tight text-foreground leading-[1.1] max-w-4xl mx-auto"
            variants={itemVariants}
          >
            Your Personal Agent That Coordinates Your Time
          </motion.h1>

          {/* Subheadline */}
          <motion.p 
            className="mt-8 text-lg lg:text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto"
            variants={itemVariants}
          >
            A coordination platform where personal agents represent users and collaborate to manage commitments, schedules, and shared time.
          </motion.p>

          {/* CTAs */}
          <motion.div 
            className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4"
            variants={{
              hidden: { opacity: 0 },
              visible: {
                opacity: 1,
                transition: {
                  staggerChildren: 0.15,
                },
              },
            }}
          >
            <motion.div variants={itemVariants}>
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
            </motion.div>
            <motion.div variants={itemVariants}>
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
            </motion.div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  )
}
