"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { ChevronRight, Search } from "lucide-react"
import { cn } from "@/lib/utils"
import { useEffect, useRef } from "react"
import gsap from "gsap"

type CoordinationStatus = 
  | "initiated"
  | "proposal_generated"
  | "awaiting_approval"
  | "approved"
  | "completed"
  | "rejected"

interface Coordination {
  id: string
  type: string
  participants: string[]
  state: CoordinationStatus
  timeSlot: string
  created: string
}

const coordinations: Coordination[] = [
  {
    id: "coord-001",
    type: "Meeting",
    participants: ["@sarah-chen", "@alex-johnson"],
    state: "completed",
    timeSlot: "Mar 10, 3:00 PM",
    created: "2 hours ago",
  },
  {
    id: "coord-002",
    type: "Meeting",
    participants: ["@mike-wilson", "@alex-johnson"],
    state: "awaiting_approval",
    timeSlot: "Mar 11, 10:00 AM",
    created: "1 hour ago",
  },
  {
    id: "coord-003",
    type: "Meeting",
    participants: ["@emily-davis", "@alex-johnson"],
    state: "completed",
    timeSlot: "Mar 9, 2:00 PM",
    created: "1 day ago",
  },
  {
    id: "coord-004",
    type: "Meeting",
    participants: ["@john-doe", "@alex-johnson"],
    state: "rejected",
    timeSlot: "Mar 8, 4:00 PM",
    created: "2 days ago",
  },
  {
    id: "coord-005",
    type: "Meeting",
    participants: ["@jane-smith", "@alex-johnson"],
    state: "initiated",
    timeSlot: "TBD",
    created: "30 min ago",
  },
]

const filterOptions = ["All", "Active", "Completed", "Rejected"]

const statusStyles: Record<CoordinationStatus, { bg: string; text: string; dot: string }> = {
  initiated: { bg: "bg-charcoal-lighter", text: "text-foreground-secondary", dot: "bg-foreground-muted" },
  proposal_generated: { bg: "bg-accent/10", text: "text-accent", dot: "bg-accent" },
  awaiting_approval: { bg: "bg-accent/10", text: "text-accent", dot: "bg-accent" },
  approved: { bg: "bg-accent/10", text: "text-accent", dot: "bg-accent" },
  completed: { bg: "bg-charcoal-lighter", text: "text-cream", dot: "bg-cream" },
  rejected: { bg: "bg-destructive/10", text: "text-destructive", dot: "bg-destructive" },
}

export function CoordinationTable() {
  const tableRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!tableRef.current) return

    const ctx = gsap.context(() => {
      const rows = tableRef.current?.querySelectorAll("tbody tr")
      if (rows) {
        gsap.fromTo(
          rows,
          { y: 20, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.4,
            stagger: 0.05,
            ease: "power3.out",
            delay: 0.2,
          }
        )
      }
    }, tableRef)

    return () => ctx.revert()
  }, [])

  return (
    <div ref={tableRef} className="bg-charcoal-light rounded-xl border border-border overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h3 className="text-lg font-semibold text-cream font-[family-name:var(--font-display)]">
          Coordination History
        </h3>
        <div className="flex items-center gap-3">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-foreground-muted" />
            <input
              type="text"
              placeholder="Search..."
              className="bg-charcoal border border-border rounded-lg pl-9 pr-4 py-2 text-sm text-cream placeholder:text-foreground-muted focus:outline-none focus:border-accent/50 w-48 transition-colors"
            />
          </div>
          {/* Filter */}
          <div className="flex items-center gap-1 bg-charcoal rounded-lg p-1 border border-border">
            {filterOptions.map((option) => (
              <button
                key={option}
                className={cn(
                  "px-3 py-1.5 text-xs font-medium rounded-md transition-colors",
                  option === "All"
                    ? "bg-accent/10 text-accent"
                    : "text-foreground-muted hover:text-cream"
                )}
              >
                {option}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="border-border hover:bg-transparent">
              <TableHead className="text-foreground-muted text-xs font-mono uppercase tracking-wider">Type</TableHead>
              <TableHead className="text-foreground-muted text-xs font-mono uppercase tracking-wider">Participants</TableHead>
              <TableHead className="text-foreground-muted text-xs font-mono uppercase tracking-wider">State</TableHead>
              <TableHead className="text-foreground-muted text-xs font-mono uppercase tracking-wider">Time Slot</TableHead>
              <TableHead className="text-foreground-muted text-xs font-mono uppercase tracking-wider">Created</TableHead>
              <TableHead className="text-foreground-muted text-xs font-mono uppercase tracking-wider text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {coordinations.map((coord) => {
              const style = statusStyles[coord.state]
              return (
                <TableRow
                  key={coord.id}
                  className="border-border hover:bg-charcoal/50 transition-colors"
                >
                  <TableCell className="font-medium text-cream">
                    {coord.type}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      {coord.participants.map((p, i) => (
                        <span key={i} className="text-sm text-accent">
                          {p}
                          {i < coord.participants.length - 1 && (
                            <span className="text-foreground-muted">, </span>
                          )}
                        </span>
                      ))}
                    </div>
                  </TableCell>
                  <TableCell>
                    <span className={cn(
                      "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium",
                      style.bg,
                      style.text
                    )}>
                      <span className={cn("w-1.5 h-1.5 rounded-full", style.dot, 
                        (coord.state === "awaiting_approval" || coord.state === "initiated") && "status-pulse"
                      )} />
                      {coord.state.replace(/_/g, " ")}
                    </span>
                  </TableCell>
                  <TableCell className="font-mono text-sm text-foreground-secondary">
                    {coord.timeSlot}
                  </TableCell>
                  <TableCell className="text-sm text-foreground-muted">
                    {coord.created}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      asChild
                      className="text-foreground-secondary hover:text-accent"
                    >
                      <Link href={`/dashboard/coordinations/${coord.id}`}>
                        View
                        <ChevronRight className="w-4 h-4 ml-1" />
                      </Link>
                    </Button>
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
