"use client"

import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useUser } from "../layout"
import { activityAPI, AgentActivityEntry } from "@/lib/api/activity"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Download, Search, Filter, ChevronLeft, ChevronRight, ChevronDown, ChevronRight as ChevronRightSm, Info, X } from "lucide-react"
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { Badge } from "@/components/ui/badge"

function LevelChip({ level }: { level: AgentActivityEntry['level'] }) {
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

function LogRow({ log }: { log: AgentActivityEntry }) {
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
          {log.eventType}
        </td>
        <td className="px-6 py-3 text-foreground/80 text-sm max-w-[21rem]">
          <div className="flex items-center gap-2">
            <span className="truncate">{log.description}</span>
            <HoverCard>
              <HoverCardTrigger asChild>
                <button
                  className="text-foreground/40 hover:text-foreground/80 focus:outline-none shrink-0 cursor-help"
                  aria-label="View full description"
                >
                  <Info className="w-4 h-4" />
                </button>
              </HoverCardTrigger>
              <HoverCardContent side="top" className="w-[400px] text-sm leading-relaxed overflow-hidden text-ellipsis z-50">
                {log.description}
              </HoverCardContent>
            </HoverCard>
          </div>
        </td>
        <td className="px-6 py-3 whitespace-nowrap text-right">
          <LevelChip level={log.level} />
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={5} className="px-6 py-3 bg-muted/10 border-b border-border/50">
            <div className="flex flex-col gap-1 text-[10px] text-foreground/50 font-mono uppercase tracking-widest pl-6">
              <span>Correlation ID: {log.correlationId || 'none'}</span>
              <span>Coordination ID: {log.coordinationId || 'none'}</span>
            </div>
          </td>
        </tr>
      )}
    </>
  )
}

