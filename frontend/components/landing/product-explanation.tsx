"use client"

import { fadeSlideUpVariants, itemVariants, staggerContainerVariants, useScrollAnimation } from "@/hooks/use-framer-animations"
import { motion, useScroll, useSpring, useTransform } from "framer-motion"
import { Bot, CheckCircle, Users } from "lucide-react"
import { useEffect, useRef, useState } from "react"

export function ProductExplanation() {
  const { ref: sectionRef, isInView } = useScrollAnimation()
  const parallaxRef = useRef<HTMLDivElement>(null)
  const [activeCardIndex, setActiveCardIndex] = useState<number | null>(null)
  
  useEffect(() => {
    // Framer Motion wave effect
    if (isInView) {
      const t1 = setTimeout(() => setActiveCardIndex(0), 800)
      const t2 = setTimeout(() => setActiveCardIndex(1), 2000)
      const t3 = setTimeout(() => setActiveCardIndex(2), 3200)
      const t4 = setTimeout(() => setActiveCardIndex(null), 4400)
      
      return () => { 
        clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); clearTimeout(t4);
      }
    }
  }, [isInView])

  const { scrollYProgress } = useScroll({
    target: parallaxRef,
    offset: ["start end", "end start"],
  })

  const y = useSpring(useTransform(scrollYProgress, [0, 1], [50, -50]), {
    stiffness: 100,
    damping: 30,
  })

  return (
    <section ref={parallaxRef} className="py-16 lg:py-24 bg-muted/20 relative overflow-hidden">
      {/* Parallax background elements */}
      <motion.div
        className="absolute inset-0 pointer-events-none"
        style={{ y }}
      >
        <div className="absolute top-20 left-10 w-64 h-64 bg-foreground/[0.02] rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-foreground/[0.02] rounded-full blur-3xl" />
      </motion.div>
      <div className="mx-auto max-w-7xl px-6">
        <motion.div
          ref={sectionRef}
          className="max-w-[85rem] mx-auto text-center"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-6">
            The Platform
          </p>
          <h2 className="text-4xl sm:text-5xl lg:text-6xl font-semibold tracking-tight text-foreground mb-8 leading-[1.1]">
            Personal Agent Coordination Infrastructure
          </h2>
          <p className="text-xl lg:text-2xl text-muted-foreground leading-relaxed max-w-3xl mx-auto">
            CoAgent4U is a coordination layer where personal agents represent users and collaborate to manage commitments across people and tools.
          </p>
        </motion.div>

        {/* Three pillars */}
        <motion.div
          className="mt-10 lg:mt-14 grid sm:grid-cols-3 gap-10 lg:gap-16"
          variants={staggerContainerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <motion.div
            className={`text-center p-8 rounded-3xl bg-background border transition-all duration-300 group ${
              activeCardIndex === 0 
                ? "scale-[1.03] shadow-[0_20px_40px_rgba(0,0,0,0.1)] border-foreground/20" 
                : "border-border/40"
            }`}
            variants={itemVariants}
            whileHover={{
              scale: 1.03,
              boxShadow: "0 20px 40px rgba(0, 0, 0, 0.1)",
              borderColor: "hsl(var(--foreground) / 0.2)"
            }}
            transition={{ duration: 0.25 }}
          >
            <motion.div
              className={`w-16 h-16 rounded-2xl border border-border/60 flex items-center justify-center mx-auto mb-7 transition-colors duration-300 group-hover:bg-foreground/5 ${
                activeCardIndex === 0 ? "bg-foreground/5" : "bg-muted"
              }`}
              animate={{ y: [0, -8, 0] }}
              transition={{ repeat: Infinity, duration: 4, ease: "easeInOut", delay: 0 }}
            >
              <Bot className={`w-8 h-8 transition-all duration-300 group-hover:scale-110 group-hover:rotate-6 ${
                activeCardIndex === 0 
                  ? "scale-110 rotate-6 text-blue-500" 
                  : "text-foreground group-hover:text-blue-500"
              }`} />
            </motion.div>
            <h3 className="text-xl font-semibold text-foreground mb-4">
              Agent Coordination
            </h3>
            <p className="text-muted-foreground text-base leading-relaxed">
              Personal agents understand your commitments, analyze availability, and coordinate with other agents to manage your time.
            </p>
          </motion.div>

          <motion.div
            className={`text-center p-8 rounded-3xl bg-background border transition-all duration-300 group ${
              activeCardIndex === 1 
                ? "scale-[1.03] shadow-[0_20px_40px_rgba(0,0,0,0.1)] border-foreground/20" 
                : "border-border/40"
            }`}
            variants={itemVariants}
            whileHover={{
              scale: 1.03,
              boxShadow: "0 20px 40px rgba(0, 0, 0, 0.1)",
              borderColor: "hsl(var(--foreground) / 0.2)"
            }}
            transition={{ duration: 0.25 }}
          >
            <motion.div
              className={`w-16 h-16 rounded-2xl border border-border/60 flex items-center justify-center mx-auto mb-7 transition-colors duration-300 group-hover:bg-foreground/5 ${
                activeCardIndex === 1 ? "bg-foreground/5" : "bg-muted"
              }`}
              animate={{ y: [0, -8, 0] }}
              transition={{ repeat: Infinity, duration: 4, ease: "easeInOut", delay: 0.5 }}
              whileHover={{ rotate: [0, -10, 10, -10, 10, 0], transition: { duration: 0.5, repeat: Infinity } }}
            >
              <Users className={`w-8 h-8 transition-all duration-300 group-hover:scale-110 ${
                activeCardIndex === 1 
                  ? "scale-110 text-amber-500" 
                  : "text-foreground group-hover:text-amber-500"
              }`} />
            </motion.div>
            <h3 className="text-xl font-semibold text-foreground mb-4">
              Commitment Management
            </h3>
            <p className="text-muted-foreground text-base leading-relaxed">
              Manage scheduling, events, time conflicts, work sessions, team availability, and time windows through your personal agent.
            </p>
          </motion.div>

          <motion.div
            className={`text-center p-8 rounded-3xl bg-background border transition-all duration-300 group ${
              activeCardIndex === 2 
                ? "scale-[1.03] shadow-[0_20px_40px_rgba(0,0,0,0.1)] border-foreground/20" 
                : "border-border/40"
            }`}
            variants={itemVariants}
            whileHover={{
              scale: 1.03,
              boxShadow: "0 20px 40px rgba(0, 0, 0, 0.1)",
              borderColor: "hsl(var(--foreground) / 0.2)"
            }}
            transition={{ duration: 0.25 }}
          >
            <motion.div
              className={`w-16 h-16 rounded-2xl border border-border/60 flex items-center justify-center mx-auto mb-7 transition-colors duration-300 group-hover:bg-foreground/5 ${
                activeCardIndex === 2 ? "bg-foreground/5" : "bg-muted"
              }`}
              animate={{ y: [0, -8, 0] }}
              transition={{ repeat: Infinity, duration: 4, ease: "easeInOut", delay: 1.0 }}
            >
              <CheckCircle className={`w-8 h-8 transition-all duration-300 group-hover:scale-110 group-hover:rotate-12 ${
                activeCardIndex === 2 
                  ? "scale-110 rotate-12 text-green-500" 
                  : "text-foreground group-hover:text-green-500"
              }`} />
            </motion.div>
            <h3 className="text-xl font-semibold text-foreground mb-4">
              Human-in-the-Loop Approvals
            </h3>
            <p className="text-muted-foreground text-base leading-relaxed">
              Users remain in control through approvals. Personal agents propose actions, humans approve before execution.
            </p>
          </motion.div>
        </motion.div>
      </div>
    </section>
  )
}
