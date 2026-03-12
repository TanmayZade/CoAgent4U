"use client"

import { useState } from "react"
import { cn } from "@/lib/utils"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"

interface CalendarEvent {
  id: string
  title: string
  time: string
  type: "personal" | "coordinated" | "pending"
  day: number
}

const events: CalendarEvent[] = [
  { id: "1", title: "Team Standup", time: "9:00 AM", type: "personal", day: 11 },
  { id: "2", title: "Project Sync with @mike", time: "10:00 AM", type: "pending", day: 11 },
  { id: "3", title: "Lunch with @sarah", time: "12:00 PM", type: "coordinated", day: 11 },
  { id: "4", title: "Design Review", time: "2:00 PM", type: "personal", day: 12 },
  { id: "5", title: "1:1 with Manager", time: "3:00 PM", type: "coordinated", day: 13 },
  { id: "6", title: "Planning Session", time: "10:00 AM", type: "personal", day: 14 },
  { id: "7", title: "Client Call", time: "4:00 PM", type: "coordinated", day: 15 },
]

const daysOfWeek = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]

export function CalendarGrid({
  onEventSelect,
}: {
  onEventSelect?: (event: CalendarEvent) => void
}) {
  const [currentDate] = useState(new Date(2024, 2, 11)) // March 11, 2024
  const today = 11

  // Generate calendar days
  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear()
    const month = date.getMonth()
    const firstDay = new Date(year, month, 1).getDay()
    const daysInMonth = new Date(year, month + 1, 0).getDate()
    const days: (number | null)[] = []

    // Empty cells for days before the first of the month
    for (let i = 0; i < firstDay; i++) {
      days.push(null)
    }

    // Days of the month
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i)
    }

    return days
  }

  const days = getDaysInMonth(currentDate)
  const monthName = currentDate.toLocaleString("default", { month: "long" })
  const year = currentDate.getFullYear()

  const getEventsForDay = (day: number) => {
    return events.filter((e) => e.day === day)
  }

  return (
    <div className="bg-[#0A1628] rounded-xl border border-[#1A2F4A] overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-[#1A2F4A] flex items-center justify-between">
        <h2 className="text-lg font-semibold text-foreground font-[family-name:var(--font-display)]">
          {monthName} {year}
        </h2>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            className="w-8 h-8 text-foreground-secondary hover:text-foreground"
          >
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-foreground-secondary hover:text-foreground"
          >
            Today
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="w-8 h-8 text-foreground-secondary hover:text-foreground"
          >
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* Days of week header */}
      <div className="grid grid-cols-7 border-b border-[#1A2F4A]">
        {daysOfWeek.map((day) => (
          <div
            key={day}
            className="p-2 text-center text-xs font-medium text-foreground-muted"
          >
            {day}
          </div>
        ))}
      </div>

      {/* Calendar grid */}
      <div className="grid grid-cols-7">
        {days.map((day, index) => {
          const dayEvents = day ? getEventsForDay(day) : []
          const isToday = day === today

          return (
            <div
              key={index}
              className={cn(
                "min-h-[100px] p-2 border-b border-r border-[#1A2F4A] last:border-r-0",
                index % 7 === 0 && "border-l-0",
                !day && "bg-[#050A14]/50"
              )}
            >
              {day && (
                <>
                  <div
                    className={cn(
                      "w-7 h-7 flex items-center justify-center rounded-full text-sm mb-1",
                      isToday
                        ? "bg-[#00D4FF] text-[#050A14] font-bold ring-2 ring-[#00D4FF]/30"
                        : "text-foreground-secondary"
                    )}
                  >
                    {day}
                  </div>
                  <div className="space-y-1">
                    {dayEvents.slice(0, 2).map((event) => (
                      <button
                        key={event.id}
                        onClick={() => onEventSelect?.(event)}
                        className={cn(
                          "w-full text-left px-1.5 py-0.5 rounded text-xs truncate transition-colors",
                          event.type === "personal" &&
                            "bg-[#00D4FF]/20 text-[#00D4FF] hover:bg-[#00D4FF]/30",
                          event.type === "coordinated" &&
                            "bg-[#7C3AED]/20 text-[#7C3AED] hover:bg-[#7C3AED]/30",
                          event.type === "pending" &&
                            "bg-[#F59E0B]/20 text-[#F59E0B] border border-dashed border-[#F59E0B]/50 hover:bg-[#F59E0B]/30"
                        )}
                      >
                        {event.time.split(" ")[0]} {event.title}
                      </button>
                    ))}
                    {dayEvents.length > 2 && (
                      <p className="text-xs text-foreground-muted px-1.5">
                        +{dayEvents.length - 2} more
                      </p>
                    )}
                  </div>
                </>
              )}
            </div>
          )
        })}
      </div>

      {/* Legend */}
      <div className="p-4 border-t border-[#1A2F4A] flex items-center gap-6 text-xs">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-[#00D4FF]" />
          <span className="text-foreground-secondary">Personal</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-[#7C3AED]" />
          <span className="text-foreground-secondary">Coordinated</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded border border-dashed border-[#F59E0B] bg-[#F59E0B]/20" />
          <span className="text-foreground-secondary">Pending Approval</span>
        </div>
      </div>
    </div>
  )
}
