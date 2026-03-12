"use client"

import Link from "next/link"
import { Github, Twitter } from "lucide-react"
import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

export function Footer() {
  const footerRef = useRef<HTMLElement>(null)
  const contentRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!footerRef.current || !contentRef.current) return

    const ctx = gsap.context(() => {
      gsap.fromTo(
        contentRef.current,
        { y: 30, opacity: 0 },
        {
          y: 0,
          opacity: 1,
          duration: 0.8,
          ease: "power3.out",
          scrollTrigger: {
            trigger: footerRef.current,
            start: "top 90%",
          },
        }
      )
    }, footerRef)

    return () => ctx.revert()
  }, [])

  return (
    <footer ref={footerRef} className="relative border-t border-border noise-overlay">
      <div className="bg-charcoal py-16">
        <div ref={contentRef} className="max-w-6xl mx-auto px-6 sm:px-8 lg:px-12">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-12">
            {/* Logo & Description */}
            <div className="md:col-span-2">
              <Link href="/" className="inline-flex items-center gap-3 mb-5">
                <div className="w-9 h-9 rounded-lg bg-accent flex items-center justify-center">
                  <span className="text-sm font-bold text-cream">CA</span>
                </div>
                <span className="text-lg font-semibold text-cream font-[family-name:var(--font-display)]">
                  CoAgent4U
                </span>
              </Link>
              <p className="text-sm text-foreground-secondary max-w-sm leading-relaxed">
                Deterministic Personal Agent Coordination Platform. 
                Two agents, one goal, zero back-and-forth.
              </p>
            </div>

            {/* Product Links */}
            <div>
              <h3 className="text-xs font-mono text-foreground-muted uppercase tracking-wider mb-5">Product</h3>
              <ul className="space-y-3">
                {[
                  { href: "#features", label: "Features" },
                  { href: "#how-it-works", label: "How It Works" },
                  { href: "/dashboard", label: "Dashboard" },
                  { href: "/signin", label: "Sign In" },
                ].map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-foreground-secondary hover:text-accent transition-colors link-underline"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>

            {/* Legal Links */}
            <div>
              <h3 className="text-xs font-mono text-foreground-muted uppercase tracking-wider mb-5">Legal</h3>
              <ul className="space-y-3">
                {[
                  { href: "/privacy", label: "Privacy Policy" },
                  { href: "/terms", label: "Terms of Service" },
                  { href: "/data", label: "Data & Permissions" },
                ].map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-sm text-foreground-secondary hover:text-accent transition-colors link-underline"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Divider */}
          <hr className="hr-gradient mb-8" />

          {/* Bottom bar */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <p className="text-xs text-foreground-muted">
              {new Date().getFullYear()} CoAgent4U. All rights reserved.
            </p>
            <div className="flex items-center gap-5">
              <Link
                href="https://github.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-foreground-muted hover:text-accent transition-colors"
              >
                <Github className="w-5 h-5" />
                <span className="sr-only">GitHub</span>
              </Link>
              <Link
                href="https://twitter.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-foreground-muted hover:text-accent transition-colors"
              >
                <Twitter className="w-5 h-5" />
                <span className="sr-only">Twitter</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </footer>
  )
}
