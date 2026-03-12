import { AgentStatusCard } from "@/components/agent/agent-status-card"
import { ActiveCoordinationBanner } from "@/components/dashboard/active-coordination-banner"
import { QuickActions } from "@/components/dashboard/quick-actions"
import { CoordinationTable } from "@/components/coordination/coordination-table"
import { ActivityChart } from "@/components/dashboard/activity-chart"

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      {/* Agent Status Card */}
      <AgentStatusCard />

      {/* Active Coordination Banner */}
      <ActiveCoordinationBanner />

      {/* Quick Actions */}
      <QuickActions />

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Coordination History - 2 columns */}
        <div className="xl:col-span-2">
          <CoordinationTable />
        </div>

        {/* Activity Chart - 1 column */}
        <div>
          <ActivityChart />
        </div>
      </div>
    </div>
  )
}
