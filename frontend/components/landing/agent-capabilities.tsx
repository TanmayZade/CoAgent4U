"use client"

import { Calendar, ListChecks, AlertCircle, Brain, Plug, Cpu } from "lucide-react"
import { motion } from "framer-motion"
import { useEffect, useState } from "react"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const capabilities = [
  {
    icon: Calendar,
    title: "Calendar via MCP",
    description: "17 MCP tools for Google Calendar — list events, check availability, create/delete events, find free slots, all through the standard MCP protocol.",
    colorText: "text-blue-500",
    hoverColorText: "group-hover:text-blue-500",
  },
  {
    icon: ListChecks,
    title: "Tasks via MCP",
    description: "8 MCP tools for Google Tasks — create task lists, add/update/complete/delete tasks. Your agent manages your to-do list through natural language.",
    colorText: "text-purple-500",
    hoverColorText: "group-hover:text-purple-500",
  },
  {
    icon: Brain,
    title: "LLM-Driven Planning",
    description: "Groq-powered LLM planner decides which MCP tools to call based on your message. It plans multi-step tasks and summarizes results in natural language.",
    colorText: "text-emerald-500",
    hoverColorText: "group-hover:text-emerald-500",
  },
  {
    icon: Plug,
    title: "A2A Coordination",
    description: "Your agent communicates with other users' agents via the Agent-to-Agent protocol to find common availability and schedule meetings across calendars.",
    colorText: "text-amber-500",
    hoverColorText: "group-hover:text-amber-500",
  },
  {
    icon: Cpu,
    title: "Productivity Intelligence",
    description: "7 MCP productivity tools — daily briefings, weekly summaries, focus time blocks, smart scheduling with preferences, and recurring routines.",
    colorText: "text-rose-500",
    hoverColorText: "group-hover:text-rose-500",
  },
  {
    icon: AlertCircle,
    title: "Conflict Detection",
    description: "Automatic conflict detection across all your commitments before creating events. Your agent checks availability and prevents double-bookings.",
    colorText: "text-indigo-500",
    hoverColorText: "group-hover:text-indigo-500",
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
    <section id="capabilities" className="py-14 lg:py-16" ref={sectionRef}>
      <div className="mx-auto max-w-7xl px-6">
        {/* Section header */}
        <motion.div 
          className="max-w-3xl mb-12"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-[0.2em] mb-4">
            32 MCP Tools
          </p>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-foreground mb-4 leading-[1.1]">
            Your agent, your tools, your rules
          </h2>
          <p className="text-lg lg:text-xl text-muted-foreground leading-relaxed">
            Your personal agent connects to 3 MCP servers with 32 tools for calendar, tasks, and productivity — all accessible through natural language.
          </p>
        </motion.div>

        {/* Capability cards */}
        <motion.div 
          className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5 lg:gap-6"
          variants={staggerContainerVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          {capabilities.map((capability, index) => (
            <motion.div
              key={capability.title}
              className={`text-center p-6 lg:p-7 rounded-3xl bg-background border transition-all duration-300 group ${
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
                className={`w-14 h-14 rounded-2xl border border-border/60 flex items-center justify-center mx-auto mb-6 transition-colors duration-300 group-hover:bg-foreground/5 ${
                  activeCardIndex === index ? "bg-foreground/5" : "bg-muted"
                }`}
                animate={{ y: [0, -8, 0] }}
                transition={{ repeat: Infinity, duration: 4, ease: "easeInOut", delay: index * 0.5 }}
              >
                <capability.icon className={`w-7 h-7 transition-all duration-300 group-hover:scale-110 ${
                  activeCardIndex === index 
                    ? `scale-110 ${capability.colorText}` 
                    : `text-foreground ${capability.hoverColorText}`
                }`} />
              </motion.div>
              <h3 className="text-lg font-semibold text-foreground mb-3">
                {capability.title}
              </h3>
              <p className="text-muted-foreground text-[15px] leading-relaxed">
                {capability.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
