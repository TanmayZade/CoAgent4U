"use client"

import { useState } from "react"
import { Bot, Calendar, CheckCircle2, Clock, AlertTriangle } from "lucide-react"

const useCases = [
  {
    id: "view",
    command: "@CoAgent4U show my schedule today",
    label: "View Schedule",
    response: "Here's your schedule for today:",
    calendar: [
      { time: "9:00 AM", event: "Team Standup", duration: "30m" },
      { time: "10:30 AM", event: "Design Review", duration: "1h" },
      { time: "1:00 PM", event: "Lunch with Alex", duration: "1h" },
      { time: "3:00 PM", event: "Sprint Planning", duration: "2h" },
    ],
    status: "complete",
  },
  {
    id: "add",
    command: "@CoAgent4U add team lunch next Tuesday at noon",
    label: "Add Event",
    response: "I've checked your calendar. No conflicts found.",
    proposal: {
      title: "Team Lunch",
      date: "Tuesday, March 17",
      time: "12:00 PM",
      duration: "1 hour",
    },
    status: "pending",
  },
  {
    id: "coordinate",
    command: "@CoAgent4U schedule meeting with @UserB Friday evening",
    label: "Coordinate",
    response: "Initiating agent-to-agent coordination with UserB's agent...",
    coordination: {
      userA: "Available: 6:00 PM - 9:00 PM",
      userB: "Available: 5:00 PM - 7:00 PM",
      match: "6:00 PM - 7:00 PM",
      slots: ["6:00 PM", "6:15 PM", "6:30 PM"],
    },
    status: "awaiting",
  },
]

