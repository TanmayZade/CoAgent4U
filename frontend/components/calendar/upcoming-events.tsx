"use client"

import { GlowCard } from "@/components/common/glow-card"
import { Calendar, Clock, Users, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface Event {
  id: string
  title: string
  date: string
  time: string
  type: "personal" | "coordinated" | "pending"
  participants?: string[]
}

const events: Event[] = [
  {
    id: "1",
    title: "Team Standup",
    date: "Today",
    time: "9:00 AM",
    type: "personal",
  },
  {
    id: "2",
    title: "Project Sync",
    date: "Today",
    time: "10:00 AM",
    type: "pending",
    participants: ["@mike-wilson"],
  },
  {
    id: "3",
    title: "Lunch with Sarah",
    date: "Today",
    time: "12:00 PM",
    type: "coordinated",
    participants: ["@sarah-chen"],
  },
  {
    id: "4",
    title: "Design Review",
    date: "Tomorrow",
    time: "2:00 PM",
    type: "personal",
  },
  {
    id: "5",
    title: "1:1 with Manager",
    date: "Wed, Mar 13",
    time: "3:00 PM",
    type: "coordinated",
    participants: ["@jane-smith"],
  },
  {
    id: "6",
    title: "Planning Session",
    date: "Thu, Mar 14",
    time: "10:00 AM",
    type: "personal",
  },
  {
    id: "7",
    title: "Client Call",
    date: "Fri, Mar 15",
    time: "4:00 PM",
    type: "coordinated",
    participants: ["@client-team"],
  },
]

export function UpcomingEvents() {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-cream font-[family-name:var(--font-display)]">
          Upcoming Events
        </h2>
        <Button
          size="sm"
          className="bg-accent hover:bg-accent-light text-charcoal"
        >
          <Plus className="w-4 h-4 mr-1" />
          Add Event
        </Button>
      </div>

      <div className="space-y-3 max-h-[600px] overflow-y-auto pr-2">
        {events.map((event, index) => (
          <GlowCard
            key={event.id}
            className={cn(
              "p-4 cursor-pointer",
              "animate-[fadeInUp_0.4s_ease-out_forwards]"
            )}
            style={{ animationDelay: `${index * 50}ms`, opacity: 0 } as React.CSSProperties}
          >
            <div className="flex items-start justify-between mb-2">
              <h3 className="font-medium text-cream">{event.title}</h3>
              <span
                className={cn(
                  "text-xs px-2 py-0.5 rounded-full",
                  event.type === "personal" &&
                    "bg-accent/10 text-accent",
                  event.type === "coordinated" &&
                    "bg-emerald-500/10 text-emerald-500",
                  event.type === "pending" && "bg-amber-500/10 text-amber-500"
                )}
              >
                {event.type === "pending" ? "Pending" : event.type}
              </span>
            </div>

            <div className="space-y-2 text-sm text-cream/70">
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                <span>{event.date}</span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4" />
                <span>{event.time}</span>
              </div>
              {event.participants && (
                <div className="flex items-center gap-2">
                  <Users className="w-4 h-4" />
                  <span className="text-accent">
                    {event.participants.join(", ")}
                  </span>
                </div>
              )}
            </div>
          </GlowCard>
        ))}
      </div>
    </div>
  )
}
