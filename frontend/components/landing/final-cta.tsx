"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import { motion } from "framer-motion"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants } from "@/hooks/use-framer-animations"

export function FinalCTA() {
  const { ref: sectionRef, isInView } = useScrollAnimation()

  const indicatorVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: {
        duration: 0.5,
        ease: "easeOut",
      },
    },
  }

  return (
    <section className="py-32 lg:py-44" ref={sectionRef}>
      <div className="mx-auto max-w-6xl px-6">
        <motion.div 
          className="max-w-4xl mx-auto text-center"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-8">
            Get Started Today
          </p>
          <h2 className="text-4xl sm:text-5xl lg:text-6xl xl:text-7xl font-semibold tracking-tight text-foreground mb-10 leading-[1.1] text-balance">
            Let Your Personal Agent Coordinate Your Commitments
          </h2>
          <p className="text-xl lg:text-2xl text-muted-foreground mb-14 max-w-2xl mx-auto leading-relaxed">
            Stop wasting time on manual coordination. Let your agent collaborate with others while you focus on what matters.
          </p>

          <motion.div 
            className="flex flex-col sm:flex-row items-center justify-center gap-5"
            variants={{
              hidden: { opacity: 0 },
              visible: {
                opacity: 1,
                transition: {
                  staggerChildren: 0.15,
                },
              },
            }}
            initial="hidden"
            animate={isInView ? "visible" : "hidden"}
          >
            <motion.div variants={indicatorVariants}>
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
            </motion.div>
          </motion.div>

          {/* Trust indicators */}
          <motion.div 
            className="mt-16 flex flex-wrap items-center justify-center gap-x-10 gap-y-5 text-base text-muted-foreground"
            variants={staggerContainerVariants}
            initial="hidden"
            animate={isInView ? "visible" : "hidden"}
          >
            <motion.span className="flex items-center gap-3 transition-colors duration-300 hover:text-foreground" variants={indicatorVariants}>
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              Free to start
            </motion.span>
            <motion.span className="flex items-center gap-3 transition-colors duration-300 hover:text-foreground" variants={indicatorVariants}>
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              No credit card required
            </motion.span>
            <motion.span className="flex items-center gap-3 transition-colors duration-300 hover:text-foreground" variants={indicatorVariants}>
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              Setup in 2 minutes
            </motion.span>
          </motion.div>
        </motion.div>
      </div>
    </section>
  )
}
