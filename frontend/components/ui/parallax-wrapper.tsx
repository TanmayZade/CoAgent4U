"use client"

import { motion, useScroll, useTransform, useSpring } from "framer-motion"
import { useRef, ReactNode } from "react"

interface ParallaxWrapperProps {
  children: ReactNode
  className?: string
  speed?: number // -1 to 1, negative = opposite direction
  direction?: "vertical" | "horizontal"
  fadeIn?: boolean
  scale?: boolean
}

export function ParallaxWrapper({
  children,
  className = "",
  speed = 0.3,
  direction = "vertical",
  fadeIn = false,
  scale = false,
}: ParallaxWrapperProps) {
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  })
  
  const springConfig = { stiffness: 100, damping: 30, restDelta: 0.001 }
  
  // Parallax movement
  const y = useSpring(
    useTransform(scrollYProgress, [0, 1], [100 * speed, -100 * speed]),
    springConfig
  )
  
  const x = useSpring(
    useTransform(scrollYProgress, [0, 1], [50 * speed, -50 * speed]),
    springConfig
  )
  
  // Fade effect
  const opacity = useSpring(
    useTransform(scrollYProgress, [0, 0.2, 0.8, 1], [0, 1, 1, 0]),
    springConfig
  )
  
  // Scale effect
  const scaleValue = useSpring(
    useTransform(scrollYProgress, [0, 0.5, 1], [0.95, 1, 0.95]),
    springConfig
  )
  
  return (
    <motion.div
      ref={ref}
      className={className}
      style={{
        y: direction === "vertical" ? y : 0,
        x: direction === "horizontal" ? x : 0,
        opacity: fadeIn ? opacity : 1,
        scale: scale ? scaleValue : 1,
      }}
    >
      {children}
    </motion.div>
  )
}

// Scroll-triggered reveal component
interface ScrollRevealProps {
  children: ReactNode
  className?: string
  delay?: number
  duration?: number
  direction?: "up" | "down" | "left" | "right"
  distance?: number
}

export function ScrollReveal({
  children,
  className = "",
  delay = 0,
  duration = 0.6,
  direction = "up",
  distance = 40,
}: ScrollRevealProps) {
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start 0.9", "start 0.5"],
  })
  
  const directionMap = {
    up: { y: distance, x: 0 },
    down: { y: -distance, x: 0 },
    left: { y: 0, x: distance },
    right: { y: 0, x: -distance },
  }
  
  const { y: initialY, x: initialX } = directionMap[direction]
  
  const y = useTransform(scrollYProgress, [0, 1], [initialY, 0])
  const x = useTransform(scrollYProgress, [0, 1], [initialX, 0])
  const opacity = useTransform(scrollYProgress, [0, 1], [0, 1])
  
  return (
    <motion.div
      ref={ref}
      className={className}
      style={{
        y,
        x,
        opacity,
      }}
    >
      {children}
    </motion.div>
  )
}

// Smooth scroll progress indicator
export function ScrollProgressBar({ className = "" }: { className?: string }) {
  const { scrollYProgress } = useScroll()
  
  const scaleX = useSpring(scrollYProgress, {
    stiffness: 100,
    damping: 30,
    restDelta: 0.001,
  })
  
  return (
    <motion.div
      className={`fixed top-0 left-0 right-0 h-1 bg-foreground origin-left z-50 ${className}`}
      style={{ scaleX }}
    />
  )
}

// Background parallax for hero sections
interface ParallaxBackgroundProps {
  children?: ReactNode
  className?: string
  layers?: number
}

export function ParallaxBackground({ 
  children, 
  className = "",
  layers = 3,
}: ParallaxBackgroundProps) {
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  })
  
  return (
    <div ref={ref} className={`relative overflow-hidden ${className}`}>
      {/* Parallax layers */}
      {Array.from({ length: layers }).map((_, i) => {
        const speed = 0.1 * (i + 1)
        const y = useTransform(scrollYProgress, [0, 1], [0, 100 * speed])
        const opacity = 0.1 - (i * 0.02)
        
        return (
          <motion.div
            key={i}
            className="absolute inset-0 pointer-events-none"
            style={{ y }}
          >
            <div 
              className="w-full h-full"
              style={{
                background: `radial-gradient(circle at ${30 + i * 20}% ${40 + i * 10}%, var(--foreground) 0%, transparent 50%)`,
                opacity,
              }}
            />
          </motion.div>
        )
      })}
      
      {/* Content */}
      <div className="relative z-10">
        {children}
      </div>
    </div>
  )
}
