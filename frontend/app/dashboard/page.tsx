"use client"

import { useState } from "react"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { AgentStatusCard } from "@/components/agent/agent-status-card"
import { ActiveCoordinationBanner } from "@/components/dashboard/active-coordination-banner"
import { ActivityChart } from "@/components/dashboard/activity-chart"
import { SlackInstallationGuard } from "@/components/dashboard/slack-installation-guard"
import { useUser } from "./layout"
import { dashboardAPI } from "@/lib/api/dashboard"
import { coordinationsAPI } from "@/lib/api/coordinations"
import { CoordinationDetailModal } from "@/components/coordination/coordination-detail-modal"
import { StatusChip } from "@/components/ui/status-chip"
import { Button } from "@/components/ui/button"
import { Calendar, Check, X, User, Loader2 } from "lucide-react"

export default function DashboardPage() {
  const { user, isLoading: isUserLoading } = useUser()
  const queryClient = useQueryClient()
  const [slackGuardDismissed, setSlackGuardDismissed] = useState(false)
  const [selectedCoordId, setSelectedCoordId] = useState<string | null>(null)

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
  
  // Fetch detail when a coordination is selected
  const { data: detailData, isLoading: isDetailLoading } = useQuery({
    queryKey: ['coordination-detail', selectedCoordId, user?.username],
    queryFn: () => coordinationsAPI.getDetail(selectedCoordId!, user!.username),
    enabled: !!selectedCoordId && !!user?.username
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
            <div className="space-y-4">
                {summary?.recentCoordinations?.slice(0, 5).map(coord => (
                  <div 
                    key={coord.coordinationId} 
                    className="flex items-center gap-5 px-5 py-5 rounded-2xl border border-border/40 bg-card/40 backdrop-blur-xl shadow-sm hover:border-foreground/20 transition-colors"
                  >
                    {/* Left: Avatar & Name (horizontal below photo) */}
                    <div className="flex flex-col items-center justify-center shrink-0">
                      <div className="w-16 h-16 rounded-full border-2 border-foreground/15 bg-muted flex items-center justify-center overflow-hidden">
                        {coord.withAvatarUrl ? (
                          <img 
                            src={coord.withAvatarUrl} 
                            alt={coord.withDisplayName || coord.withUsername || "User"} 
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <span className="text-lg font-semibold text-foreground/70">
                            {(coord.withDisplayName || coord.withUsername || "?").substring(0, 2).toUpperCase()}
                          </span>
                        )}
                      </div>
                      <span className="text-xs font-medium text-foreground/70 text-center mt-1.5 whitespace-nowrap">
                        {coord.withDisplayName || coord.withUsername || "Unknown"}
                      </span>
                    </div>

                    {/* Center: Title, Status, Times */}
                    <div className="flex-grow min-w-0 py-0.5">
                      <div className="flex items-center gap-2.5 mb-2">
                        <h3 className="text-base font-semibold text-foreground truncate">
                          {coord.meetingTitle || "Active Sync Session"}
                        </h3>
                        <StatusChip state={coord.state} />
                      </div>

                      <div className="flex flex-col gap-1 text-sm text-foreground/55">
                        <span>
                          {coord.meetingTime 
                            ? (() => {
                                const dt = new Date(coord.meetingTime)
                                const datePart = dt.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
                                const timePart = dt.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
                                return `${datePart}, ${timePart}`
                              })()
                            : "Pending Time"
                          }
                        </span>
                        <span className="text-xs text-foreground/40">
                          {coord.role === 'REQUESTER' ? '↗ Initiated' : '↙ Received'}{' '}
                          {coord.createdAt
                            ? (() => {
                                const dt = new Date(coord.createdAt)
                                const datePart = dt.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
                                const timePart = dt.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
                                return `${datePart}, ${timePart}`
                              })()
                            : ""
                          }
                        </span>
                      </div>
                    </div>

                    {/* Right: View Detailed Status */}
                    <div className="shrink-0">
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-xs font-medium whitespace-nowrap rounded-full px-4"
                        onClick={() => setSelectedCoordId(coord.coordinationId)}
                      >
                        View Detailed Status
                      </Button>
                    </div>
                  </div>
                ))}
                {!summary?.recentCoordinations?.length && !isSummaryLoading && (
                  <div className="p-12 text-center text-foreground/40 bg-muted/10 border border-dashed border-border/50 rounded-2xl font-mono text-sm">
                    <Calendar className="h-10 w-10 mx-auto mb-3 opacity-30" />
                    No recent coordinations found.
                  </div>
                )}
                {isSummaryLoading && (
                  <div className="p-12 text-center text-foreground/40 bg-muted/5 border border-border/20 rounded-2xl">
                    <Loader2 className="w-8 h-8 animate-spin mx-auto mb-3 opacity-30" />
                    <p className="text-sm">Loading recent activity...</p>
                  </div>
                )}
            </div>
          </div>

          {/* Activity Chart */}
          <div>
            <h3 className="text-lg font-semibold tracking-tight mb-4">7-Day Activity</h3>
            <ActivityChart data={summary?.activitySummary} isLoading={isSummaryLoading} />
          </div>
        </div>
      </div>
      {selectedCoordId && detailData && (
        <CoordinationDetailModal
          detail={detailData}
          username={user!.username}
          onClose={() => setSelectedCoordId(null)}
          onCancel={async () => {
            if (selectedCoordId) {
              await coordinationsAPI.cancel(selectedCoordId, user!.username)
              setSelectedCoordId(null)
              queryClient.invalidateQueries({ queryKey: ['dashboard'] })
              queryClient.invalidateQueries({ queryKey: ['coordination-detail'] })
            }
          }}
          onApprove={async (approved) => {
            if (selectedCoordId) {
              await coordinationsAPI.approve(selectedCoordId, user!.username, approved)
              setSelectedCoordId(null)
              queryClient.invalidateQueries({ queryKey: ['dashboard'] })
              queryClient.invalidateQueries({ queryKey: ['coordination-detail'] })
            }
          }}
          onSelectSlot={async (slot) => {
            if (selectedCoordId) {
              await coordinationsAPI.selectSlot(selectedCoordId, user!.username, slot)
              setSelectedCoordId(null)
              queryClient.invalidateQueries({ queryKey: ['dashboard'] })
              queryClient.invalidateQueries({ queryKey: ['coordination-detail'] })
            }
          }}
        />
      )}
    </>
  )
}