export function UseCaseExamples() {
  const [activeCase, setActiveCase] = useState("view")

  const currentCase = useCases.find((uc) => uc.id === activeCase)

  return (
    <section id="use-cases" className="py-24 lg:py-32">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <div className="max-w-2xl mx-auto text-center mb-16">
          <p className="text-sm font-medium text-primary mb-3">
            Real Use Case Examples
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            See it in action
          </h2>
          <p className="text-muted-foreground text-lg">
            Simple commands, powerful coordination. Here's how users interact with their personal agent.
          </p>
        </div>

        {/* Use case selector */}
        <div className="flex flex-wrap justify-center gap-2 mb-10">
          {useCases.map((uc) => (
            <button
              key={uc.id}
              onClick={() => setActiveCase(uc.id)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                activeCase === uc.id
                  ? "bg-foreground text-background"
                  : "bg-muted text-muted-foreground hover:text-foreground"
              }`}
            >
              {uc.label}
            </button>
          ))}
        </div>

        {/* Use case display */}
        <div className="max-w-3xl mx-auto">
          <div className="rounded-2xl border border-border/60 bg-card overflow-hidden shadow-lg shadow-black/[0.02]">
            {/* Header */}
            <div className="px-5 py-3 bg-muted/30 border-b border-border/40 flex items-center gap-2">
              <Bot className="w-4 h-4 text-primary" />
              <span className="text-sm font-medium text-foreground">CoAgent4U</span>
            </div>

            {/* Content */}
            <div className="p-6 space-y-5">
              {/* Command */}
              <div className="rounded-xl bg-muted/50 p-4">
                <p className="text-xs text-muted-foreground mb-1.5">Your command:</p>
                <p className="font-mono text-sm text-foreground">{currentCase?.command}</p>
              </div>

              {/* Response */}
              <div className="rounded-xl border border-primary/20 bg-primary/[0.02] p-4">
                <p className="text-xs text-primary mb-1.5">Agent response:</p>
                <p className="text-sm text-foreground">{currentCase?.response}</p>
              </div>

              {/* Calendar view */}
              {currentCase?.calendar && (
                <div className="rounded-xl border border-border/60 p-4">
                  <div className="flex items-center gap-2 mb-4">
                    <Calendar className="w-4 h-4 text-muted-foreground" />
                    <span className="text-sm font-medium text-foreground">Today's Schedule</span>
                  </div>
                  <div className="space-y-2">
                    {currentCase.calendar.map((item, i) => (
                      <div key={i} className="flex items-center justify-between py-2 px-3 bg-muted/30 rounded-lg">
                        <div className="flex items-center gap-3">
                          <span className="text-xs font-mono text-muted-foreground w-16">{item.time}</span>
                          <span className="text-sm text-foreground">{item.event}</span>
                        </div>
                        <span className="text-xs text-muted-foreground">{item.duration}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Event proposal */}
              {currentCase?.proposal && (
                <div className="rounded-xl border border-border/60 p-4">
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-sm font-medium text-foreground">Event Proposal</span>
                    <span className="text-xs px-2 py-1 rounded-full bg-yellow-500/10 text-yellow-600">
                      Pending Approval
                    </span>
                  </div>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Title</span>
                      <span className="text-foreground">{currentCase.proposal.title}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Date</span>
                      <span className="text-foreground">{currentCase.proposal.date}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Time</span>
                      <span className="text-foreground">{currentCase.proposal.time}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Duration</span>
                      <span className="text-foreground">{currentCase.proposal.duration}</span>
                    </div>
                  </div>
                  <div className="flex gap-2 mt-4">
                    <button className="flex-1 py-2 text-sm font-medium bg-foreground text-background rounded-lg hover:bg-foreground/90 transition-colors">
                      Approve
                    </button>
                    <button className="flex-1 py-2 text-sm font-medium border border-border rounded-lg hover:bg-muted transition-colors">
                      Decline
                    </button>
                  </div>
                </div>
              )}

              {/* Coordination view */}
              {currentCase?.coordination && (
                <div className="rounded-xl border border-border/60 p-4">
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-sm font-medium text-foreground">Agent Coordination Status</span>
                    <span className="text-xs px-2 py-1 rounded-full bg-blue-500/10 text-blue-600">
                      Awaiting UserB Approval
                    </span>
                  </div>
                  <div className="space-y-3">
                    <div className="flex items-center gap-3 text-sm">
                      <Clock className="w-4 h-4 text-muted-foreground" />
                      <span className="text-muted-foreground">Your availability:</span>
                      <span className="text-foreground">{currentCase.coordination.userA}</span>
                    </div>
                    <div className="flex items-center gap-3 text-sm">
                      <Clock className="w-4 h-4 text-muted-foreground" />
                      <span className="text-muted-foreground">UserB availability:</span>
                      <span className="text-foreground">{currentCase.coordination.userB}</span>
                    </div>
                    <div className="flex items-center gap-3 text-sm pt-2 border-t border-border/40">
                      <CheckCircle2 className="w-4 h-4 text-green-500" />
                      <span className="text-muted-foreground">Common window:</span>
                      <span className="text-foreground font-medium">{currentCase.coordination.match}</span>
                    </div>
                    <div className="pt-2">
                      <p className="text-xs text-muted-foreground mb-2">Slot options sent to UserB:</p>
                      <div className="flex gap-2">
                        {currentCase.coordination.slots.map((slot, i) => (
                          <span key={i} className="px-2 py-1 text-xs bg-muted rounded-md text-foreground">
                            {slot}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Status indicator */}
              <div className="flex items-center gap-2 pt-2">
                {currentCase?.status === "complete" && (
                  <>
                    <CheckCircle2 className="w-4 h-4 text-green-500" />
                    <span className="text-xs text-muted-foreground">Schedule retrieved successfully</span>
                  </>
                )}
                {currentCase?.status === "pending" && (
                  <>
                    <AlertTriangle className="w-4 h-4 text-yellow-500" />
                    <span className="text-xs text-muted-foreground">Waiting for your approval</span>
                  </>
                )}
                {currentCase?.status === "awaiting" && (
                  <>
                    <Clock className="w-4 h-4 text-blue-500" />
                    <span className="text-xs text-muted-foreground">UserB selects slot, then both users approve</span>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
