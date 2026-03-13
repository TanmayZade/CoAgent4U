"use client"

import { Bot, Users, CheckCircle } from "lucide-react"
import { motion } from "framer-motion"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

export function ProductExplanation() {
  const { ref: sectionRef, isInView } = useScrollAnimation()

  return (
    <section className="py-32 lg:py-40 bg-muted/20">
      <div className="mx-auto max-w-6xl px-6">
        <motion.div 
          ref={sectionRef}
          className="max-w-4xl mx-auto text-center"
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
          className="mt-20 lg:mt-28 grid sm:grid-cols-3 gap-10 lg:gap-16"
          variants={staggerContainerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <motion.div className="text-center card-hover p-8 rounded-3xl bg-background border border-border/40" variants={itemVariants}>
            <div className="w-16 h-16 rounded-2xl bg-muted border border-border/60 flex items-center justify-center mx-auto mb-7 transition-all duration-500 hover:scale-110 hover:rotate-3">
              <Bot className="w-8 h-8 text-foreground" />
            </div>
            <h3 className="text-xl font-semibold text-foreground mb-4">
              Agent Coordination
            </h3>
            <p className="text-muted-foreground text-base leading-relaxed">
              Personal agents understand your commitments, analyze availability, and coordinate with other agents to manage your time.
            </p>
          </motion.div>

          <motion.div className="text-center card-hover p-8 rounded-3xl bg-background border border-border/40" variants={itemVariants}>
            <div className="w-16 h-16 rounded-2xl bg-muted border border-border/60 flex items-center justify-center mx-auto mb-7 transition-all duration-500 hover:scale-110 hover:rotate-3">
              <Users className="w-8 h-8 text-foreground" />
            </div>
            <h3 className="text-xl font-semibold text-foreground mb-4">
              Commitment Management
            </h3>
            <p className="text-muted-foreground text-base leading-relaxed">
              Manage scheduling, events, time conflicts, work sessions, team availability, and time windows through your personal agent.
            </p>
          </motion.div>

          <motion.div className="text-center card-hover p-8 rounded-3xl bg-background border border-border/40" variants={itemVariants}>
            <div className="w-16 h-16 rounded-2xl bg-muted border border-border/60 flex items-center justify-center mx-auto mb-7 transition-all duration-500 hover:scale-110 hover:rotate-3">
              <CheckCircle className="w-8 h-8 text-foreground" />
            </div>
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
