"use client"

import { useQuery } from "@tanstack/react-query"
import { useUser } from "../../layout"
import { coordinationsAPI } from "@/lib/api/coordinations"
import { StatusChip } from "@/components/ui/status-chip"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Clock, Calendar, Check, X, ShieldAlert } from "lucide-react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { CoordinationState } from "@/lib/api/dashboard"

const PROTOCOL_STATES = [
  "INITIATED",
  "CHECKING_AVAILABILITY_A",
  "CHECKING_AVAILABILITY_B",
  "MATCHING",
  "PROPOSAL_GENERATED",
  "AWAITING_APPROVAL_B",
  "AWAITING_APPROVAL_A",
  "APPROVED_BY_BOTH",
  "CREATING_EVENT_A",
  "CREATING_EVENT_B",
  "COMPLETED"
]

export default function CoordinationDetailPage() {
  const { user } = useUser()
  const params = useParams()
  const id = params.id as string

  const { data, isLoading } = useQuery({
    queryKey: ['coordination', id, user?.username],
    queryFn: () => coordinationsAPI.getDetail(id, user!.username),
    enabled: !!user?.username && !!id,
    refetchInterval: 10000 // refresh every 10s
  })

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header & Back tracking */}
      <div className="flex items-center gap-4">
        <Button asChild variant="ghost" size="icon" className="h-8 w-8 text-foreground/60 hover:text-foreground">
          <Link href="/dashboard/coordinations">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div className="flex-1 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-3">
              Meeting with {data?.contactName || '...'}
              {data && <StatusChip state={data.status} />}
            </h1>
            <p className="text-foreground/50 font-mono text-xs mt-1">
              ID: {id}
            </p>
          </div>
          {data && (
            <div className="text-sm text-foreground/60 flex items-center gap-2">
              <Clock className="w-4 h-4" />
              Started {new Date(data.createdAt).toLocaleString()}
            </div>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-foreground/50">
          <div className="flex justify-center mb-4">
            <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
          </div>
          Loading coordination details...
        </div>
      ) : data ? (
        <>
          {/* THE SIGNATURE ELEMENT: STATE MACHINE TIMELINE */}
          <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm overflow-hidden">
            <h3 className="text-lg font-semibold tracking-tight mb-8">State Machine</h3>
            
            <div className="relative pb-4">
              <div className="flex items-center justify-between w-full overflow-x-auto pb-6 scrollbar-thin">
                <div className="relative flex items-center min-w-[800px] w-full px-4">
                  
                  {/* Background track line */}
                  <div className="absolute left-8 right-8 top-1/2 -translate-y-1/2 h-0.5 bg-border/40 z-0"></div>

                  {PROTOCOL_STATES.map((stateInfo, index) => {
                    const isTerminalRejection = (data.status === 'REJECTED' || data.status === 'FAILED')
                    const isActive = data.status === stateInfo || 
                                    (isTerminalRejection && stateInfo === data.status)
                    const isPast = PROTOCOL_STATES.indexOf(data.status) > index && !isTerminalRejection
                    const stateLog = data.stateLogs?.find((log: any) => log.toState === stateInfo)

                    let colorClass = "bg-muted border-border/50 text-foreground/40"
                    if (isActive) {
                      colorClass = isTerminalRejection 
                        ? "bg-rose-500/20 border-rose-500 shadow-[0_0_15px_rgba(244,63,94,0.3)] text-rose-500 animate-pulse"
                        : "bg-primary/20 border-primary shadow-[0_0_15px_rgba(129,140,248,0.3)] text-primary animate-pulse"
                    } else if (isPast) {
                      colorClass = "bg-primary border-primary text-primary-foreground"
                    }

                    return (
                      <div key={stateInfo} className="relative z-10 flex flex-col items-center flex-1">
                        <div className={`w-4 h-4 rounded-full border-2 ${colorClass} transition-all duration-500`}></div>
                        <div className="absolute top-6 w-32 text-center">
                          <p className={`text-[9px] font-mono leading-tight ${isActive || isPast ? 'text-foreground' : 'text-foreground/40'}`}>
                            {stateInfo.replace(/_/g, ' ')}
                          </p>
                          {stateLog && (
                            <p className="text-[10px] text-foreground/50 mt-1 font-mono">
                              {new Date(stateLog.occurredAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                            </p>
                          )}
                        </div>
                      </div>
                    )
                  })}
                  
                  {/* Append Terminal Error Node dynamically if applicable */}
                  {(data.status === 'REJECTED' || data.status === 'FAILED') && (
                     <div className="relative z-10 flex flex-col items-center ml-4">
                        <div className="absolute right-full top-1/2 -translate-y-1/2 h-0.5 bg-rose-500/40 w-8 -z-10"></div>
                        <div className={`w-4 h-4 rounded-full border-2 bg-rose-500/20 border-rose-500 shadow-[0_0_15px_rgba(244,63,94,0.3)] text-rose-500 animate-pulse transition-all duration-500 flex items-center justify-center`}>
                          <ShieldAlert className="w-2.5 h-2.5" />
                        </div>
                        <div className="absolute top-6 w-32 text-center">
                          <p className={`text-[9px] font-mono leading-tight text-rose-500`}>
                            {data.status}
                          </p>
                        </div>
                     </div>
                  )}

                </div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Proposal Context */}
            {data.proposalTime && (
              <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm">
                <h3 className="text-lg font-semibold tracking-tight mb-4 flex items-center gap-2">
                  <Calendar className="w-5 h-5 text-primary" />
                  Proposed Schedule
                </h3>
                <div className="bg-muted/20 p-4 rounded-lg border border-border/50">
                  <p className="text-foreground text-lg mb-1">{new Date(data.proposalTime).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
                  <p className="text-primary font-mono text-xl">{new Date(data.proposalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                </div>
              </div>
            )}

            {/* Approvals History (if any) */}
            <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm">
              <h3 className="text-lg font-semibold tracking-tight mb-4 flex items-center gap-2">
                <Check className="w-5 h-5 text-emerald-500" />
                Approval Context
              </h3>
              <div className="space-y-3">
                {/* Simulated dummy data for the approval trace if not available yet on backend */}
                <div className="flex items-center justify-between p-3 rounded-lg border border-emerald-500/20 bg-emerald-500/5">
                  <span className="text-sm font-medium">You</span>
                  <div className="flex items-center gap-2 text-xs">
                    <span className="text-emerald-500 bg-emerald-500/10 px-2 py-0.5 rounded-full font-medium">Approved</span>
                    <span className="text-foreground/50 font-mono">14:03 PM</span>
                  </div>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg border border-emerald-500/20 bg-emerald-500/5">
                  <span className="text-sm font-medium">{data.contactName}</span>
                  <div className="flex items-center gap-2 text-xs">
                     {data.status === 'AWAITING_APPROVAL_B' ? (
                        <span className="text-amber-500 bg-amber-500/10 px-2 py-0.5 rounded-full font-medium animate-pulse">Pending...</span>
                     ) : (
                        <>
                          <span className="text-emerald-500 bg-emerald-500/10 px-2 py-0.5 rounded-full font-medium">Approved</span>
                          <span className="text-foreground/50 font-mono">14:01 PM</span>
                        </>
                     )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      ) : null}
    </div>
  )
}
