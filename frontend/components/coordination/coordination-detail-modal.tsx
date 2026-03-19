"use client"

import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { X, CheckCircle2, Circle, AlertCircle, XCircle, Clock, ArrowRight, CalendarCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import { CoordinationDetailDto, TimeSlotDto, coordinationsAPI } from "@/lib/api/coordinations"

// ── State color map ───────────────────────────────────────────────────────────
const stateConfig: Record<string, { bg: string; border: string; text: string; icon: React.ReactNode }> = {
  INITIATED:               { bg:"bg-blue-500/15",    border:"border-blue-400/40",    text:"text-blue-400",    icon:<Circle className="w-4 h-4"/> },
  CHECKING_AVAILABILITY_A: { bg:"bg-cyan-500/15",    border:"border-cyan-400/40",    text:"text-cyan-400",    icon:<Clock className="w-4 h-4"/> },
  CHECKING_AVAILABILITY_B: { bg:"bg-cyan-500/15",    border:"border-cyan-400/40",    text:"text-cyan-400",    icon:<Clock className="w-4 h-4"/> },
  MATCHING:                { bg:"bg-indigo-500/15",  border:"border-indigo-400/40",  text:"text-indigo-400",  icon:<ArrowRight className="w-4 h-4"/> },
  PROPOSAL_GENERATED:      { bg:"bg-violet-500/15",  border:"border-violet-400/40",  text:"text-violet-400",  icon:<CalendarCheck className="w-4 h-4"/> },
  AWAITING_APPROVAL_B:     { bg:"bg-amber-500/15",   border:"border-amber-400/40",   text:"text-amber-400",   icon:<AlertCircle className="w-4 h-4"/> },
  AWAITING_APPROVAL_A:     { bg:"bg-amber-500/15",   border:"border-amber-400/40",   text:"text-amber-400",   icon:<AlertCircle className="w-4 h-4"/> },
  APPROVED_BY_BOTH:        { bg:"bg-emerald-500/15", border:"border-emerald-400/40", text:"text-emerald-400", icon:<CheckCircle2 className="w-4 h-4"/> },
  CREATING_EVENT_A:        { bg:"bg-teal-500/15",    border:"border-teal-400/40",    text:"text-teal-400",    icon:<Clock className="w-4 h-4"/> },
  CREATING_EVENT_B:        { bg:"bg-teal-500/15",    border:"border-teal-400/40",    text:"text-teal-400",    icon:<Clock className="w-4 h-4"/> },
  COMPLETED:               { bg:"bg-green-500/15",   border:"border-green-400/40",   text:"text-green-400",   icon:<CheckCircle2 className="w-4 h-4"/> },
  REJECTED:                { bg:"bg-red-500/15",     border:"border-red-400/40",     text:"text-red-400",     icon:<XCircle className="w-4 h-4"/> },
  FAILED:                  { bg:"bg-red-500/15",     border:"border-red-400/40",     text:"text-red-400",     icon:<XCircle className="w-4 h-4"/> },
}
const defaultCfg = { bg:"bg-muted/20", border:"border-border/40", text:"text-foreground/60", icon:<Circle className="w-4 h-4"/> }

function humanizeState(s: string) {
  return s.replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase())
}

// ── Helper: format a UTC instant to local date+time string ───────────────────
function fmt(iso: string) {
  const d = new Date(iso)
  return d.toLocaleDateString(undefined, { weekday:"short", month:"short", day:"numeric" }) + ", " +
         d.toLocaleTimeString(undefined, { hour:"2-digit", minute:"2-digit" })
}

// ── Who should act now? ───────────────────────────────────────────────────────
// PROPOSAL_GENERATED → invitee (INVITEE) must select a slot
// AWAITING_APPROVAL_B → invitee must approve
// AWAITING_APPROVAL_A → requester must approve
function needsSlotSelection(state: string, role: string) {
  return state === "PROPOSAL_GENERATED" && role === "INVITEE"
}
function needsApproval(state: string, role: string) {
  return (state === "AWAITING_APPROVAL_B" && role === "INVITEE") ||
         (state === "AWAITING_APPROVAL_A" && role === "REQUESTER")
}

interface Props {
  detail: CoordinationDetailDto
  username: string
  onClose: () => void
  onCancel: () => Promise<void>
  onApprove: (approved: boolean) => Promise<void>
  onSelectSlot: (slot: TimeSlotDto) => Promise<void>
}

