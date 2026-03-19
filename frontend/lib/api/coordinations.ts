import { apiRequest } from '../api'
import { CoordinationState, CoordinationSummaryDto } from './dashboard'

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

export interface StateLogEntryDto {
  fromState: string | null
  toState: string
  reason: string | null
  transitionedAt: string
}

export interface MeetingProposalDto {
  title: string
  startTime: string
  endTime: string
  durationMinutes: number
  timeZone: string
}

export interface CoordinationDetailDto {
  coordinationId: string
  requesterUsername: string
  requesterDisplayName: string | null
  requesterAvatarUrl: string | null
  inviteeUsername: string
  inviteeDisplayName: string | null
  inviteeAvatarUrl: string | null
  role: 'REQUESTER' | 'INVITEE'
  state: string
  proposal: MeetingProposalDto | null
  createdAt: string
  completedAt: string | null
  stateLog: StateLogEntryDto[]
}

export interface TimeSlotDto {
  start: string
  end: string
}

export const coordinationsAPI = {
  getHistory: async (
    username: string, 
    page: number = 0, 
    size: number = 20, 
    status?: string
  ): Promise<PaginatedResponse<CoordinationSummaryDto>> => {
    let url = `/api/coordinations?username=${encodeURIComponent(username)}&page=${page}&size=${size}`
    if (status && status !== 'ALL') {
      url += `&status=${encodeURIComponent(status)}`
    }
    return apiRequest<PaginatedResponse<CoordinationSummaryDto>>(url)
  },

  getDetail: async (id: string, username: string): Promise<CoordinationDetailDto> => {
    return apiRequest<CoordinationDetailDto>(`/api/coordinations/${encodeURIComponent(id)}?username=${encodeURIComponent(username)}`)
  },

  cancel: async (id: string, username: string): Promise<{ status: string }> => {
    return apiRequest<{ status: string }>(`/api/coordinations/${encodeURIComponent(id)}/cancel?username=${encodeURIComponent(username)}`, {
      method: 'POST'
    })
  },

  approve: async (id: string, username: string, approved: boolean): Promise<{ status: string }> => {
    return apiRequest<{ status: string }>(`/api/coordinations/${encodeURIComponent(id)}/approve?username=${encodeURIComponent(username)}&approved=${approved}`, {
      method: 'POST'
    })
  },

  getSlots: async (id: string, username: string): Promise<TimeSlotDto[]> => {
    return apiRequest<TimeSlotDto[]>(`/api/coordinations/${encodeURIComponent(id)}/slots?username=${encodeURIComponent(username)}`)
  },

  selectSlot: async (id: string, username: string, slot: TimeSlotDto): Promise<{ status: string }> => {
    return apiRequest<{ status: string }>(
      `/api/coordinations/${encodeURIComponent(id)}/select-slot?username=${encodeURIComponent(username)}&start=${encodeURIComponent(slot.start)}&end=${encodeURIComponent(slot.end)}`,
      { method: 'POST' }
    )
  }
}
