"use client"

import { Calendar, ListChecks, AlertCircle, CheckSquare, Users, History } from "lucide-react"
import { motion } from "framer-motion"
import { useEffect, useState } from "react"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const capabilities = [
  {
    icon: Calendar,
    title: "View My Schedule",
    description: "Access your calendar instantly. Your agent retrieves commitments directly from Google Calendar.",
    colorText: "text-blue-500",
  },
  {
    icon: ListChecks,
    title: "Manage Commitments",
    description: "Create and organize events, meetings, and time blocks through natural language commands.",
    colorText: "text-purple-500",
  },
  {
    icon: AlertCircle,
    title: "Detect Conflicts",
    description: "Automatic conflict detection across all your commitments. Never double-book or miss overlaps.",
    colorText: "text-rose-500",
  },
  {
    icon: Users,
    title: "Coordinate With Other Agents",
    description: "Your agent communicates with other users' agents to find common availability automatically.",
    colorText: "text-amber-500",
  },
  {
    icon: CheckSquare,
    title: "Request Human Approval",
    description: "Every coordination proposal requires your explicit approval before any action is taken.",
    colorText: "text-emerald-500",
  },
  {
    icon: History,
    title: "Maintain History",
    description: "Full coordination history and agent activity logs. Track every agent interaction and decision.",
    colorText: "text-indigo-500",
  },
]

export function AgentCapabilities() {
  const { ref: sectionRef, isInView } = useScrollAnimation()
  const [activeCardIndex, setActiveCardIndex] = useState<number | null>(null)

  useEffect(() => {
    if (isInView) {
      // Smooth left-to-right highlight sequence like a "wave"
      const timeouts = capabilities.map((_, i) => setTimeout(() => setActiveCardIndex(i), 800 + i * 1200))
      const endTimeout = setTimeout(() => setActiveCardIndex(null), 800 + capabilities.length * 1200)

      return () => {
        timeouts.forEach(clearTimeout)
        clearTimeout(endTimeout)
      }
    }
  }, [isInView])

  return (
    <section id="capabilities" className="py-16 lg:py-20" ref={sectionRef}>
      <div className="mx-auto max-w-7xl px-6">
        {/* Section header */}
        <motion.div 
          className="max-w-3xl mb-12"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-4">
            Capabilities
          </p>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-foreground mb-4 leading-[1.1]">
            Your agent, your rules
          </h2>
          <p className="text-lg lg:text-xl text-muted-foreground leading-relaxed">
            Each user receives a personal agent that can perform these core actions on your behalf.
          </p>
        </motion.div>

        {/* Capability cards */}
        <motion.div 
          className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8"
          variants={staggerContainerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          {capabilities.map((capability, index) => (
            <motion.div
              key={capability.title}
              className={`text-center p-8 rounded-3xl bg-background border transition-all duration-300 group ${
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
                className={`w-16 h-16 rounded-2xl border border-border/60 flex items-center justify-center mx-auto mb-7 transition-colors duration-300 group-hover:bg-foreground/5 ${
                  activeCardIndex === index ? "bg-foreground/5" : "bg-muted"
                }`}
                animate={{ y: [0, -8, 0] }}
                transition={{ repeat: Infinity, duration: 4, ease: "easeInOut", delay: index * 0.5 }}
              >
                <capability.icon className={`w-8 h-8 transition-all duration-300 group-hover:scale-110 ${capability.colorText} ${
                  activeCardIndex === index ? "scale-110" : ""
                }`} />
              </motion.div>
              <h3 className="text-xl font-semibold text-foreground mb-4">
                {capability.title}
              </h3>
              <p className="text-muted-foreground text-base leading-relaxed">
                {capability.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
