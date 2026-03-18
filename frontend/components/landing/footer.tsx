"use client"

import Link from "next/link"
import Image from "next/image"
import { Github, Twitter } from "lucide-react"
import { useScrollReveal, useStaggerReveal } from "@/hooks/use-gsap-animations"

const footerLinks = {
  product: [
    { href: "#capabilities", label: "Capabilities" },
    { href: "#how-it-works", label: "How It Works" },
    { href: "#use-cases", label: "Use Cases" },
    { href: "#security", label: "Security" },
  ],
  company: [
    { href: "/about", label: "About" },
    { href: "/blog", label: "Blog" },
    { href: "/careers", label: "Careers" },
  ],
  legal: [
    { href: "/privacy", label: "Privacy" },
    { href: "/terms", label: "Terms" },
  ],
}

export function Footer() {
  const footerRef = useScrollReveal<HTMLElement>({ y: 40, duration: 1 })
  const linksRef = useStaggerReveal<HTMLDivElement>({ 
    stagger: 0.08, 
    y: 25, 
    duration: 0.6,
    childSelector: "li"
  })

  return (
    <footer ref={footerRef} className="border-t border-border/40 bg-muted/20">
      <div className="mx-auto max-w-7xl px-6 py-16 lg:py-20">
        <div ref={linksRef} className="grid grid-cols-2 md:grid-cols-5 gap-10 lg:gap-16 mb-16">
          {/* Logo & Description */}
          <div className="col-span-2">
            <Link href="/" className="inline-flex items-center gap-4 mb-6 group">
              <Image 
                src="/images/logo-light.png" 
                alt="CoAgent4U Logo" 
                width={48} 
                height={48}
                className="transition-transform duration-300 group-hover:scale-105"
                style={{ width: '40px', height: '40px' }}
              />
              <span className="text-2xl font-serif font-medium text-foreground tracking-tight italic">
                CoAgent4U
              </span>
            </Link>
            <p className="text-base text-muted-foreground max-w-sm leading-relaxed">
              Your personal agent that coordinates your time. Deterministic, human-in-the-loop scheduling.
            </p>
          </div>

          {/* Product Links */}
          <div>
            <h3 className="text-sm font-semibold text-foreground uppercase tracking-wider mb-5">Product</h3>
            <ul className="space-y-4">
              {footerLinks.product.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-base text-muted-foreground hover:text-foreground transition-all duration-200 underline-hover"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Company Links */}
          <div>
            <h3 className="text-sm font-semibold text-foreground uppercase tracking-wider mb-5">Company</h3>
            <ul className="space-y-4">
              {footerLinks.company.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-base text-muted-foreground hover:text-foreground transition-all duration-200 underline-hover"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Legal Links */}
          <div>
            <h3 className="text-sm font-semibold text-foreground uppercase tracking-wider mb-5">Legal</h3>
            <ul className="space-y-4">
              {footerLinks.legal.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-base text-muted-foreground hover:text-foreground transition-all duration-200 underline-hover"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="pt-10 border-t border-border/40 flex flex-col sm:flex-row items-center justify-between gap-5">
          <p className="text-base text-muted-foreground">
            {new Date().getFullYear()} CoAgent4U. All rights reserved.
          </p>
          <div className="flex items-center gap-5">
            <Link
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-muted-foreground hover:text-foreground transition-all duration-300 hover:scale-110"
              aria-label="GitHub"
            >
              <Github className="w-6 h-6" />
            </Link>
            <Link
              href="https://twitter.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-muted-foreground hover:text-foreground transition-all duration-300 hover:scale-110"
              aria-label="Twitter"
            >
              <Twitter className="w-6 h-6" />
            </Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
