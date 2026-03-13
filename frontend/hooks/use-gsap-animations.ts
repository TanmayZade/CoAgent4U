"use client"

import { useEffect, useRef, RefObject } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

// Register GSAP plugins
if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

// Scroll reveal animation hook
export function useScrollReveal<T extends HTMLElement>(
  options: {
    y?: number
    x?: number
    duration?: number
    delay?: number
    ease?: string
    start?: string
    markers?: boolean
  } = {}
): RefObject<T | null> {
  const ref = useRef<T>(null)
  
  const {
    y = 60,
    x = 0,
    duration = 1,
    delay = 0,
    ease = "power3.out",
    start = "top 85%",
  } = options

  useEffect(() => {
    const element = ref.current
    if (!element) return

    gsap.fromTo(
      element,
      { 
        opacity: 0, 
        y, 
        x 
      },
      {
        opacity: 1,
        y: 0,
        x: 0,
        duration,
        delay,
        ease,
        scrollTrigger: {
          trigger: element,
          start,
          toggleActions: "play none none reverse",
        },
      }
    )

    return () => {
      ScrollTrigger.getAll().forEach(trigger => {
        if (trigger.trigger === element) {
          trigger.kill()
        }
      })
    }
  }, [y, x, duration, delay, ease, start])

  return ref
}

// Stagger children animation hook
export function useStaggerReveal<T extends HTMLElement>(
  options: {
    stagger?: number
    y?: number
    duration?: number
    ease?: string
    start?: string
    childSelector?: string
  } = {}
): RefObject<T | null> {
  const ref = useRef<T>(null)
  
  const {
    stagger = 0.1,
    y = 40,
    duration = 0.8,
    ease = "power3.out",
    start = "top 85%",
    childSelector = "> *",
  } = options

  useEffect(() => {
    const container = ref.current
    if (!container) return

    // Handle relative selectors by prepending :scope
    const selector = childSelector.startsWith(">") 
      ? `:scope ${childSelector}` 
      : childSelector
    const children = container.querySelectorAll(selector)
    if (!children.length) return

    gsap.fromTo(
      children,
      { 
        opacity: 0, 
        y 
      },
      {
        opacity: 1,
        y: 0,
        duration,
        stagger,
        ease,
        scrollTrigger: {
          trigger: container,
          start,
          toggleActions: "play none none reverse",
        },
      }
    )

    return () => {
      ScrollTrigger.getAll().forEach(trigger => {
        if (trigger.trigger === container) {
          trigger.kill()
        }
      })
    }
  }, [stagger, y, duration, ease, start, childSelector])

  return ref
}

// Text split animation hook for headlines
export function useTextReveal<T extends HTMLElement>(
  options: {
    type?: "chars" | "words" | "lines"
    stagger?: number
    duration?: number
    ease?: string
    start?: string
    y?: number
  } = {}
): RefObject<T | null> {
  const ref = useRef<T>(null)
  
  const {
    type = "words",
    stagger = 0.05,
    duration = 0.8,
    ease = "power3.out",
    start = "top 85%",
    y = 100,
  } = options

  useEffect(() => {
    const element = ref.current
    if (!element) return

    const text = element.textContent || ""
    let elements: string[] = []

    if (type === "chars") {
      elements = text.split("")
    } else if (type === "words") {
      elements = text.split(" ")
    } else {
      elements = [text]
    }

    // Create wrapped elements
    element.innerHTML = elements
      .map(item => {
        const chars = type === "chars" 
          ? `<span class="split-char">${item === " " ? "&nbsp;" : item}</span>`
          : `<span class="split-word"><span class="split-char">${item}</span></span>`
        return chars
      })
      .join(type === "words" ? " " : "")

    const chars = element.querySelectorAll(".split-char")

    gsap.fromTo(
      chars,
      { 
        opacity: 0, 
        y,
        rotateX: -90,
      },
      {
        opacity: 1,
        y: 0,
        rotateX: 0,
        duration,
        stagger,
        ease,
        scrollTrigger: {
          trigger: element,
          start,
          toggleActions: "play none none reverse",
        },
      }
    )

    return () => {
      ScrollTrigger.getAll().forEach(trigger => {
        if (trigger.trigger === element) {
          trigger.kill()
        }
      })
    }
  }, [type, stagger, duration, ease, start, y])

  return ref
}

// Counter animation hook
export function useCounterAnimation<T extends HTMLElement>(
  endValue: number,
  options: {
    duration?: number
    start?: string
    prefix?: string
    suffix?: string
  } = {}
): RefObject<T | null> {
  const ref = useRef<T>(null)
  
  const {
    duration = 2,
    start = "top 85%",
    prefix = "",
    suffix = "",
  } = options

  useEffect(() => {
    const element = ref.current
    if (!element) return

    const counter = { value: 0 }

    gsap.to(counter, {
      value: endValue,
      duration,
      ease: "power2.out",
      scrollTrigger: {
        trigger: element,
        start,
        toggleActions: "play none none reverse",
      },
      onUpdate: () => {
        element.textContent = `${prefix}${Math.round(counter.value)}${suffix}`
      },
    })

    return () => {
      ScrollTrigger.getAll().forEach(trigger => {
        if (trigger.trigger === element) {
          trigger.kill()
        }
      })
    }
  }, [endValue, duration, start, prefix, suffix])

  return ref
}

// Parallax effect hook
export function useParallax<T extends HTMLElement>(
  speed: number = 0.5
): RefObject<T | null> {
  const ref = useRef<T>(null)

  useEffect(() => {
    const element = ref.current
    if (!element) return

    gsap.to(element, {
      y: () => -ScrollTrigger.maxScroll(window) * speed,
      ease: "none",
      scrollTrigger: {
        trigger: element,
        start: "top bottom",
        end: "bottom top",
        scrub: true,
      },
    })

    return () => {
      ScrollTrigger.getAll().forEach(trigger => {
        if (trigger.trigger === element) {
          trigger.kill()
        }
      })
    }
  }, [speed])

  return ref
}

// Scale on scroll hook
export function useScaleOnScroll<T extends HTMLElement>(
  options: {
    startScale?: number
    endScale?: number
    start?: string
    end?: string
  } = {}
): RefObject<T | null> {
  const ref = useRef<T>(null)
  
  const {
    startScale = 0.8,
    endScale = 1,
    start = "top bottom",
    end = "top center",
  } = options

  useEffect(() => {
    const element = ref.current
    if (!element) return

    gsap.fromTo(
      element,
      { scale: startScale, opacity: 0 },
      {
        scale: endScale,
        opacity: 1,
        ease: "power2.out",
        scrollTrigger: {
          trigger: element,
          start,
          end,
          scrub: 1,
        },
      }
    )

    return () => {
      ScrollTrigger.getAll().forEach(trigger => {
        if (trigger.trigger === element) {
          trigger.kill()
        }
      })
    }
  }, [startScale, endScale, start, end])

  return ref
}

// Magnetic button effect
export function useMagneticEffect<T extends HTMLElement>(
  strength: number = 0.3
): RefObject<T | null> {
  const ref = useRef<T>(null)

  useEffect(() => {
    const element = ref.current
    if (!element) return

    const handleMouseMove = (e: MouseEvent) => {
      const rect = element.getBoundingClientRect()
      const x = e.clientX - rect.left - rect.width / 2
      const y = e.clientY - rect.top - rect.height / 2

      gsap.to(element, {
        x: x * strength,
        y: y * strength,
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
  }, [strength])

  return ref
}
