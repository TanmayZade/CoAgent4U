"use client"

import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

const metrics = [
  { value: 95, prefix: ">", suffix: "%", label: "Success Rate" },
  { value: 30, prefix: "<", suffix: "s", label: "Avg. Time" },
  { value: 100, suffix: "%", label: "Deterministic" },
  { value: 99.5, suffix: "%", label: "Uptime" },
]

export function MetricsSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headingRef = useRef<HTMLDivElement>(null)
  const metricsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current) return

    const ctx = gsap.context(() => {
      // Heading
      if (headingRef.current) {
        gsap.fromTo(
          headingRef.current.children,
          { y: 30, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.8,
            stagger: 0.1,
            ease: "power3.out",
            scrollTrigger: {
              trigger: headingRef.current,
              start: "top 85%",
            },
          }
        )
      }

      // Metrics with counter animation
      if (metricsRef.current) {
        const items = metricsRef.current.querySelectorAll(".metric-item")
        
        items.forEach((item, index) => {
          const valueEl = item.querySelector(".metric-value")
          const metric = metrics[index]
          
          gsap.fromTo(
            item,
            { y: 40, opacity: 0 },
            {
              y: 0,
              opacity: 1,
              duration: 0.6,
              delay: index * 0.1,
              ease: "power3.out",
              scrollTrigger: {
                trigger: metricsRef.current,
                start: "top 85%",
              },
            }
          )

          // Counter animation
          if (valueEl) {
            const obj = { value: 0 }
            gsap.to(obj, {
              value: metric.value,
              duration: 2,
              delay: index * 0.1 + 0.3,
              ease: "power2.out",
              scrollTrigger: {
                trigger: metricsRef.current,
                start: "top 85%",
              },
              onUpdate: () => {
                const display = Number.isInteger(metric.value)
                  ? Math.round(obj.value)
                  : obj.value.toFixed(1)
                valueEl.textContent = `${metric.prefix || ""}${display}${metric.suffix}`
              },
            })
          }
        })
      }
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} className="relative py-24 overflow-hidden">
      <div className="absolute inset-0 bg-charcoal-light" />
      
      <div className="relative z-10 max-w-6xl mx-auto px-6 sm:px-8 lg:px-12">
        {/* Section header */}
        <div ref={headingRef} className="text-center mb-16">
          <span className="inline-block text-xs font-mono text-accent tracking-widest uppercase mb-4">
            Performance
          </span>
          <h2 className="text-3xl sm:text-4xl font-bold text-cream font-[family-name:var(--font-display)] tracking-tight">
            Numbers that speak
          </h2>
        </div>

        {/* Metrics grid */}
        <div ref={metricsRef} className="grid grid-cols-2 lg:grid-cols-4 gap-6">
          {metrics.map((metric) => (
            <div
              key={metric.label}
              className="metric-item text-center p-8 bg-charcoal rounded-xl border border-border"
            >
              <div className="metric-value text-4xl sm:text-5xl font-bold text-accent font-[family-name:var(--font-display)] tabular-nums mb-2">
                {metric.prefix || ""}0{metric.suffix}
              </div>
              <p className="text-sm text-foreground-secondary">{metric.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
