"use client"

import { Zap, GitBranch, Plug, Shield, Cpu } from "lucide-react"
import { motion } from "framer-motion"
import { useEffect, useState } from "react"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const benefits = [
  {
    icon: Plug,
    title: "MCP-Based Extensibility",
    description: "Adding a new tool is just adding another MCP server. No agent code changes. Calendar, Tasks, Drive, Gmail — any resource becomes an MCP tool.",
    colorText: "text-emerald-500",
    hoverColorText: "group-hover:text-emerald-500",
  },
  {
    icon: Zap,
    title: "Agent-to-Agent Collaboration",
    description: "Your agent talks to others' agents via A2A. Cross-user scheduling happens automatically without manual back-and-forth communication.",
    colorText: "text-blue-500",
    hoverColorText: "group-hover:text-blue-500",
  },
  {
    icon: Cpu,
    title: "LLM-Orchestrated Planning",
    description: "The Groq-powered LLM planner sees all 32 MCP tool schemas and decides the optimal execution plan for each user message.",
    colorText: "text-purple-500",
    hoverColorText: "group-hover:text-purple-500",
  },
  {
    icon: GitBranch,
    title: "Hybrid Architecture",
    description: "Java Spring Boot backend for A2A coordination + Python FastAPI agent for MCP tools and LLM planning. Best of both worlds.",
    colorText: "text-amber-500",
    hoverColorText: "group-hover:text-amber-500",
  },
  {
    icon: Shield,
    title: "Human-in-the-Loop Control",
    description: "Every coordination action requires your explicit approval via Slack. Your agent proposes, you decide. Nothing happens without consent.",
    colorText: "text-rose-500",
    hoverColorText: "group-hover:text-rose-500",
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
            Why MCP + A2A
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            Built on open protocols, not proprietary locks
          </h2>
          <p className="text-muted-foreground text-lg">
            CoAgent4U combines MCP for tool access and A2A for agent collaboration — giving you extensibility without vendor lock-in.
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
