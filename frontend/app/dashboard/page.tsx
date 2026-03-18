"use client"

import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { AgentStatusCard } from "@/components/agent/agent-status-card"
import { ActiveCoordinationBanner } from "@/components/dashboard/active-coordination-banner"
import { CoordinationTable } from "@/components/coordination/coordination-table"
import { ActivityChart } from "@/components/dashboard/activity-chart"
import { SlackInstallationGuard } from "@/components/dashboard/slack-installation-guard"
import { useUser } from "./layout"
import { dashboardAPI } from "@/lib/api/dashboard"
import { StatusChip } from "@/components/ui/status-chip"
import { Button } from "@/components/ui/button"
import { Calendar, Check, X } from "lucide-react"

export default function DashboardPage() {
  const { user, isLoading: isUserLoading } = useUser()
  const [slackGuardDismissed, setSlackGuardDismissed] = useState(false)

  // Fetch dashboard summary
  const { data: summary, isLoading: isSummaryLoading } = useQuery({
    queryKey: ['dashboard', 'summary', user?.username],
    queryFn: () => dashboardAPI.getSummary(user!.username),
    enabled: !!user?.username,
    refetchInterval: 30000 // refresh every 30s
  })

  // Fetch pending approvals
  const { data: pendingApprovals, isLoading: isApprovalsLoading } = useQuery({
    queryKey: ['dashboard', 'approvals', user?.username],
    queryFn: () => dashboardAPI.getPendingApprovals(user!.username),
    enabled: !!user?.username,
    refetchInterval: 15000 // refresh every 15s
  })

  // Show Slack installation guard if not installed and not dismissed
  const showSlackGuard = !isUserLoading && user && !user.isSlackAppInstalled && !slackGuardDismissed

  // Determine if there is an active coordination
  const activeCoordinations = summary?.recentCoordinations?.filter((c: any) => 
    !['COMPLETED', 'REJECTED', 'FAILED'].includes(c.state)
  ) || []
  
  const mostRecentActive = activeCoordinations.length > 0 ? activeCoordinations[0] : null;

  return (
    <>
      <SlackInstallationGuard
        isOpen={!!showSlackGuard}
        onDismiss={() => setSlackGuardDismissed(true)}
      />
      
      <div className="space-y-6">
        {/* Agent Status Card */}
        <AgentStatusCard 
          status={summary?.agentStatus} 
          services={summary?.services}
          isLoading={isSummaryLoading} 
        />

        {/* Active Coordination Banner (Conditional) */}
        {mostRecentActive && (
          <ActiveCoordinationBanner 
            coordinationId={mostRecentActive.coordinationId}
            participant={mostRecentActive.withUsername}
            state={mostRecentActive.state}
            startedAgo="Just now" // Ideally from updatedAt
          />
        )}

        {/* Pending Approvals */}
        {pendingApprovals && pendingApprovals.length > 0 && (
          <div className="space-y-4">
            <h3 className="text-lg font-semibold tracking-tight">Requires Your Action</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {pendingApprovals.map(approval => (
                <div key={approval.approvalId} className="p-5 rounded-xl border border-amber-500/30 bg-amber-500/5 relative overflow-hidden group">
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-3 text-amber-500 font-medium">
                      <Calendar className="w-5 h-5" />
                      Pending Approval
                    </div>
                  </div>
                  <h4 className="text-lg font-semibold text-foreground mb-1">
                    Meeting Context / {approval.approvalType}
                  </h4>
                  <p className="text-sm text-foreground/60 mb-6 font-mono">
                    Expires: {new Date(approval.expiresAt).toLocaleString()}
                  </p>
                  <div className="flex items-center gap-3">
                    <Button className="w-full bg-emerald-500 hover:bg-emerald-600 text-white">
                      <Check className="w-4 h-4 mr-2" />
                      Approve
                    </Button>
                    <Button variant="outline" className="w-full border-rose-500/50 text-rose-500 hover:bg-rose-500/10">
                      <X className="w-4 h-4 mr-2" />
                      Reject
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
          {/* Recent Coordinations */}
          <div className="xl:col-span-2">
            <h3 className="text-lg font-semibold tracking-tight mb-4">Recent Coordinations</h3>
            <div className="rounded-xl border border-border/50 bg-card overflow-hidden shadow-sm">
              <div className="divide-y divide-border/50">
                {summary?.recentCoordinations?.slice(0, 5).map(coord => (
                  <div key={coord.coordinationId} className="flex items-center justify-between p-4 hover:bg-muted/50 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-medium text-sm">
                        {(coord.withUsername || '?').substring(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <div className="font-medium text-foreground">
                          Meeting with {coord.withUsername}
                        </div>
                        <div className="text-xs text-foreground/60 font-mono mt-1">
                          {new Date(coord.createdAt).toLocaleDateString()}
                        </div>
                      </div>
                    </div>
                    <div>
                      <StatusChip state={coord.state} />
                    </div>
                  </div>
                ))}
                {!summary?.recentCoordinations?.length && !isSummaryLoading && (
                  <div className="p-8 text-center text-foreground/60 font-mono text-sm">
                    No recent coordinations found.
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Activity Chart */}
          <div>
            <h3 className="text-lg font-semibold tracking-tight mb-4">7-Day Activity</h3>
            <ActivityChart data={summary?.activityChart} isLoading={isSummaryLoading} />
          </div>
        </div>
      </div>
    </>
  )
}
