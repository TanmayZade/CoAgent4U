import { apiRequest } from '../api'

export interface AgentStatus {
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR'
  slackConnected: boolean
  googleCalendarConnected: boolean
  lastActivityAt: string
}

export interface ConnectedService {
  name: string
  status: 'CONNECTED' | 'DISCONNECTED'
  lastSync: string
}

export interface ActivityDataPoint {
  day: string
  completed: number
  failed: number
  rejected: number
}

// Ensure these types match the domain exactly
export type CoordinationState =
  | 'INITIATED' | 'CHECKING_AVAILABILITY_A' | 'CHECKING_AVAILABILITY_B'
  | 'MATCHING' | 'PROPOSAL_GENERATED' | 'AWAITING_APPROVAL_A' | 'AWAITING_APPROVAL_B'
  | 'APPROVED_BY_BOTH' | 'CREATING_EVENT_A' | 'CREATING_EVENT_B'
  | 'COMPLETED' | 'REJECTED' | 'FAILED'

export interface CoordinationSummaryDto {
  coordinationId: string
  withUsername: string
  withDisplayName: string | null
  withAvatarUrl: string | null
  role: 'REQUESTER' | 'INVITEE'
  state: CoordinationState
  createdAt: string
  meetingTitle: string | null
  meetingTime: string | null
}

export interface DashboardSummaryResponse {
  agentStatus: AgentStatus
  pendingApprovalsCount: number
  recentCoordinations: CoordinationSummaryDto[]
  activitySummary: ActivityDataPoint[]
}

export interface PendingApprovalDto {
  approvalId: string
  coordinationId: string
  approvalType: 'PROPOSAL' | 'INVITATION' | 'CANCELLATION'
  createdAt: string
  expiresAt: string
}

export const dashboardAPI = {
  getSummary: async (username: string): Promise<DashboardSummaryResponse> => {
    return apiRequest<DashboardSummaryResponse>(`/api/dashboard/summary?username=${encodeURIComponent(username)}`)
  },

  getPendingApprovals: async (username: string): Promise<PendingApprovalDto[]> => {
    return apiRequest<PendingApprovalDto[]>(`/api/dashboard/approvals/pending?username=${encodeURIComponent(username)}`)
  }
}
