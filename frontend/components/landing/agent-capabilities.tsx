"use client"

import { Calendar, ListChecks, AlertCircle, CheckSquare, Users, History } from "lucide-react"
import { motion } from "framer-motion"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const capabilities = [
  {
    icon: Calendar,
    title: "View My Schedule",
    description: "Access your calendar instantly. Your agent retrieves commitments directly from Google Calendar.",
  },
  {
    icon: ListChecks,
    title: "Manage Commitments",
    description: "Create and organize events, meetings, and time blocks through natural language commands.",
  },
  {
    icon: AlertCircle,
    title: "Detect Conflicts",
    description: "Automatic conflict detection across all your commitments. Never double-book or miss overlaps.",
  },
  {
    icon: Users,
    title: "Coordinate With Other Agents",
    description: "Your agent communicates with other users' agents to find common availability automatically.",
  },
  {
    icon: CheckSquare,
    title: "Request Human Approval",
    description: "Every coordination proposal requires your explicit approval before any action is taken.",
  },
  {
    icon: History,
    title: "Maintain History",
    description: "Full coordination history and audit logs. Track every agent interaction and decision.",
  },
]

export function AgentCapabilities() {
  const { ref: sectionRef, isInView } = useScrollAnimation()

  return (
    <section id="capabilities" className="py-32 lg:py-40" ref={sectionRef}>
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <motion.div 
          className="max-w-3xl mb-20"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-6">
            Capabilities
          </p>
          <h2 className="text-4xl sm:text-5xl lg:text-6xl font-semibold tracking-tight text-foreground mb-8 leading-[1.1]">
            Your agent, your rules
          </h2>
          <p className="text-xl lg:text-2xl text-muted-foreground leading-relaxed">
            Each user receives a personal agent that can perform these core actions on your behalf.
          </p>
        </motion.div>

        {/* Capability cards */}
        <motion.div 
          className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8"
          variants={staggerContainerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          {capabilities.map((capability, index) => (
            <motion.div
              key={capability.title}
              className="group p-8 rounded-3xl border border-border/40 bg-card transition-all duration-300"
              variants={itemVariants}
              whileHover={{ 
                scale: 1.03,
                boxShadow: "0 20px 40px rgba(0, 0, 0, 0.08)",
                borderColor: "hsl(var(--foreground) / 0.15)"
              }}
              transition={{ duration: 0.25 }}
            >
              <div className="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-6 group-hover:bg-foreground/10 group-hover:scale-110 transition-all duration-300">
                <capability.icon className="w-7 h-7 text-foreground/70 group-hover:text-foreground transition-colors duration-300" />
              </div>
              <h3 className="text-xl font-semibold text-foreground mb-4">
                {capability.title}
              </h3>
              <p className="text-base text-muted-foreground leading-relaxed">
                {capability.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
