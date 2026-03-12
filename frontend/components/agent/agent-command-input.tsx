"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Send } from "lucide-react"

const placeholders = [
  "schedule a meeting with @sarah friday evening",
  "check my availability tomorrow",
  "add a reminder for the project deadline",
  "find a time with @mike next week",
]

export function AgentCommandInput() {
  const [value, setValue] = useState("")
  const [placeholderIndex, setPlaceholderIndex] = useState(0)
  const [displayPlaceholder, setDisplayPlaceholder] = useState("")
  const [isTyping, setIsTyping] = useState(true)

  useEffect(() => {
    const currentPlaceholder = placeholders[placeholderIndex]
    let charIndex = 0

    if (isTyping) {
      const typeInterval = setInterval(() => {
        if (charIndex <= currentPlaceholder.length) {
          setDisplayPlaceholder(currentPlaceholder.slice(0, charIndex))
          charIndex++
        } else {
          clearInterval(typeInterval)
          setTimeout(() => setIsTyping(false), 2000)
        }
      }, 50)

      return () => clearInterval(typeInterval)
    } else {
      const deleteInterval = setInterval(() => {
        if (displayPlaceholder.length > 0) {
          setDisplayPlaceholder((prev) => prev.slice(0, -1))
        } else {
          clearInterval(deleteInterval)
          setPlaceholderIndex((prev) => (prev + 1) % placeholders.length)
          setIsTyping(true)
        }
      }, 30)

      return () => clearInterval(deleteInterval)
    }
  }, [placeholderIndex, isTyping])

  return (
    <div className="relative bg-[#0A1628] rounded-xl border border-[#1A2F4A] overflow-hidden">
      <div className="flex items-center gap-3 p-4">
        <div className="flex items-center gap-2 px-3 py-1.5 bg-[#00D4FF]/10 rounded-lg border border-[#00D4FF]/20">
          <span className="text-sm font-medium text-[#00D4FF]">@CoAgent4U</span>
        </div>
        <span className="text-foreground-muted">{"›"}</span>
        <div className="flex-1 relative">
          <input
            type="text"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            className="w-full bg-transparent text-foreground placeholder:text-foreground-muted outline-none"
          />
          {!value && (
            <span className="absolute left-0 top-0 text-foreground-muted pointer-events-none">
              {displayPlaceholder}
              <span className="animate-pulse">|</span>
            </span>
          )}
        </div>
        <Button
          size="sm"
          disabled={!value}
          className="bg-[#00D4FF] hover:bg-[#00D4FF]/90 text-[#050A14]"
        >
          <Send className="w-4 h-4" />
        </Button>
      </div>
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[#00D4FF]/50 to-transparent" />
    </div>
  )
}
