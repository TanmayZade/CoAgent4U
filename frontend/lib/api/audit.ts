import { apiRequest } from '../api'

export interface AuditLogEntry {
  logId: string
  userId: string
  action: string
  entityType?: string
  entityId?: string
  level: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  payload?: any
  correlationId?: string
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

export const auditAPI = {
  getLogs: async (
    username: string, 
    page: number = 0, 
    size: number = 20, 
    level?: string
  ): Promise<PaginatedResponse<AuditLogEntry>> => {
    let url = `/api/audit-logs?username=${encodeURIComponent(username)}&page=${page}&size=${size}`
    if (level && level !== 'ALL') {
      url += `&level=${encodeURIComponent(level)}`
    }
    return apiRequest<PaginatedResponse<AuditLogEntry>>(url)
  },

  exportLogsUrl: (username: string): string => {
    return `/api/audit-logs/export?username=${encodeURIComponent(username)}`
  }
}
