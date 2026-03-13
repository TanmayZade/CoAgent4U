"use client"

import { useScroll, useTransform, useSpring, MotionValue } from "framer-motion"
import { useRef } from "react"

interface ParallaxOptions {
  offset?: number
  speed?: number
  direction?: "up" | "down"
}

export function useParallax(options: ParallaxOptions = {}) {
  const { offset = 50, speed = 0.5, direction = "up" } = options
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  })
  
  const multiplier = direction === "up" ? -1 : 1
  
  const y = useTransform(
    scrollYProgress,
    [0, 1],
    [offset * multiplier, -offset * multiplier * speed]
  )
  
  const smoothY = useSpring(y, {
    stiffness: 100,
    damping: 30,
    restDelta: 0.001,
  })
  
  return { ref, y: smoothY }
}

// Hook for opacity fade based on scroll
export function useScrollFade(options: { start?: number; end?: number } = {}) {
  const { start = 0.2, end = 0.8 } = options
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  })
  
  const opacity = useTransform(scrollYProgress, [0, start, end, 1], [0, 1, 1, 0])
  
  const smoothOpacity = useSpring(opacity, {
    stiffness: 100,
    damping: 30,
  })
  
  return { ref, opacity: smoothOpacity }
}

// Hook for scale effect based on scroll
export function useScrollScale(options: { 
  minScale?: number
  maxScale?: number
} = {}) {
  const { minScale = 0.9, maxScale = 1 } = options
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "center center"],
  })
  
  const scale = useTransform(scrollYProgress, [0, 1], [minScale, maxScale])
  
  const smoothScale = useSpring(scale, {
    stiffness: 100,
    damping: 30,
  })
  
  return { ref, scale: smoothScale }
}

// Combined parallax with multiple effects
export function useAdvancedParallax(options: {
  yOffset?: number
  xOffset?: number
  rotateRange?: number
  scaleRange?: [number, number]
} = {}) {
  const { 
    yOffset = 50, 
    xOffset = 0, 
    rotateRange = 0,
    scaleRange = [1, 1]
  } = options
  
  const ref = useRef<HTMLDivElement>(null)
  
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  })
  
  const y = useTransform(scrollYProgress, [0, 1], [yOffset, -yOffset])
  const x = useTransform(scrollYProgress, [0, 1], [-xOffset, xOffset])
  const rotate = useTransform(scrollYProgress, [0, 1], [-rotateRange, rotateRange])
  const scale = useTransform(scrollYProgress, [0, 0.5, 1], [scaleRange[0], 1, scaleRange[1]])
  
  const springConfig = { stiffness: 100, damping: 30, restDelta: 0.001 }
  
  return {
    ref,
    y: useSpring(y, springConfig),
    x: useSpring(x, springConfig),
    rotate: useSpring(rotate, springConfig),
    scale: useSpring(scale, springConfig),
    scrollYProgress,
  }
}

// Hook for smooth scroll progress indicator
export function useScrollProgress() {
  const { scrollYProgress } = useScroll()
  
  const scaleX = useSpring(scrollYProgress, {
    stiffness: 100,
    damping: 30,
    restDelta: 0.001,
  })
  
  return { scaleX, scrollYProgress }
}
