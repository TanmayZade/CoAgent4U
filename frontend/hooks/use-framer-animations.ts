"use client"

import { useRef } from "react"
import { useInView, Variants } from "framer-motion"

/**
 * Custom hook for scroll-triggered animations with Framer Motion
 * Detects when an element enters the viewport and returns animation controls
 */
export function useScrollAnimation() {
  const ref = useRef(null)
  const isInView = useInView(ref, { 
    once: true, // Animation triggers only once
    margin: "0px 0px -100px 0px" // Trigger when element is 100px from bottom of viewport
  })

  return { ref, isInView }
}

/**
 * Container animation variants for fade-in and slide-up
 */
export const containerVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0,
    },
  },
}

/**
 * Item animation variants for staggered children
 */
export const itemVariants: Variants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.5,
      ease: "easeOut",
    },
  },
}

/**
 * Fade and slide up animation
 */
export const fadeSlideUpVariants: Variants = {
  hidden: { 
    opacity: 0, 
    y: 40 
  },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.6,
      ease: [0.25, 0.46, 0.45, 0.94], // custom easing curve
    },
  },
}

/**
 * Staggered fade in for multiple items
 */
export const staggerContainerVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.12,
      delayChildren: 0.1,
    },
  },
}

/**
 * Subtle scale animation on scroll
 */
export const scaleUpVariants: Variants = {
  hidden: { 
    opacity: 0, 
    scale: 0.95,
    y: 20
  },
  visible: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: {
      duration: 0.6,
      ease: "easeOut",
    },
  },
}
