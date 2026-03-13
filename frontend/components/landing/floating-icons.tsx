"use client"

import { useEffect, useRef, useState } from "react"
import { motion, useMotionValue, useSpring, useTransform } from "framer-motion"
import { 
  Calendar, 
  Clock, 
  Users, 
  CheckCircle, 
  Bell, 
  MessageSquare,
  Zap,
  Bot,
  Sparkles,
  Target,
  Layers,
  Shield
} from "lucide-react"

interface FloatingIcon {
  id: number
  Icon: React.ElementType
  x: number
  y: number
  size: number
  delay: number
  duration: number
  rotationRange: number
}

const iconComponents = [
  Calendar, Clock, Users, CheckCircle, Bell, MessageSquare,
  Zap, Bot, Sparkles, Target, Layers, Shield
]

function generateIcons(count: number): FloatingIcon[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i,
    Icon: iconComponents[i % iconComponents.length],
    x: Math.random() * 100,
    y: Math.random() * 100,
    size: 24 + Math.random() * 24,
    delay: Math.random() * 2,
    duration: 15 + Math.random() * 10,
    rotationRange: 10 + Math.random() * 20,
  }))
}

function FloatingIconItem({ icon, mouseX, mouseY }: { 
  icon: FloatingIcon
  mouseX: ReturnType<typeof useMotionValue<number>>
  mouseY: ReturnType<typeof useMotionValue<number>>
}) {
  const ref = useRef<HTMLDivElement>(null)
  
  // Mouse parallax effect
  const springConfig = { damping: 25, stiffness: 150 }
  const x = useSpring(useTransform(mouseX, [0, 1], [-15, 15]), springConfig)
  const y = useSpring(useTransform(mouseY, [0, 1], [-15, 15]), springConfig)
  
  // Multiply effect based on icon position
  const parallaxMultiplier = 0.3 + (icon.y / 100) * 0.7
  
  return (
    <motion.div
      ref={ref}
      className="absolute pointer-events-none"
      style={{
        left: `${icon.x}%`,
        top: `${icon.y}%`,
        x: useTransform(x, v => v * parallaxMultiplier),
        y: useTransform(y, v => v * parallaxMultiplier),
      }}
      initial={{ opacity: 0, scale: 0 }}
      animate={{ 
        opacity: [0.15, 0.35, 0.15],
        scale: [1, 1.1, 1],
        rotate: [-icon.rotationRange / 2, icon.rotationRange / 2, -icon.rotationRange / 2],
        y: [0, -20, 0],
      }}
      transition={{
        duration: icon.duration,
        delay: icon.delay,
        repeat: Infinity,
        ease: "easeInOut",
      }}
    >
      <icon.Icon 
        style={{ width: icon.size, height: icon.size }}
        className="text-foreground/20 dark:text-foreground/15"
        strokeWidth={1.5}
      />
    </motion.div>
  )
}

export function FloatingIcons({ className = "" }: { className?: string }) {
  const [icons] = useState(() => generateIcons(18))
  const containerRef = useRef<HTMLDivElement>(null)
  const mouseX = useMotionValue(0.5)
  const mouseY = useMotionValue(0.5)
  
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!containerRef.current) return
      const rect = containerRef.current.getBoundingClientRect()
      mouseX.set((e.clientX - rect.left) / rect.width)
      mouseY.set((e.clientY - rect.top) / rect.height)
    }
    
    window.addEventListener("mousemove", handleMouseMove)
    return () => window.removeEventListener("mousemove", handleMouseMove)
  }, [mouseX, mouseY])
  
  return (
    <div 
      ref={containerRef}
      className={`absolute inset-0 overflow-hidden ${className}`}
    >
      {icons.map((icon) => (
        <FloatingIconItem 
          key={icon.id} 
          icon={icon} 
          mouseX={mouseX}
          mouseY={mouseY}
        />
      ))}
    </div>
  )
}

// Morphing icon component for special effects
export function MorphingIcon({ className = "" }: { className?: string }) {
  const iconVariants = {
    initial: { pathLength: 0, opacity: 0 },
    animate: { 
      pathLength: 1, 
      opacity: 1,
      transition: { duration: 2, ease: "easeInOut" }
    },
  }
  
  return (
    <motion.div 
      className={`relative ${className}`}
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ duration: 0.8, ease: "easeOut" }}
    >
      <svg 
        viewBox="0 0 100 100" 
        className="w-full h-full"
        fill="none"
        stroke="currentColor"
        strokeWidth="1"
      >
        <motion.circle
          cx="50"
          cy="50"
          r="40"
          className="text-foreground/10"
          variants={iconVariants}
          initial="initial"
          animate="animate"
        />
        <motion.circle
          cx="50"
          cy="50"
          r="30"
          className="text-foreground/15"
          variants={iconVariants}
          initial="initial"
          animate="animate"
          transition={{ delay: 0.3, duration: 2, ease: "easeInOut" }}
        />
        <motion.circle
          cx="50"
          cy="50"
          r="20"
          className="text-foreground/20"
          variants={iconVariants}
          initial="initial"
          animate="animate"
          transition={{ delay: 0.6, duration: 2, ease: "easeInOut" }}
        />
      </svg>
    </motion.div>
  )
}

// Orbiting icons around a central element
export function OrbitingIcons({ className = "" }: { className?: string }) {
  const orbitIcons = [Calendar, Users, Clock, CheckCircle, Bot, Zap]
  
  return (
    <div className={`relative ${className}`}>
      {orbitIcons.map((Icon, index) => {
        const angle = (index / orbitIcons.length) * 360
        const delay = index * 0.2
        
        return (
          <motion.div
            key={index}
            className="absolute"
            style={{
              left: "50%",
              top: "50%",
            }}
            animate={{
              rotate: [angle, angle + 360],
            }}
            transition={{
              duration: 20 + index * 2,
              repeat: Infinity,
              ease: "linear",
            }}
          >
            <motion.div
              style={{
                x: 120 + index * 15,
                y: 0,
              }}
              initial={{ opacity: 0, scale: 0 }}
              animate={{ 
                opacity: [0.2, 0.4, 0.2],
                scale: [0.9, 1.1, 0.9],
              }}
              transition={{
                duration: 3,
                delay,
                repeat: Infinity,
                ease: "easeInOut",
              }}
            >
              <Icon 
                className="w-6 h-6 text-foreground/30 dark:text-foreground/20"
                strokeWidth={1.5}
              />
            </motion.div>
          </motion.div>
        )
      })}
    </div>
  )
}
