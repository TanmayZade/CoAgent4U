"use client"

import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useUser } from "../layout"
import { coordinationsAPI } from "@/lib/api/coordinations"
import { StatusChip } from "@/components/ui/status-chip"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Search, Filter, ChevronLeft, ChevronRight, ArrowRight } from "lucide-react"
import Link from "next/link"

export default function CoordinationsPage() {
  const { user } = useUser()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState("ALL")
  const [searchQuery, setSearchQuery] = useState("")

  const { data, isLoading } = useQuery({
    queryKey: ['coordinations', user?.username, page, statusFilter],
    queryFn: () => coordinationsAPI.getHistory(user!.username, page, 20, statusFilter),
    enabled: !!user?.username,
    keepPreviousData: true
  })

  // Basic client-side search across the current page results (real impl would search backend)
  const filteredData = data?.content.filter(item => 
    searchQuery === "" || 
    item.contactName.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const states = ["ALL", "INITIATED", "MATCHING", "PROPOSAL_GENERATED", "AWAITING_APPROVAL_A", "AWAITING_APPROVAL_B", "APPROVED_BY_BOTH", "COMPLETED", "REJECTED", "FAILED"]

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Coordinations</h1>
          <p className="text-foreground/60 text-sm mt-1">
            Complete history of your agent's scheduling activities.
          </p>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card overflow-hidden shadow-sm">
        {/* Filter Bar */}
        <div className="p-4 border-b border-border/50 flex flex-col sm:flex-row gap-4 items-center justify-between bg-muted/20">
          <div className="relative w-full sm:max-w-xs">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-foreground/40" />
            <Input 
              placeholder="Search by name..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 bg-background/50 border-border/50"
            />
          </div>
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Filter className="h-4 w-4 text-foreground/40" />
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              className="px-3 py-2 bg-background/50 border border-border/50 rounded-md text-sm outline-none w-full sm:w-auto text-foreground"
            >
              {states.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        </div>

        {/* Data Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/30 text-foreground/60 text-xs uppercase font-medium">
              <tr>
                <th className="px-6 py-4 font-medium tracking-wider">Date</th>
                <th className="px-6 py-4 font-medium tracking-wider">With</th>
                <th className="px-6 py-4 font-medium tracking-wider">Status</th>
                <th className="px-6 py-4 font-medium tracking-wider">Proposal Time</th>
                <th className="px-6 py-4 font-medium tracking-wider text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {isLoading && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-foreground/50">
                    <div className="flex justify-center mb-4">
                      <div className="w-6 h-6 rounded-full border-2 border-primary border-t-transparent animate-spin" />
                    </div>
                    Loading coordinations...
                  </td>
                </tr>
              )}
              
              {!isLoading && filteredData?.map((coord) => (
                <tr key={coord.coordinationId} className="hover:bg-muted/20 transition-colors group">
                  <td className="px-6 py-4 whitespace-nowrap text-foreground font-mono text-xs">
                    {new Date(coord.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center text-primary font-medium text-[10px]">
                        {coord.contactName.substring(0, 2).toUpperCase()}
                      </div>
                      <span className="font-medium text-foreground">{coord.contactName}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <StatusChip state={coord.status} />
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-foreground/70 font-mono text-xs">
                    {coord.proposalTime ? new Date(coord.proposalTime).toLocaleString() : '—'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right">
                    <Button asChild variant="ghost" size="icon" className="h-8 w-8 text-foreground/40 group-hover:text-primary transition-colors">
                      <Link href={`/dashboard/coordinations/${coord.coordinationId}`}>
                        <ArrowRight className="h-4 w-4" />
                      </Link>
                    </Button>
                  </td>
                </tr>
              ))}
              
              {!isLoading && filteredData?.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-foreground/50">
                    No coordinations found matching the criteria.
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
              Showing {page * 20 + 1}-{Math.min((page + 1) * 20, data.totalElements)} of {data.totalElements} entries
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
