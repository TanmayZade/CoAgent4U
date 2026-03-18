"use client"

import { Zap, GitBranch, UserCheck, ShieldAlert, Users } from "lucide-react"
import { motion } from "framer-motion"
import { useEffect, useState } from "react"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const benefits = [
  {
    icon: Zap,
    title: "Reduced Coordination Friction",
    description: "Eliminate constant back-and-forth communication. Your agent handles all the coordination while you focus on work.",
    colorText: "text-amber-500",
    hoverColorText: "group-hover:text-amber-500",
  },
  {
    icon: Users,
    title: "Personal Agents Representing Users",
    description: "Each user has a personal agent that understands their commitments and coordinates on their behalf.",
    colorText: "text-blue-500",
    hoverColorText: "group-hover:text-blue-500",
  },
  {
    icon: GitBranch,
    title: "Deterministic Coordination Workflows",
    description: "Predictable outcomes based on explicit rules. No AI hallucinations or unexpected behaviors.",
    colorText: "text-purple-500",
    hoverColorText: "group-hover:text-purple-500",
  },
  {
    icon: ShieldAlert,
    title: "Conflict-Aware Scheduling",
    description: "Automatic detection of scheduling conflicts before they become problems across all commitments.",
    colorText: "text-rose-500",
    hoverColorText: "group-hover:text-rose-500",
  },
  {
    icon: UserCheck,
    title: "Transparent Approval Control",
    description: "Every coordination action requires your explicit approval. Nothing happens without consent.",
    colorText: "text-emerald-500",
    hoverColorText: "group-hover:text-emerald-500",
  },
]

export function PlatformBenefits() {
  const { ref: sectionRef, isInView } = useScrollAnimation()
  const [activeCardIndex, setActiveCardIndex] = useState<number | null>(null)

  useEffect(() => {
    if (isInView) {
      // Smooth left-to-right highlight sequence like a "wave"
      const timeouts = benefits.map((_, i) => setTimeout(() => setActiveCardIndex(i), 800 + i * 1200))
      const endTimeout = setTimeout(() => setActiveCardIndex(null), 800 + benefits.length * 1200)

      return () => {
        timeouts.forEach(clearTimeout)
        clearTimeout(endTimeout)
      }
    }
  }, [isInView])

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
              className={`p-6 rounded-2xl bg-background border transition-all duration-300 cursor-pointer group ${
                index === benefits.length - 1 && benefits.length % 3 !== 0
                  ? "sm:col-span-2 lg:col-span-1"
                  : ""
              } ${
                activeCardIndex === index 
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
                className={`w-14 h-14 rounded-2xl border border-border/60 flex items-center justify-center mb-6 transition-colors duration-300 group-hover:bg-foreground/5 ${
                  activeCardIndex === index ? "bg-foreground/5" : "bg-muted"
                }`}
                animate={{ y: [0, -8, 0] }}
                transition={{ repeat: Infinity, duration: 4, ease: "easeInOut", delay: index * 0.5 }}
              >
                <benefit.icon className={`w-7 h-7 transition-all duration-300 group-hover:scale-110 ${
                  activeCardIndex === index 
                    ? `scale-110 ${benefit.colorText}` 
                    : `text-foreground ${benefit.hoverColorText}`
                }`} />
              </motion.div>
              <h3 className="text-lg font-semibold text-foreground mb-3 transition-colors duration-300">
                {benefit.title}
              </h3>
              <p className="text-muted-foreground text-[15px] leading-relaxed">
                {benefit.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
