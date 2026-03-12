"use client"

import Link from "next/link"
import { StateMachineViz } from "@/components/coordination/state-machine-viz"
import { ParticipantCard } from "@/components/coordination/participant-card"
import { EventProposalCard } from "@/components/coordination/event-proposal-card"
import { AuditTimeline } from "@/components/common/audit-timeline"
import { GlowCard } from "@/components/common/glow-card"
import { Button } from "@/components/ui/button"
import { ChevronLeft, Copy } from "lucide-react"

const auditEvents = [
  {
    timestamp: "14:23:01",
    description: "Agent A initiated coordination request",
    type: "info" as const,
  },
  {
    timestamp: "14:23:02",
    description: "Agent A queried User A calendar",
    type: "info" as const,
    payload: { action: "CALENDAR_READ", userId: "user_alex", slots: 8 },
  },
  {
    timestamp: "14:23:03",
    description: "Free slots found: 4 windows",
    type: "success" as const,
    payload: {
      slots: [
        "Fri 3:00-3:30 PM",
        "Fri 4:00-4:30 PM",
        "Fri 6:00-6:30 PM",
        "Mon 10:00-10:30 AM",
      ],
    },
  },
  {
    timestamp: "14:23:04",
    description: "A2A Coordination Engine engaged Agent B",
    type: "info" as const,
  },
  {
    timestamp: "14:23:05",
    description: "Agent B queried User B calendar",
    type: "info" as const,
    payload: { action: "CALENDAR_READ", userId: "user_mike", slots: 6 },
  },
  {
    timestamp: "14:23:06",
    description: "Matching algorithm found 1 common slot",
    type: "success" as const,
  },
  {
    timestamp: "14:23:06",
    description: "Proposal generated: Friday 6:00-6:30 PM",
    type: "success" as const,
    payload: {
      proposedSlot: "2024-03-15T18:00:00Z",
      duration: 30,
      title: "Project Sync",
    },
  },
  {
    timestamp: "14:23:07",
    description: "Approval request sent to User B",
    type: "info" as const,
  },
  {
    timestamp: "14:25:12",
    description: "User B approved the proposal",
    type: "success" as const,
  },
  {
    timestamp: "14:25:13",
    description: "Approval request sent to User A",
    type: "info" as const,
  },
  {
    timestamp: "14:27:45",
    description: "Awaiting User A approval",
    type: "warning" as const,
  },
]

export default function CoordinationDetailPage({
  params,
}: {
  params: { id: string }
}) {
  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            asChild
            className="text-cream/70 hover:text-cream"
          >
            <Link href="/dashboard/coordinations">
              <ChevronLeft className="w-4 h-4 mr-1" />
              Back
            </Link>
          </Button>
          <div className="w-px h-6 bg-charcoal-light" />
          <div>
            <h1 className="text-xl font-bold text-cream font-[family-name:var(--font-display)]">
              Coordination Details
            </h1>
            <div className="flex items-center gap-2 mt-1">
              <span className="text-xs font-mono text-cream/50">
                {params.id}
              </span>
              <button className="text-cream/50 hover:text-accent transition-colors">
                <Copy className="w-3 h-3" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* State Machine Visualization */}
      <GlowCard className="p-6">
        <h2 className="text-lg font-semibold text-cream mb-6 font-[family-name:var(--font-display)]">
          Coordination State
        </h2>
        <StateMachineViz currentStateIndex={4} interactive={false} />
      </GlowCard>

      {/* Main content grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column - Participants */}
        <div className="space-y-4">
          <h3 className="text-sm font-medium text-cream/50">
            Participants
          </h3>
          <ParticipantCard
            name="Alex Johnson"
            handle="@alex-johnson"
            avatarInitials="AJ"
            role="initiator"
            approvalStatus="pending"
            availability="Mon-Fri, 9AM-6PM"
            isCurrentUser
          />
          <ParticipantCard
            name="Mike Wilson"
            handle="@mike-wilson"
            avatarInitials="MW"
            role="recipient"
            approvalStatus="approved"
            availability="Mon-Fri, 10AM-5PM"
          />
        </div>

        {/* Center column - Event Proposal */}
        <div className="space-y-4">
          <h3 className="text-sm font-medium text-cream/50">
            Event Proposal
          </h3>
          <EventProposalCard
            title="Project Sync"
            date="Friday, March 15, 2024"
            time="6:00 PM"
            duration="30 minutes"
            participants={["@alex-johnson", "@mike-wilson"]}
            canApprove={true}
            status="pending"
          />
        </div>

        {/* Right column - Quick Info */}
        <div className="space-y-4">
          <h3 className="text-sm font-medium text-cream/50">
            Quick Info
          </h3>
          <GlowCard className="p-4">
            <div className="space-y-4">
              <div>
                <p className="text-xs text-cream/50 mb-1">Type</p>
                <p className="text-sm text-cream">Meeting</p>
              </div>
              <div>
                <p className="text-xs text-cream/50 mb-1">Created</p>
                <p className="text-sm font-mono text-cream">
                  2024-03-11 14:23:01
                </p>
              </div>
              <div>
                <p className="text-xs text-cream/50 mb-1">Duration</p>
                <p className="text-sm text-cream">4 min 44 sec</p>
              </div>
              <div>
                <p className="text-xs text-cream/50 mb-1">
                  Current State
                </p>
                <p className="text-sm text-amber-500 font-mono">
                  AWAITING_APPROVAL_A
                </p>
              </div>
            </div>
          </GlowCard>
        </div>
      </div>

      {/* Audit Timeline */}
      <AuditTimeline events={auditEvents} />
    </div>
  )
}
