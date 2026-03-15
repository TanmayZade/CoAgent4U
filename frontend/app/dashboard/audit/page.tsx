"use client"

import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useUser } from "../layout"
import { auditAPI, AuditLogEntry } from "@/lib/api/audit"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Download, Search, Filter, ChevronLeft, ChevronRight, ChevronDown, ChevronRight as ChevronRightSm } from "lucide-react"

function LevelChip({ level }: { level: AuditLogEntry['level'] }) {
  const colors = {
    INFO: "bg-blue-500/10 text-blue-500 border-blue-500/20",
    SUCCESS: "bg-emerald-500/10 text-emerald-500 border-emerald-500/20",
    WARNING: "bg-amber-500/10 text-amber-500 border-amber-500/20",
    ERROR: "bg-rose-500/10 text-rose-500 border-rose-500/20"
  }
  
  return (
    <span className={`px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider border ${colors[level]}`}>
      {level}
    </span>
  )
}

function LogRow({ log }: { log: AuditLogEntry }) {
  const [expanded, setExpanded] = useState(false)

  return (
    <>
      <tr className="hover:bg-muted/20 transition-colors cursor-pointer group" onClick={() => setExpanded(!expanded)}>
        <td className="px-6 py-3 w-8">
          {expanded ? <ChevronDown className="w-4 h-4 text-foreground/40" /> : <ChevronRightSm className="w-4 h-4 text-foreground/40 group-hover:text-primary" />}
        </td>
        <td className="px-6 py-3 whitespace-nowrap text-foreground/70 font-mono text-xs">
          {new Date(log.occurredAt).toLocaleString()}
        </td>
        <td className="px-6 py-3 whitespace-nowrap font-medium text-foreground">
          {log.action}
        </td>
        <td className="px-6 py-3 whitespace-nowrap text-foreground/60 font-mono text-xs">
          {log.entityType ? `${log.entityType}:${log.entityId?.substring(0,8)}` : '—'}
        </td>
        <td className="px-6 py-3 whitespace-nowrap text-right">
          <LevelChip level={log.level} />
        </td>
      </tr>
      {expanded && log.payload && (
        <tr>
          <td colSpan={5} className="px-6 py-4 bg-muted/10 border-b border-border/50">
            <pre className="p-4 rounded-lg bg-black/40 border border-border/30 text-[11px] font-mono text-primary/80 overflow-x-auto">
              {JSON.stringify(log.payload, null, 2)}
            </pre>
            <div className="flex justify-end mt-2">
              <span className="text-[10px] text-foreground/40 font-mono uppercase tracking-widest">
                Correlation ID: {log.correlationId || 'none'}
              </span>
            </div>
          </td>
        </tr>
      )}
    </>
  )
}

export default function AuditLogPage() {
  const { user } = useUser()
  const [page, setPage] = useState(0)
  const [levelFilter, setLevelFilter] = useState("ALL")
  const [searchQuery, setSearchQuery] = useState("")

  const { data, isLoading } = useQuery({
    queryKey: ['audit', user?.username, page, levelFilter],
    queryFn: () => auditAPI.getLogs(user!.username, page, 50, levelFilter),
    enabled: !!user?.username,
    keepPreviousData: true
  })

  // Basic client-side search across specific page logs
  const filteredData = data?.content.filter(item => 
    searchQuery === "" || 
    item.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
    JSON.stringify(item.payload || {}).toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleExport = () => {
    if (!user) return
    const url = auditAPI.exportLogsUrl(user.username)
    window.open(url, '_blank')
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Audit Log</h1>
          <p className="text-foreground/60 text-sm mt-1">
            Complete visibility into every action taken by the agent or system.
          </p>
        </div>
        <Button onClick={handleExport} variant="outline" className="border-border/50 gap-2">
          <Download className="w-4 h-4" />
          Export JSON
        </Button>
      </div>

      <div className="rounded-xl border border-border/50 bg-card overflow-hidden shadow-sm">
        {/* Filter Bar */}
        <div className="p-4 border-b border-border/50 flex flex-col sm:flex-row gap-4 items-center justify-between bg-muted/20">
          <div className="relative w-full sm:max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-foreground/40" />
            <Input 
              placeholder="Search action or payload..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 bg-background/50 border-border/50"
            />
          </div>
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Filter className="h-4 w-4 text-foreground/40" />
            <select
              value={levelFilter}
              onChange={(e) => { setLevelFilter(e.target.value); setPage(0); }}
              className="px-3 py-2 bg-background/50 border border-border/50 rounded-md text-sm outline-none w-full sm:w-auto text-foreground"
            >
              <option value="ALL">All Levels</option>
              <option value="INFO">Info</option>
              <option value="SUCCESS">Success</option>
              <option value="WARNING">Warning</option>
              <option value="ERROR">Error</option>
            </select>
          </div>
        </div>

        {/* Data Table */}
        <div className="overflow-x-auto min-h-[400px]">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/30 text-foreground/60 text-xs uppercase font-medium">
              <tr>
                <th className="px-6 py-4 w-8"></th>
                <th className="px-6 py-4 font-medium tracking-wider">Timestamp</th>
                <th className="px-6 py-4 font-medium tracking-wider">Action</th>
                <th className="px-6 py-4 font-medium tracking-wider">Entity</th>
                <th className="px-6 py-4 font-medium tracking-wider text-right">Level</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {isLoading && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-foreground/50">
                    <div className="flex justify-center mb-4">
                      <div className="w-6 h-6 rounded-full border-2 border-primary border-t-transparent animate-spin" />
                    </div>
                    Loading audit trail...
                  </td>
                </tr>
              )}
              
              {!isLoading && filteredData?.map((log) => (
                <LogRow key={log.logId} log={log} />
              ))}
              
              {!isLoading && filteredData?.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-foreground/50">
                    No logs found matching the current filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="p-4 border-t border-border/50 flex items-center justify-between text-sm text-foreground/60 bg-muted/20">
            <div>
              Showing {page * 50 + 1}-{Math.min((page + 1) * 50, data.totalElements)} of {data.totalElements} entries
            </div>
            <div className="flex items-center gap-1">
              <Button 
                variant="outline" 
                size="icon" 
                className="h-8 w-8" 
                disabled={data.isFirst}
                onClick={() => setPage(page - 1)}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="px-4 font-medium text-foreground">
                Page {page + 1} of {data.totalPages}
              </span>
              <Button 
                variant="outline" 
                size="icon" 
                className="h-8 w-8" 
                disabled={data.isLast}
                onClick={() => setPage(page + 1)}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
