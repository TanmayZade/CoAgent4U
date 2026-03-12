"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { useState, useEffect, useRef } from "react"
import { Menu, X } from "lucide-react"
import { cn } from "@/lib/utils"
import gsap from "gsap"

export function NavHeader() {
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const headerRef = useRef<HTMLElement>(null)
  const logoRef = useRef<HTMLAnchorElement>(null)

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  useEffect(() => {
    if (!headerRef.current) return

    gsap.fromTo(
      headerRef.current,
      { y: -100, opacity: 0 },
      { y: 0, opacity: 1, duration: 0.8, ease: "power3.out", delay: 0.1 }
    )
  }, [])

  return (
    <header
      ref={headerRef}
      className={cn(
        "fixed top-0 left-0 right-0 z-50 transition-all duration-500",
        isScrolled
          ? "bg-charcoal/95 backdrop-blur-md border-b border-border/50"
          : "bg-transparent"
      )}
    >
      <div className="max-w-6xl mx-auto px-6 sm:px-8 lg:px-12">
        <div className="flex items-center justify-between h-20">
          {/* Logo */}
          <Link
            ref={logoRef}
            href="/"
            className="flex items-center gap-3 group"
          >
            <div className="w-9 h-9 rounded-lg bg-accent flex items-center justify-center transition-transform duration-300 group-hover:scale-105">
              <span className="text-sm font-bold text-cream">CA</span>
            </div>
            <span className="text-lg font-semibold text-cream font-[family-name:var(--font-display)] tracking-tight">
              CoAgent4U
            </span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-8">
            {[
              { href: "#features", label: "Features" },
              { href: "#how-it-works", label: "How It Works" },
              { href: "/dashboard", label: "Dashboard" },
            ].map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-sm text-foreground-secondary hover:text-cream transition-colors duration-300 link-underline"
              >
                {link.label}
              </Link>
            ))}
          </nav>

          {/* Desktop CTA */}
          <div className="hidden md:flex items-center gap-4">
            <Button
              variant="ghost"
              asChild
              className="text-foreground-secondary hover:text-cream hover:bg-transparent transition-colors duration-300"
            >
              <Link href="/signin">Sign In</Link>
            </Button>
            <Button
              asChild
              className="bg-accent hover:bg-accent-dark text-cream font-medium px-6 rounded-full transition-all duration-300"
            >
              <Link href="/signin">Get Started</Link>
            </Button>
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden p-2 text-foreground-secondary hover:text-cream transition-colors"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? (
              <X className="w-6 h-6" />
            ) : (
              <Menu className="w-6 h-6" />
            )}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      <div
        className={cn(
          "md:hidden absolute top-20 left-0 right-0 bg-charcoal-light border-b border-border transition-all duration-300 overflow-hidden",
          isMobileMenuOpen ? "max-h-80 opacity-100" : "max-h-0 opacity-0"
        )}
      >
        <nav className="flex flex-col p-6 gap-4">
          {[
            { href: "#features", label: "Features" },
            { href: "#how-it-works", label: "How It Works" },
            { href: "/dashboard", label: "Dashboard" },
          ].map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-sm text-foreground-secondary hover:text-cream transition-colors py-2"
              onClick={() => setIsMobileMenuOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          <hr className="border-border my-2" />
          <Link
            href="/signin"
            className="text-sm text-foreground-secondary hover:text-cream transition-colors py-2"
            onClick={() => setIsMobileMenuOpen(false)}
          >
            Sign In
          </Link>
          <Button
            asChild
            className="bg-accent hover:bg-accent-dark text-cream font-medium w-full rounded-full"
          >
            <Link href="/signin">Get Started</Link>
          </Button>
        </nav>
      </div>
    </header>
  )
}
