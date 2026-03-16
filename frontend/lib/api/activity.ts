import { apiRequest } from '../api'

export interface AgentActivityEntry {
  logId: string
  correlationId?: string
  coordinationId?: string
  eventType: string
  description: string
  level: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  occurredAt: string
}

export interface PaginatedResponse<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  isFirst: boolean
  isLast: boolean
  hasNext: boolean
  hasPrevious: boolean
}

export const activityAPI = {
  getLogs: async (
    username: string, 
    page: number = 0, 
    size: number = 20, 
    level?: string
  ): Promise<PaginatedResponse<AgentActivityEntry>> => {
    let url = `/api/agent-activities?username=${encodeURIComponent(username)}&page=${page}&size=${size}`
    if (level && level !== 'ALL') {
      url += `&level=${encodeURIComponent(level)}`
    }
    return apiRequest<PaginatedResponse<AgentActivityEntry>>(url)
  },

  exportLogsUrl: (username: string): string => {
    return `/api/agent-activities/export?username=${encodeURIComponent(username)}`
  }
}
