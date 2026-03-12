"use client"

import { useState } from "react"
import { GlowCard } from "@/components/common/glow-card"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Search,
  Filter,
  Download,
  ChevronDown,
  ChevronRight,
  Info,
  CheckCircle,
  AlertTriangle,
  XCircle,
} from "lucide-react"
import { cn } from "@/lib/utils"

type LogLevel = "info" | "success" | "warning" | "error"

interface AuditLog {
  id: string
  timestamp: string
  user: string
  action: string
  entity: string
  status: LogLevel
  payload?: Record<string, unknown>
}

const logs: AuditLog[] = [
  {
    id: "log-001",
    timestamp: "2024-03-11 14:27:45",
    user: "alex-johnson",
    action: "COORDINATION_INITIATED",
    entity: "coord-002",
    status: "info",
    payload: { targetUser: "mike-wilson", type: "meeting" },
  },
  {
    id: "log-002",
    timestamp: "2024-03-11 14:27:46",
    user: "system",
    action: "CALENDAR_READ",
    entity: "user_alex",
    status: "success",
    payload: { slots: 8, provider: "google" },
  },
  {
    id: "log-003",
    timestamp: "2024-03-11 14:27:47",
    user: "system",
    action: "CALENDAR_READ",
    entity: "user_mike",
    status: "success",
    payload: { slots: 6, provider: "google" },
  },
  {
    id: "log-004",
    timestamp: "2024-03-11 14:27:48",
    user: "system",
    action: "PROPOSAL_GENERATED",
    entity: "coord-002",
    status: "success",
    payload: { proposedSlot: "2024-03-15T18:00:00Z", duration: 30 },
  },
  {
    id: "log-005",
    timestamp: "2024-03-11 14:27:49",
    user: "system",
    action: "APPROVAL_REQUESTED",
    entity: "user_mike",
    status: "info",
  },
  {
    id: "log-006",
    timestamp: "2024-03-11 14:30:12",
    user: "mike-wilson",
    action: "APPROVAL_GRANTED",
    entity: "coord-002",
    status: "success",
  },
  {
    id: "log-007",
    timestamp: "2024-03-11 14:30:13",
    user: "system",
    action: "APPROVAL_REQUESTED",
    entity: "user_alex",
    status: "info",
  },
  {
    id: "log-008",
    timestamp: "2024-03-11 12:15:30",
    user: "sarah-chen",
    action: "APPROVAL_REJECTED",
    entity: "coord-001",
    status: "error",
    payload: { reason: "Schedule conflict" },
  },
  {
    id: "log-009",
    timestamp: "2024-03-11 11:00:00",
    user: "system",
    action: "OAUTH_REFRESH",
    entity: "google_calendar",
    status: "warning",
    payload: { attempts: 2 },
  },
  {
    id: "log-010",
    timestamp: "2024-03-11 10:30:00",
    user: "alex-johnson",
    action: "CALENDAR_WRITE",
    entity: "event_123",
    status: "success",
    payload: { eventId: "ev_abc123", title: "Team Standup" },
  },
]

const statusConfig = {
  info: {
    icon: Info,
    color: "text-accent",
    bgColor: "bg-accent/10",
  },
  success: {
    icon: CheckCircle,
    color: "text-emerald-500",
    bgColor: "bg-emerald-500/10",
  },
  warning: {
    icon: AlertTriangle,
    color: "text-amber-500",
    bgColor: "bg-amber-500/10",
  },
  error: {
    icon: XCircle,
    color: "text-red-500",
    bgColor: "bg-red-500/10",
  },
}

const filterOptions = ["All", "INFO", "SUCCESS", "WARNING", "ERROR"]

