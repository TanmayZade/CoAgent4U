"use client"

import Link from "next/link"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { useState, useEffect, useRef } from "react"
import { Menu, X, Lock } from "lucide-react"
import { toast } from "sonner"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"
import { ScrollToPlugin } from "gsap/ScrollToPlugin"

function TestOnlyTooltip({ children }: { children: React.ReactNode }) {
  const [show, setShow] = useState(false)
  return (
    <div className="relative inline-block" onMouseEnter={() => setShow(true)} onMouseLeave={() => setShow(false)}>
      {children}
      {show && (
        <div className="absolute right-0 top-full mt-2 z-50 w-64 rounded-xl border border-border/60 bg-background/95 backdrop-blur-md p-3 shadow-xl text-sm">
          <div className="flex items-start gap-2">
            <Lock className="h-4 w-4 text-amber-500 mt-0.5 shrink-0" />
            <span className="text-muted-foreground leading-snug">
              Available to <span className="font-semibold text-foreground">test users only</span>. Interested? Contact{" "}
              <a href="mailto:easychat148@gmail.com" className="text-primary underline underline-offset-2 hover:text-primary/80 break-all">easychat148@gmail.com</a>
            </span>
          </div>
        </div>
      )}
    </div>
  )
}

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const logoRef = useRef<HTMLImageElement>(null)

  useEffect(() => {
    gsap.registerPlugin(ScrollTrigger, ScrollToPlugin)
    
    // Rotate the logo continuously based on scroll position
    if (logoRef.current) {
      gsap.to(logoRef.current, {
        rotation: 360 * 4, // 4 full rotations over the page length
        ease: "none",
        scrollTrigger: {
          trigger: document.body,
          start: "top top",
          end: "bottom bottom",
          scrub: true, // Immediate scrubbing without lag
        }
      })
    }
  }, [])

  // Scroll detection for navbar background
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  // Custom scroll handler to center sections
  const scrollToSection = (e: React.MouseEvent, id: string) => {
    e.preventDefault()
    
    // First, close mobile menu if it's open
    setMobileMenuOpen(false)

    // Give the browser a moment to settle (and for mobile menu to start closing)
    setTimeout(() => {
      // Refresh ScrollTrigger to ensure all layout/scaling calculations are up-to-date
      ScrollTrigger.refresh()
      
      const element = document.querySelector(id)
      if (element) {
        const windowHeight = window.innerHeight
        const elementRect = (element as HTMLElement).getBoundingClientRect()
        const absoluteTop = elementRect.top + window.scrollY
        
        // Calculate offset to center the section in the viewport
        const offset = (windowHeight - elementRect.height) / 2

        gsap.to(window, {
          duration: 1.2,
          scrollTo: {
            y: absoluteTop,
            offsetY: offset > 0 ? offset : 0, 
          },
          ease: "power3.inOut"
        })
      }
    }, 10)
  }

  // Mobile menu animation
  useEffect(() => {
    if (mobileMenuOpen) {
      gsap.fromTo(
        ".mobile-menu-item",
        { opacity: 0, x: -30 },
        { opacity: 1, x: 0, duration: 0.4, stagger: 0.08, ease: "power2.out" }
      )
    }
  }, [mobileMenuOpen])

  return (
    <header 
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        scrolled 
          ? "bg-background/95 backdrop-blur-lg border-b border-border/40 shadow-sm" 
          : "bg-transparent border-b border-transparent"
      }`}
    >
      <nav className="mx-auto max-w-7xl px-6">
        <div className="flex h-20 items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-4 group">
            <Image 
              ref={logoRef}
              src="/images/logo-light.png" 
              alt="CoAgent4U Logo" 
              width={56} 
              height={56}
              className="transition-all duration-300 group-hover:scale-105"
              style={{ width: '48px', height: '48px', transformOrigin: 'center center' }}
              priority
            />
            <span className="text-2xl font-serif font-medium tracking-tight text-foreground italic">
              CoAgent4U
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-10">
            {[
              { id: "#capabilities", label: "Capabilities" },
              { id: "#architecture", label: "Architecture" },
              { id: "#use-cases", label: "Use Cases" },
              { id: "#security", label: "Security" },
            ].map((link) => (
              <a
                key={link.id}
                href={link.id}
                onClick={(e) => scrollToSection(e, link.id)}
                className="text-base text-muted-foreground hover:text-foreground transition-colors duration-300 underline-hover"
              >
                {link.label}
              </a>
            ))}
          </div>

          {/* Desktop CTA */}
          <div className="hidden md:flex items-center gap-4">
            <TestOnlyTooltip>
              <Button variant="ghost" size="lg" className="text-base opacity-50 cursor-not-allowed" disabled>
                <Lock className="mr-1.5 h-3.5 w-3.5" />
                Sign In
              </Button>
            </TestOnlyTooltip>
            <TestOnlyTooltip>
              <Button size="lg" className="text-base rounded-full px-6 shadow-lg opacity-50 cursor-not-allowed" disabled>
                <Lock className="mr-1.5 h-3.5 w-3.5" />
                Get Started
              </Button>
            </TestOnlyTooltip>
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden p-2 text-muted-foreground hover:text-foreground transition-all duration-300 hover:scale-110"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden py-6 border-t border-border/40">
            <div className="flex flex-col gap-2">
              {[
                { id: "#capabilities", label: "Capabilities" },
                { id: "#architecture", label: "Architecture" },
                { id: "#use-cases", label: "Use Cases" },
                { id: "#security", label: "Security" },
              ].map((link) => (
                <a
                  key={link.id}
                  href={link.id}
                  onClick={(e) => scrollToSection(e, link.id)}
                  className="mobile-menu-item px-4 py-3 text-base text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-xl transition-all duration-200"
                >
                  {link.label}
                </a>
              ))}
              <div className="mobile-menu-item flex flex-col gap-3 pt-6 mt-4 border-t border-border/40">
                <div className="flex flex-col gap-1">
                  <Button variant="ghost" size="lg" className="justify-start text-base opacity-50 cursor-not-allowed" disabled>
                    <Lock className="mr-1.5 h-3.5 w-3.5" />
                    Sign In
                  </Button>
                  <Button size="lg" className="text-base rounded-full opacity-50 cursor-not-allowed" disabled>
                    <Lock className="mr-1.5 h-3.5 w-3.5" />
                    Get Started
                  </Button>
                  <p className="text-xs text-muted-foreground px-2 mt-1">
                    Test users only — contact{" "}
                    <a href="mailto:easychat148@gmail.com" className="text-primary underline underline-offset-2">easychat148@gmail.com</a>
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </nav>
    </header>
  )
}