export function CoordinationDetailModal({ detail, username, onClose, onCancel, onApprove, onSelectSlot }: Props) {
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [selectedSlot, setSelectedSlot] = useState<TimeSlotDto | null>(null)

  const isTerminal = ["COMPLETED", "REJECTED", "FAILED"].includes(detail.state)
  const showSlotPicker = needsSlotSelection(detail.state, detail.role)
  const showApproval   = needsApproval(detail.state, detail.role)

  // Fetch slots only when the invitee is at PROPOSAL_GENERATED
  const { data: slots, isLoading: slotsLoading } = useQuery({
    queryKey: ["coordination-slots", detail.coordinationId, username],
    queryFn: () => coordinationsAPI.getSlots(detail.coordinationId, username),
    enabled: showSlotPicker,
  })

  // Other participant info
  const otherName = detail.role === "REQUESTER"
    ? (detail.inviteeDisplayName || detail.inviteeUsername)
    : (detail.requesterDisplayName || detail.requesterUsername)
  const otherAvatar = detail.role === "REQUESTER"
    ? detail.inviteeAvatarUrl
    : detail.requesterAvatarUrl

  const handle = async (key: string, fn: () => Promise<void>) => {
    setActionLoading(key)
    try { await fn() } finally { setActionLoading(null) }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div
        className="relative w-full max-w-lg mx-4 max-h-[88vh] overflow-y-auto rounded-2xl border border-border/40 bg-card shadow-2xl"
        onClick={e => e.stopPropagation()}
      >
        {/* ── Header ───────────────────────────────────── */}
        <div className="sticky top-0 z-10 flex items-center justify-between px-6 py-4 border-b border-border/30 bg-card/95 backdrop-blur-md rounded-t-2xl">
          <div className="flex items-center gap-3">
            {otherAvatar ? (
              <img src={otherAvatar} alt={otherName} className="w-10 h-10 rounded-full object-cover border-2 border-primary/30" />
            ) : (
              <div className="w-10 h-10 rounded-full bg-primary/10 border-2 border-primary/30 flex items-center justify-center text-sm font-bold text-primary">
                {otherName.substring(0,2).toUpperCase()}
              </div>
            )}
            <div>
              <h2 className="text-base font-semibold text-foreground">{detail.proposal?.title || "Coordination"}</h2>
              <p className="text-xs text-foreground/50">with {otherName} · <span className={detail.role === "REQUESTER" ? "text-blue-400" : "text-purple-400"}>{detail.role === "REQUESTER" ? "↗ You initiated" : "↙ You received"}</span></p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-muted/80 transition-colors text-foreground/50 hover:text-foreground">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* ── Meeting Proposal ─────────────────────────── */}
        {detail.proposal && (
          <div className="mx-6 mt-5 p-4 rounded-xl bg-primary/5 border border-primary/15">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div>
                <p className="text-sm font-medium text-foreground">{detail.proposal.title}</p>
                <p className="text-xs text-foreground/50 mt-0.5">
                  {fmt(detail.proposal.startTime)} – {new Date(detail.proposal.endTime).toLocaleTimeString(undefined,{hour:"2-digit",minute:"2-digit"})}
                </p>
              </div>
              <span className="text-xs font-medium text-primary bg-primary/10 px-2.5 py-1 rounded-full">
                {detail.proposal.durationMinutes} min
              </span>
            </div>
          </div>
        )}

        {/* ── Slot selection (invitee at PROPOSAL_GENERATED) ── */}
        {showSlotPicker && (
          <div className="mx-6 mt-5">
            <p className="text-xs font-semibold text-foreground/40 uppercase tracking-wider mb-3">Select a Time Slot</p>
            {slotsLoading ? (
              <p className="text-sm text-foreground/40">Loading available slots…</p>
            ) : !slots || slots.length === 0 ? (
              <p className="text-sm text-foreground/40">No available slots found.</p>
            ) : (
              <div className="flex flex-col gap-2">
                {slots.map((slot, i) => {
                  const isSelected = selectedSlot?.start === slot.start
                  return (
                    <button
                      key={i}
                      onClick={() => setSelectedSlot(slot)}
                      className={`w-full text-left px-4 py-3 rounded-xl border text-sm transition-all ${
                        isSelected
                          ? "border-primary bg-primary/10 text-primary"
                          : "border-border/40 bg-muted/10 text-foreground/70 hover:border-primary/40 hover:bg-primary/5"
                      }`}
                    >
                      <span className="font-medium">{fmt(slot.start)}</span>
                      <span className="text-foreground/40"> – </span>
                      <span>{new Date(slot.end).toLocaleTimeString(undefined,{hour:"2-digit",minute:"2-digit"})}</span>
                    </button>
                  )
                })}
              </div>
            )}
          </div>
        )}

        {/* ── Approval section (AWAITING_APPROVAL for the right user) ── */}
        {showApproval && (
          <div className="mx-6 mt-5 p-4 rounded-xl bg-amber-500/8 border border-amber-400/20">
            <div className="flex items-center gap-2 mb-1">
              <AlertCircle className="w-4 h-4 text-amber-400" />
              <p className="text-sm font-semibold text-amber-400">Your Approval Required</p>
            </div>
            <p className="text-xs text-foreground/50">The agent has prepared a meeting slot based on availability. Please review and approve or reject.</p>
          </div>
        )}

        {/* ── Flow Timeline ────────────────────────────── */}
        <div className="px-6 py-5">
          <p className="text-xs font-semibold text-foreground/40 uppercase tracking-wider mb-4">Coordination Flow</p>
          <div className="relative">
            {detail.stateLog.map((entry, i) => {
              const cfg = stateConfig[entry.toState] || defaultCfg
              const isLast = i === detail.stateLog.length - 1
              return (
                <div key={i} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <div className={`w-8 h-8 rounded-full ${cfg.bg} border ${cfg.border} flex items-center justify-center ${cfg.text} shrink-0`}>
                      {cfg.icon}
                    </div>
                    {!isLast && <div className="w-px h-7 bg-border/30" />}
                  </div>
                  <div className="flex-grow pb-1">
                    <p className={`text-sm font-medium ${cfg.text}`}>{humanizeState(entry.toState)}</p>
                    {entry.reason && <p className="text-xs text-foreground/40 mt-0.5">{entry.reason}</p>}
                    <p className="text-[10px] text-foreground/30 mt-0.5">
                      {new Date(entry.transitionedAt).toLocaleString(undefined,{month:"short",day:"numeric",hour:"2-digit",minute:"2-digit",second:"2-digit"})}
                    </p>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* ── Action Buttons ───────────────────────────── */}
        <div className="sticky bottom-0 px-6 py-4 border-t border-border/30 bg-card/95 backdrop-blur-md rounded-b-2xl flex items-center gap-3 flex-wrap">
          {/* Cancel — non-terminal only */}
          {!isTerminal && (
            <Button
              variant="outline"
              size="sm"
              className="text-destructive border-destructive/30 hover:bg-destructive/10"
              disabled={actionLoading !== null}
              onClick={() => handle("cancel", onCancel)}
            >
              {actionLoading === "cancel" ? "Cancelling…" : "Cancel Coordination"}
            </Button>
          )}

          {/* Slot confirm — invitee at PROPOSAL_GENERATED */}
          {showSlotPicker && (
            <Button
              size="sm"
              className="ml-auto bg-violet-600 hover:bg-violet-700 text-white"
              disabled={!selectedSlot || actionLoading !== null}
              onClick={() => selectedSlot && handle("slot", () => onSelectSlot(selectedSlot))}
            >
              {actionLoading === "slot" ? "Confirming…" : "Confirm Slot"}
            </Button>
          )}

          {/* Approve/Reject — only for the correct participant */}
          {showApproval && (
            <div className="flex items-center gap-2 ml-auto">
              <Button
                variant="outline"
                size="sm"
                className="text-red-500 border-red-500/30 hover:bg-red-500/10"
                disabled={actionLoading !== null}
                onClick={() => handle("reject", () => onApprove(false))}
              >
                {actionLoading === "reject" ? "Rejecting…" : "Reject"}
              </Button>
              <Button
                size="sm"
                className="bg-green-600 hover:bg-green-700 text-white"
                disabled={actionLoading !== null}
                onClick={() => handle("approve", () => onApprove(true))}
              >
                {actionLoading === "approve" ? "Approving…" : "Approve"}
              </Button>
            </div>
          )}

          {/* Close — terminal states */}
          {isTerminal && (
            <Button variant="ghost" size="sm" className="ml-auto" onClick={onClose}>Close</Button>
          )}
        </div>
      </div>
    </div>
  )
}
