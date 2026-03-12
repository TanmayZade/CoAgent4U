"use client"

import { useEffect, useRef, ReactNode } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

interface GSAPProviderProps {
  children: ReactNode
}

export function GSAPProvider({ children }: GSAPProviderProps) {
  const initialized = useRef(false)

  useEffect(() => {
    if (initialized.current) return
    initialized.current = true

    // Refresh ScrollTrigger after all content loads
    const timeout = setTimeout(() => {
      ScrollTrigger.refresh()
    }, 100)

    return () => {
      clearTimeout(timeout)
      ScrollTrigger.getAll().forEach(trigger => trigger.kill())
    }
  }, [])

  return <>{children}</>
}

// Custom hook for scroll animations
export function useScrollReveal(
  ref: React.RefObject<HTMLElement | null>,
  options?: {
    y?: number
    scale?: number
    duration?: number
    delay?: number
    stagger?: number
    start?: string
  }
) {
  useEffect(() => {
    if (!ref.current) return

    const element = ref.current
    const children = element.querySelectorAll(".reveal-item")
    const targets = children.length > 0 ? children : element

    const ctx = gsap.context(() => {
      gsap.fromTo(
        targets,
        {
          y: options?.y ?? 60,
          opacity: 0,
          scale: options?.scale ?? 1,
        },
        {
          y: 0,
          opacity: 1,
          scale: 1,
          duration: options?.duration ?? 0.8,
          delay: options?.delay ?? 0,
          stagger: options?.stagger ?? 0.1,
          ease: "power3.out",
          scrollTrigger: {
            trigger: element,
            start: options?.start ?? "top 85%",
            toggleActions: "play none none none",
          },
        }
      )
    }, element)

    return () => ctx.revert()
  }, [ref, options])
}

// Custom hook for text split animation
export function useTextReveal(
  ref: React.RefObject<HTMLElement | null>,
  options?: {
    duration?: number
    stagger?: number
    delay?: number
  }
) {
  useEffect(() => {
    if (!ref.current) return

    const element = ref.current
    const text = element.innerText
    element.innerHTML = ""

    // Split into words, then chars
    const words = text.split(" ")
    words.forEach((word, wordIndex) => {
      const wordSpan = document.createElement("span")
      wordSpan.style.display = "inline-block"
      wordSpan.style.marginRight = "0.25em"
      
      word.split("").forEach((char) => {
        const charSpan = document.createElement("span")
        charSpan.className = "char"
        charSpan.style.display = "inline-block"
        charSpan.style.opacity = "0"
        charSpan.style.transform = "translateY(100%)"
        charSpan.innerText = char
        wordSpan.appendChild(charSpan)
      })
      
      element.appendChild(wordSpan)
      if (wordIndex < words.length - 1) {
        element.appendChild(document.createTextNode(""))
      }
    })

    const chars = element.querySelectorAll(".char")

    const ctx = gsap.context(() => {
      gsap.to(chars, {
        y: 0,
        opacity: 1,
        duration: options?.duration ?? 0.5,
        stagger: options?.stagger ?? 0.02,
        delay: options?.delay ?? 0,
        ease: "power3.out",
        scrollTrigger: {
          trigger: element,
          start: "top 85%",
          toggleActions: "play none none none",
        },
      })
    }, element)

    return () => ctx.revert()
  }, [ref, options])
}

// Magnetic button hook
export function useMagneticButton(ref: React.RefObject<HTMLElement | null>) {
  useEffect(() => {
    if (!ref.current) return

    const element = ref.current

    const handleMouseMove = (e: MouseEvent) => {
      const rect = element.getBoundingClientRect()
      const x = e.clientX - rect.left - rect.width / 2
      const y = e.clientY - rect.top - rect.height / 2
      
      gsap.to(element, {
        x: x * 0.2,
        y: y * 0.2,
        duration: 0.3,
        ease: "power2.out",
      })
    }

    const handleMouseLeave = () => {
      gsap.to(element, {
        x: 0,
        y: 0,
        duration: 0.5,
        ease: "elastic.out(1, 0.5)",
      })
    }

    element.addEventListener("mousemove", handleMouseMove)
    element.addEventListener("mouseleave", handleMouseLeave)

    return () => {
      element.removeEventListener("mousemove", handleMouseMove)
      element.removeEventListener("mouseleave", handleMouseLeave)
    }
  }, [ref])
}

// Parallax hook
export function useParallax(
  ref: React.RefObject<HTMLElement | null>,
  speed: number = 0.5
) {
  useEffect(() => {
    if (!ref.current) return

    const element = ref.current

    const ctx = gsap.context(() => {
      gsap.to(element, {
        y: () => -window.innerHeight * speed,
        ease: "none",
        scrollTrigger: {
          trigger: element,
          start: "top bottom",
          end: "bottom top",
          scrub: true,
        },
      })
    }, element)

    return () => ctx.revert()
  }, [ref, speed])
}

// Counter animation hook
export function useCounterAnimation(
  ref: React.RefObject<HTMLElement | null>,
  endValue: number,
  options?: {
    duration?: number
    delay?: number
    prefix?: string
    suffix?: string
  }
) {
  useEffect(() => {
    if (!ref.current) return

    const element = ref.current
    const prefix = options?.prefix ?? ""
    const suffix = options?.suffix ?? ""

    const ctx = gsap.context(() => {
      const obj = { value: 0 }
      
      gsap.to(obj, {
        value: endValue,
        duration: options?.duration ?? 2,
        delay: options?.delay ?? 0,
        ease: "power2.out",
        scrollTrigger: {
          trigger: element,
          start: "top 85%",
          toggleActions: "play none none none",
        },
        onUpdate: () => {
          const displayValue = Number.isInteger(endValue) 
            ? Math.round(obj.value) 
            : obj.value.toFixed(1)
          element.innerText = `${prefix}${displayValue}${suffix}`
        },
      })
    }, element)

    return () => ctx.revert()
  }, [ref, endValue, options])
}

// Line draw animation hook
export function useLineDraw(ref: React.RefObject<SVGElement | null>) {
  useEffect(() => {
    if (!ref.current) return

    const element = ref.current
    const paths = element.querySelectorAll("path, line, polyline")

    paths.forEach((path) => {
      const length = (path as SVGGeometryElement).getTotalLength?.() || 1000
      ;(path as SVGElement).style.strokeDasharray = `${length}`
      ;(path as SVGElement).style.strokeDashoffset = `${length}`
    })

    const ctx = gsap.context(() => {
      gsap.to(paths, {
        strokeDashoffset: 0,
        duration: 1.5,
        stagger: 0.1,
        ease: "power2.out",
        scrollTrigger: {
          trigger: element,
          start: "top 80%",
          toggleActions: "play none none none",
        },
      })
    }, element)

    return () => ctx.revert()
  }, [ref])
}
