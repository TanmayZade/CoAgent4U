"use client"

import { CalendarGrid } from "@/components/calendar/calendar-grid"
import { UpcomingEvents } from "@/components/calendar/upcoming-events"
import { AgentCommandInput } from "@/components/agent/agent-command-input"

export default function CalendarPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground font-[family-name:var(--font-display)]">
          Calendar View
        </h1>
        <p className="text-foreground-secondary mt-1">
          Manage your schedule and coordinated events
        </p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        {/* Calendar Grid - 3 columns */}
        <div className="xl:col-span-3">
          <CalendarGrid />
        </div>

        {/* Upcoming Events - 2 columns */}
        <div className="xl:col-span-2">
          <UpcomingEvents />
        </div>
      </div>

      {/* Agent Command Input */}
      <AgentCommandInput />
    </div>
  )
}
