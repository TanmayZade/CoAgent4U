"use client"

import Link from "next/link"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { useState, useEffect, useRef } from "react"
import { Menu, X } from "lucide-react"
import gsap from "gsap"

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const navRef = useRef<HTMLElement>(null)
  const logoRef = useRef<HTMLAnchorElement>(null)
  const linksRef = useRef<HTMLDivElement>(null)
  const ctaRef = useRef<HTMLDivElement>(null)

  // Scroll detection for navbar background
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  // Initial animation
  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap.fromTo(
        logoRef.current,
        { opacity: 0, x: -20 },
        { opacity: 1, x: 0, duration: 0.6, ease: "power3.out", delay: 0.1 }
      )

      const links = linksRef.current?.querySelectorAll("a")
      if (links) {
        gsap.fromTo(
          links,
          { opacity: 0, y: -10 },
          { opacity: 1, y: 0, duration: 0.4, stagger: 0.08, ease: "power3.out", delay: 0.2 }
        )
      }

      gsap.fromTo(
        ctaRef.current?.children || [],
        { opacity: 0, x: 20 },
        { opacity: 1, x: 0, duration: 0.4, stagger: 0.1, ease: "power3.out", delay: 0.3 }
      )
    }, navRef)

    return () => ctx.revert()
  }, [])

  // Mobile menu animation
  useEffect(() => {
    if (mobileMenuOpen) {
      gsap.fromTo(
        ".mobile-menu-item",
        { opacity: 0, x: -20 },
        { opacity: 1, x: 0, duration: 0.3, stagger: 0.05, ease: "power2.out" }
      )
    }
  }, [mobileMenuOpen])

  return (
    <header 
      ref={navRef}
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled 
          ? "bg-background/95 backdrop-blur-md border-b border-border/40 shadow-sm" 
          : "bg-transparent border-b border-transparent"
      }`}
    >
      <nav className="mx-auto max-w-6xl px-6">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <Link ref={logoRef} href="/" className="flex items-center gap-3 group">
            <Image 
              src="/images/logo.png" 
              alt="CoAgent4U Logo" 
              width={48} 
              height={48}
              className="transition-transform duration-300 group-hover:scale-105"
            />
            <span className="text-xl font-serif font-medium tracking-tight text-foreground italic">
              CoAgent4U
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div ref={linksRef} className="hidden md:flex items-center gap-8">
            <Link 
              href="#capabilities" 
              className="text-sm text-muted-foreground hover:text-foreground transition-colors underline-hover"
            >
              Capabilities
            </Link>
            <Link 
              href="#how-it-works" 
              className="text-sm text-muted-foreground hover:text-foreground transition-colors underline-hover"
            >
              How It Works
            </Link>
            <Link 
              href="#use-cases" 
              className="text-sm text-muted-foreground hover:text-foreground transition-colors underline-hover"
            >
              Use Cases
            </Link>
            <Link 
              href="#security" 
              className="text-sm text-muted-foreground hover:text-foreground transition-colors underline-hover"
            >
              Security
            </Link>
          </div>

          {/* Desktop CTA */}
          <div ref={ctaRef} className="hidden md:flex items-center gap-3">
            <Button variant="ghost" size="sm" className="magnetic-btn" asChild>
              <Link href="/signin">Sign In</Link>
            </Button>
            <Button size="sm" className="magnetic-btn interactive-hover" asChild>
              <Link href="/signin">Get Started</Link>
            </Button>
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden p-2 text-muted-foreground hover:text-foreground transition-all duration-300 hover:scale-110"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-border/40">
            <div className="flex flex-col gap-1">
              <Link 
                href="#capabilities" 
                className="mobile-menu-item px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-lg transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                Capabilities
              </Link>
              <Link 
                href="#how-it-works" 
                className="mobile-menu-item px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-lg transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                How It Works
              </Link>
              <Link 
                href="#use-cases" 
                className="mobile-menu-item px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-lg transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                Use Cases
              </Link>
              <Link 
                href="#security" 
                className="mobile-menu-item px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-lg transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                Security
              </Link>
              <div className="mobile-menu-item flex flex-col gap-2 pt-4 mt-2 border-t border-border/40">
                <Button variant="ghost" size="sm" asChild className="justify-start">
                  <Link href="/signin">Sign In</Link>
                </Button>
                <Button size="sm" asChild>
                  <Link href="/signin">Get Started</Link>
                </Button>
              </div>
            </div>
          </div>
        )}
      </nav>
    </header>
  )
}
