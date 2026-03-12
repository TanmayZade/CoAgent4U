"use client"

import { GlowCard } from "@/components/common/glow-card"
import { Check, X, Clock, Calendar } from "lucide-react"
import { cn } from "@/lib/utils"

interface ParticipantCardProps {
  name: string
  handle: string
  avatarInitials: string
  role: "initiator" | "recipient"
  approvalStatus: "pending" | "approved" | "rejected"
  availability?: string
  isCurrentUser?: boolean
}

export function ParticipantCard({
  name,
  handle,
  avatarInitials,
  role,
  approvalStatus,
  availability = "Mon-Fri, 9AM-5PM",
  isCurrentUser = false,
}: ParticipantCardProps) {
  return (
    <GlowCard className="p-5">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="w-12 h-12 rounded-full bg-charcoal-lighter flex items-center justify-center border border-charcoal-light">
              <span className="text-sm font-bold text-cream">{avatarInitials}</span>
            </div>
            {approvalStatus === "approved" && (
              <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-500 flex items-center justify-center">
                <Check className="w-3 h-3 text-white" />
              </div>
            )}
            {approvalStatus === "rejected" && (
              <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-red-500 flex items-center justify-center">
                <X className="w-3 h-3 text-white" />
              </div>
            )}
          </div>
          <div>
            <p className="font-medium text-cream flex items-center gap-2">
              {name}
              {isCurrentUser && (
                <span className="text-xs text-accent px-1.5 py-0.5 rounded bg-accent/10">
                  You
                </span>
              )}
            </p>
            <p className="text-sm text-accent">{handle}</p>
          </div>
        </div>
        <span
          className={cn(
            "text-xs px-2 py-1 rounded-full",
            role === "initiator"
              ? "bg-accent/10 text-accent"
              : "bg-cream/10 text-cream/70"
          )}
        >
          {role === "initiator" ? "Initiator" : "Recipient"}
        </span>
      </div>

      <div className="space-y-3">
        {/* Calendar availability */}
        <div className="flex items-center gap-2 text-sm text-cream/70">
          <Calendar className="w-4 h-4" />
          <span>{availability}</span>
        </div>

        {/* Approval status */}
        <div
          className={cn(
            "flex items-center gap-2 px-3 py-2 rounded-lg",
            approvalStatus === "approved"
              ? "bg-emerald-500/10 border border-emerald-500/20"
              : approvalStatus === "rejected"
              ? "bg-red-500/10 border border-red-500/20"
              : "bg-amber-500/10 border border-amber-500/20"
          )}
        >
          {approvalStatus === "approved" ? (
            <>
              <Check className="w-4 h-4 text-emerald-500" />
              <span className="text-sm text-emerald-500">Approved</span>
            </>
          ) : approvalStatus === "rejected" ? (
            <>
              <X className="w-4 h-4 text-red-500" />
              <span className="text-sm text-red-500">Rejected</span>
            </>
          ) : (
            <>
              <Clock className="w-4 h-4 text-amber-500" />
              <span className="text-sm text-amber-500">Pending Approval</span>
            </>
          )}
        </div>
      </div>
    </GlowCard>
  )
}
