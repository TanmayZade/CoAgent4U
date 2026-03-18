"use client"

import { Zap, GitBranch, UserCheck, ShieldAlert, Users } from "lucide-react"
import { motion } from "framer-motion"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const benefits = [
  {
    icon: Zap,
    title: "Reduced Coordination Friction",
    description: "Eliminate constant back-and-forth communication. Your agent handles all the coordination while you focus on work.",
  },
  {
    icon: Users,
    title: "Personal Agents Representing Users",
    description: "Each user has a personal agent that understands their commitments and coordinates on their behalf.",
  },
  {
    icon: GitBranch,
    title: "Deterministic Coordination Workflows",
    description: "Predictable outcomes based on explicit rules. No AI hallucinations or unexpected behaviors.",
  },
  {
    icon: ShieldAlert,
    title: "Conflict-Aware Scheduling",
    description: "Automatic detection of scheduling conflicts before they become problems across all commitments.",
  },
  {
    icon: UserCheck,
    title: "Transparent Approval Control",
    description: "Every coordination action requires your explicit approval. Nothing happens without consent.",
  },
]

export function PlatformBenefits() {
  const { ref: sectionRef, isInView } = useScrollAnimation()

  return (
    <section className="py-24 lg:py-32" ref={sectionRef}>
      <div className="mx-auto max-w-7xl px-6">
        {/* Section header */}
        <motion.div 
          className="max-w-2xl mx-auto text-center mb-16"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">
            Platform Benefits
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            Built for how you actually work
          </h2>
          <p className="text-muted-foreground text-lg">
            CoAgent4U removes coordination friction while keeping you in complete control of your commitments.
          </p>
        </motion.div>

        {/* Benefits grid */}
        <motion.div 
          className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8"
          variants={staggerContainerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          {benefits.map((benefit, index) => (
            <motion.div
              key={benefit.title}
              className={`p-6 rounded-2xl transition-all duration-300 cursor-pointer group ${
                index === benefits.length - 1 && benefits.length % 3 !== 0
                  ? "sm:col-span-2 lg:col-span-1"
                  : ""
              }`}
              variants={itemVariants}
              whileHover={{ 
                scale: 1.03,
                boxShadow: "0 16px 32px rgba(0, 0, 0, 0.06)",
                backgroundColor: "hsl(var(--muted) / 0.4)"
              }}
              transition={{ duration: 0.25 }}
            >
              <div className="w-12 h-12 rounded-2xl bg-muted flex items-center justify-center mb-5 transition-all duration-300 group-hover:scale-110 group-hover:bg-foreground/10">
                <benefit.icon className="w-6 h-6 text-foreground transition-colors duration-300" />
              </div>
              <h3 className="text-lg font-semibold text-foreground mb-2 transition-colors duration-300">
                {benefit.title}
              </h3>
              <p className="text-muted-foreground leading-relaxed">
                {benefit.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