export default function AgentActivityPage() {
  const { user } = useUser()
  const [page, setPage] = useState(0)
  const [levelFilter, setLevelFilter] = useState("ALL")
  const [eventTypeFilter, setEventTypeFilter] = useState("ALL")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [searchQuery, setSearchQuery] = useState("")

  const activeFiltersCount = [
    levelFilter !== "ALL",
    eventTypeFilter !== "ALL",
    startDate !== "",
    endDate !== ""
  ].filter(Boolean).length;

  const resetFilters = () => {
    setLevelFilter("ALL");
    setEventTypeFilter("ALL");
    setStartDate("");
    setEndDate("");
    setPage(0);
  };

  const { data, isLoading } = useQuery({
    queryKey: ['activity', user?.username, page, levelFilter, eventTypeFilter, startDate, endDate],
    queryFn: () => activityAPI.getLogs(
      user!.username, 
      page, 
      50, 
      levelFilter, 
      eventTypeFilter,
      startDate ? new Date(startDate).toISOString() : undefined,
      endDate ? new Date(endDate).toISOString() : undefined
    ),
    enabled: !!user?.username
  })

  // Basic client-side search across specific page logs
  const filteredData = data?.content.filter(item => 
    searchQuery === "" || 
    item.eventType.toLowerCase().includes(searchQuery.toLowerCase()) ||
    item.description.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleExport = () => {
    if (!user) return
    const url = activityAPI.exportLogsUrl(user.username)
    window.open(url, '_blank')
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Agent Activity</h1>
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
          <div className="p-4 border-b border-border/50 flex flex-col xl:flex-row gap-4 items-center justify-between bg-muted/20">
            {/* Search */}
            <div className="relative w-full xl:max-w-md shrink-0">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-foreground/40" />
              <Input 
                placeholder="Search action or payload..." 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 bg-background/50 border-border/50"
              />
            </div>
            
            <div className="flex items-center gap-3">
              {activeFiltersCount > 0 && (
                <Button 
                  variant="ghost" 
                  size="sm" 
                  onClick={resetFilters}
                  className="text-foreground/60 hover:text-foreground h-9 px-2 hidden sm:flex"
                >
                  <X className="w-4 h-4 mr-1" />
                  Clear Filters
                </Button>
              )}
              
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="border-border/50 gap-2 h-9 text-foreground/80 font-normal">
                    <Filter className="w-4 h-4" />
                    Filters
                    {activeFiltersCount > 0 && (
                      <Badge variant="secondary" className="ml-1 h-5 rounded-sm px-1 font-normal text-xs bg-primary/10 text-primary">
                        {activeFiltersCount}
                      </Badge>
                    )}
                  </Button>
                </PopoverTrigger>
                <PopoverContent align="end" className="w-[340px] p-4 bg-card border-border/50 shadow-lg mt-1 space-y-4">
                  <div className="space-y-2">
                    <h4 className="font-medium text-sm leading-none">Filters</h4>
                    <p className="text-xs text-foreground/60">
                      Refine the agent activity trail below.
                    </p>
                  </div>
                  <div className="grid gap-4 pt-2">
                    {/* Event Type */}
                    <div className="grid gap-1.5">
                      <label className="text-xs font-semibold text-foreground/70 uppercase tracking-wider">Event Type</label>
                      <select
                        value={eventTypeFilter}
                        onChange={(e) => { setEventTypeFilter(e.target.value); setPage(0); }}
                        className="w-full px-3 py-2 bg-background/50 border border-border/50 rounded-md text-sm outline-none text-foreground"
                      >
                        <option value="ALL">All Events</option>
                        <option value="AgentActivated">Agent Activated</option>
                        <option value="IntentParsed">Intent Parsed</option>
                        <option value="UnrecognizedIntent">Unrecognized Intent</option>
                        <option value="LLMFallbackTriggered">LLM Fallback Triggered</option>
                        <option value="PersonalApprovalRequested">Personal Approval Requested</option>
                        <option value="PersonalEventCreated">Personal Event Created</option>
                        <option value="CoordinationInitiated">Coordination Initiated</option>
                        <option value="CoordinationRequestReceived">Coordination Request Received</option>
                        <option value="SlotsProposed">Slots Proposed</option>
                        <option value="SlotsReceived">Slots Received</option>
                        <option value="ApprovalDecisionMade">Approval Decision Made</option>
                        <option value="CoordinationStateChanged">Coordination State Changed</option>
                        <option value="CoordinationCompleted">Coordination Completed</option>
                        <option value="CoordinationFailed">Coordination Failed</option>
                      </select>
                    </div>

                    {/* Level */}
                    <div className="grid gap-1.5">
                      <label className="text-xs font-semibold text-foreground/70 uppercase tracking-wider">Severity Level</label>
                      <select
                        value={levelFilter}
                        onChange={(e) => { setLevelFilter(e.target.value); setPage(0); }}
                        className="w-full px-3 py-2 bg-background/50 border border-border/50 rounded-md text-sm outline-none text-foreground"
                      >
                        <option value="ALL">All Levels</option>
                        <option value="INFO">Info</option>
                        <option value="SUCCESS">Success</option>
                        <option value="WARNING">Warning</option>
                        <option value="ERROR">Error</option>
                      </select>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      {/* Date Range Start */}
                      <div className="grid gap-1.5">
                        <label className="text-xs font-semibold text-foreground/70 uppercase tracking-wider">Start Date</label>
                        <Input
                          type="date"
                          value={startDate}
                          onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
                          className="bg-background/50 border-border/50 text-sm h-9 px-2"
                        />
                      </div>
                      
                      {/* Date Range End */}
                      <div className="grid gap-1.5">
                        <label className="text-xs font-semibold text-foreground/70 uppercase tracking-wider">End Date</label>
                        <Input
                          type="date"
                          value={endDate}
                          onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
                          className="bg-background/50 border-border/50 text-sm h-9 px-2"
                        />
                      </div>
                    </div>
                  </div>
                </PopoverContent>
              </Popover>
            </div>
          </div>

        {/* Data Table */}
        <div className="overflow-x-auto min-h-[400px]">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/30 text-foreground/60 text-xs uppercase font-medium">
              <tr>
                <th className="px-6 py-4 w-8"></th>
                <th className="px-6 py-4 font-medium tracking-wider">Timestamp</th>
                <th className="px-6 py-4 font-medium tracking-wider">Event Type</th>
                <th className="px-6 py-4 font-medium tracking-wider">Description</th>
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
        {data && (
          <div className="p-4 border-t border-border/50 flex items-center justify-between text-sm text-foreground/60 bg-muted/20">
            <div>
              Showing {data.totalElements === 0 ? 0 : page * 50 + 1}-{Math.min((page + 1) * 50, data.totalElements)} of {data.totalElements} entries
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