export default function AuditLogPage() {
  const [expandedRow, setExpandedRow] = useState<string | null>(null)
  const [activeFilter, setActiveFilter] = useState("All")

  const filteredLogs =
    activeFilter === "All"
      ? logs
      : logs.filter(
          (log) => log.status.toUpperCase() === activeFilter
        )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-cream font-[family-name:var(--font-display)]">
            Audit Log
          </h1>
          <p className="text-cream/70 mt-1">
            Complete visibility into every agent action
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            className="border-charcoal-light hover:border-accent/50 hover:bg-accent/5"
          >
            <Download className="w-4 h-4 mr-2" />
            Export JSON
          </Button>
          <Button
            variant="outline"
            className="border-charcoal-light hover:border-accent/50 hover:bg-accent/5"
          >
            <Download className="w-4 h-4 mr-2" />
            Export CSV
          </Button>
        </div>
      </div>

      <GlowCard className="overflow-hidden">
        {/* Filter Bar */}
        <div className="p-4 border-b border-charcoal-light flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="flex items-center gap-3">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-cream/40" />
              <input
                type="text"
                placeholder="Search logs..."
                className="bg-charcoal-light border border-charcoal-lighter rounded-lg pl-9 pr-4 py-2 text-sm text-cream placeholder:text-cream/40 focus:outline-none focus:border-accent/50 w-64"
              />
            </div>

            {/* Date Range */}
            <Button
              variant="outline"
              size="sm"
              className="border-charcoal-light text-cream/70"
            >
              <Filter className="w-4 h-4 mr-2" />
              Date Range
            </Button>
          </div>

          {/* Level Filter */}
          <div className="flex items-center gap-1 bg-charcoal-light rounded-lg p-1 border border-charcoal-lighter">
            {filterOptions.map((option) => (
              <button
                key={option}
                onClick={() => setActiveFilter(option)}
                className={cn(
                  "px-3 py-1.5 text-xs font-medium rounded-md transition-colors",
                  activeFilter === option
                    ? option === "All"
                      ? "bg-accent/10 text-accent"
                      : option === "SUCCESS"
                      ? "bg-emerald-500/10 text-emerald-500"
                      : option === "WARNING"
                      ? "bg-amber-500/10 text-amber-500"
                      : option === "ERROR"
                      ? "bg-red-500/10 text-red-500"
                      : "bg-accent/10 text-accent"
                    : "text-cream/50 hover:text-cream"
                )}
              >
                {option}
              </button>
            ))}
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="border-charcoal-light hover:bg-transparent">
                <TableHead className="w-10 text-cream/50"></TableHead>
                <TableHead className="text-cream/50 font-mono">
                  Timestamp
                </TableHead>
                <TableHead className="text-cream/50">User</TableHead>
                <TableHead className="text-cream/50">Action</TableHead>
                <TableHead className="text-cream/50">Entity</TableHead>
                <TableHead className="text-cream/50">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredLogs.map((log) => {
                const StatusIcon = statusConfig[log.status].icon
                const isExpanded = expandedRow === log.id
                const hasPayload = !!log.payload

                return (
                  <>
                    <TableRow
                      key={log.id}
                      className={cn(
                        "border-charcoal-light hover:bg-charcoal-light/50 transition-colors cursor-pointer",
                        isExpanded && "bg-charcoal-light/50"
                      )}
                      onClick={() =>
                        hasPayload &&
                        setExpandedRow(isExpanded ? null : log.id)
                      }
                    >
                      <TableCell>
                        {hasPayload && (
                          <button className="text-cream/50">
                            {isExpanded ? (
                              <ChevronDown className="w-4 h-4" />
                            ) : (
                              <ChevronRight className="w-4 h-4" />
                            )}
                          </button>
                        )}
                      </TableCell>
                      <TableCell className="font-mono text-sm text-cream/70">
                        {log.timestamp}
                      </TableCell>
                      <TableCell className="text-accent">
                        @{log.user}
                      </TableCell>
                      <TableCell className="font-mono text-sm text-cream">
                        {log.action}
                      </TableCell>
                      <TableCell className="font-mono text-sm text-cream/70">
                        {log.entity}
                      </TableCell>
                      <TableCell>
                        <div
                          className={cn(
                            "inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-xs",
                            statusConfig[log.status].bgColor,
                            statusConfig[log.status].color
                          )}
                        >
                          <StatusIcon className="w-3 h-3" />
                          {log.status.toUpperCase()}
                        </div>
                      </TableCell>
                    </TableRow>
                    {isExpanded && log.payload && (
                      <TableRow className="border-charcoal-light bg-charcoal-light/30">
                        <TableCell colSpan={6} className="py-0">
                          <div className="p-4">
                            <p className="text-xs text-cream/50 mb-2">
                              Payload:
                            </p>
                            <pre className="p-3 bg-charcoal rounded-lg border border-charcoal-light text-xs font-mono text-cream/70 overflow-x-auto">
                              {JSON.stringify(log.payload, null, 2)}
                            </pre>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </>
                )
              })}
            </TableBody>
          </Table>
        </div>
      </GlowCard>
    </div>
  )
}
