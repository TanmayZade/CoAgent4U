"use client"

import Link from "next/link"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { useState, useEffect } from "react"
import { Menu, X } from "lucide-react"
import { ThemeToggle } from "@/components/theme-toggle"

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  // Scroll detection for navbar background
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])



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
      <nav className="mx-auto max-w-6xl px-6">
        <div className="flex h-20 items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-4 group">
            <Image 
              src="/images/logo.png" 
              alt="CoAgent4U Logo" 
              width={56} 
              height={56}
              className="transition-transform duration-300 group-hover:scale-105"
              style={{ width: '48px', height: '48px' }}
            />
            <span className="text-2xl font-serif font-medium tracking-tight text-foreground italic">
              CoAgent4U
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-10">
            <Link 
              href="#capabilities" 
              className="text-base text-muted-foreground hover:text-foreground transition-colors duration-300 underline-hover"
            >
              Capabilities
            </Link>
            <Link 
              href="#how-it-works" 
              className="text-base text-muted-foreground hover:text-foreground transition-colors duration-300 underline-hover"
            >
              How It Works
            </Link>
            <Link 
              href="#use-cases" 
              className="text-base text-muted-foreground hover:text-foreground transition-colors duration-300 underline-hover"
            >
              Use Cases
            </Link>
            <Link 
              href="#security" 
              className="text-base text-muted-foreground hover:text-foreground transition-colors duration-300 underline-hover"
            >
              Security
            </Link>
          </div>

          {/* Desktop CTA */}
          <div className="hidden md:flex items-center gap-4">
            <ThemeToggle />
            <Button variant="ghost" size="lg" className="text-base transition-all duration-300 hover:scale-105" asChild>
              <Link href="/signin">Sign In</Link>
            </Button>
            <Button size="lg" className="text-base rounded-full px-6 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105" asChild>
              <Link href="/signin">Get Started</Link>
            </Button>
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
              <Link 
                href="#capabilities" 
                className="mobile-menu-item px-4 py-3 text-base text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-xl transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                Capabilities
              </Link>
              <Link 
                href="#how-it-works" 
                className="mobile-menu-item px-4 py-3 text-base text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-xl transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                How It Works
              </Link>
              <Link 
                href="#use-cases" 
                className="mobile-menu-item px-4 py-3 text-base text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-xl transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                Use Cases
              </Link>
              <Link 
                href="#security" 
                className="mobile-menu-item px-4 py-3 text-base text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-xl transition-all duration-200"
                onClick={() => setMobileMenuOpen(false)}
              >
                Security
              </Link>
              <div className="mobile-menu-item flex flex-col gap-3 pt-6 mt-4 border-t border-border/40">
                <Button variant="ghost" size="lg" asChild className="justify-start text-base">
                  <Link href="/signin">Sign In</Link>
                </Button>
                <Button size="lg" asChild className="text-base rounded-full">
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
